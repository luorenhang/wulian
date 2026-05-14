package com.plc.mqtt;

import com.plc.mqtt.util.S7PlcReader;
import com.plc.mqtt.util.DBUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class S71200MqttGateway {

    private static final Logger logger = Logger.getLogger(S71200MqttGateway.class.getName());

    private static final String PLC_IP = "192.168.2.31";
    private static final int PLC_RACK = 0;
    private static final int PLC_SLOT = 1;

    private static final int READ_INTERVAL_MS = 1000;

    private static final String TB_MQTT_BROKER = "tcp://localhost:1883";
    private static final String TB_DEVICE_TOKEN = "xu2bKPhPSdcLQhZ5nPIC";
    private static final String TB_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    private S7PlcReader plcReader;
    private Timer readTimer;
    private volatile boolean isReconnecting = false;

    private MqttClient mqttClient;
    private Gson gson;
    private volatile boolean isMqttReconnecting = false;

    private static class PlcPoint {
        String name;
        int byteIndex;
        int bitIndex;
        String description;
        int pointId;
        boolean lastValue;

        PlcPoint(String name, int byteIndex, int bitIndex, String description) {
            this.name = name;
            this.byteIndex = byteIndex;
            this.bitIndex = bitIndex;
            this.description = description;
            this.pointId = -1;
            this.lastValue = false;
        }
    }

    private PlcPoint[] monitoredPoints = {
        new PlcPoint("I0.2", 0, 2, "Input point I0.2"),
        new PlcPoint("I0.6", 0, 6, "Input point I0.6"),
        new PlcPoint("I1.0", 1, 0, "Input point I1.0"),
        new PlcPoint("I2.6", 2, 6, "Input point I2.6"),
        new PlcPoint("I3.1", 3, 1, "Input point I3.1"),
        new PlcPoint("I3.7", 3, 7, "Input point I3.7")
    };

    public static void main(String[] args) {
        S71200MqttGateway gateway = new S71200MqttGateway();
        gateway.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gateway...");
            gateway.stop();
        }));

        synchronized (S71200MqttGateway.class) {
            try {
                S71200MqttGateway.class.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Main thread interrupted");
            }
        }
    }

    public void start() {
        logger.info("Starting S7-1200 PLC Monitor...");

        initDatabasePoints();

        plcReader = new S7PlcReader(PLC_IP, PLC_RACK, PLC_SLOT);
        if (!plcReader.connect()) {
            logger.severe("Failed to connect to PLC. Exiting...");
            return;
        }

        initMqttClient();

        startReadingTask();
        logger.info("Gateway started successfully");
    }

    public void stop() {
        stopReadingTask();
        disconnectMqttClient();
        if (plcReader != null) {
            plcReader.disconnect();
        }
        logger.info("Gateway stopped");
    }

    private void initDatabasePoints() {
        logger.info("Initializing PLC points in database...");
        for (PlcPoint point : monitoredPoints) {
            boolean result = DBUtil.initPoint(point.name, point.description);
            if (result) {
                int pointId = DBUtil.getPointId(point.name);
                if (pointId > 0) {
                    point.pointId = pointId;
                    logger.info("Point " + point.name + " initialized with ID: " + pointId);
                }
            }
        }
    }

    private void startReadingTask() {
        readTimer = new Timer(true);
        readTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    readPLCInputs();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error in reading task: " + e.getMessage(), e);
                    attemptPLCReconnect();
                }
            }
        }, 0, READ_INTERVAL_MS);
        logger.info("Reading task started, interval: " + READ_INTERVAL_MS + "ms");
    }

    private void stopReadingTask() {
        if (readTimer != null) {
            readTimer.cancel();
            readTimer = null;
            logger.info("Reading task stopped");
        }
    }

    private void attemptPLCReconnect() {
        new Thread(() -> {
            if (isReconnecting) {
                logger.info("Reconnection already in progress, skipping...");
                return;
            }
            isReconnecting = true;
            logger.info("Attempting PLC reconnect...");
            plcReader.disconnect();

            int attempt = 0;
            long baseDelay = 5000;
            long maxDelay = 60000;

            while (!plcReader.isConnected()) {
                try {
                    attempt++;
                    long delay = Math.min(baseDelay * (long) Math.pow(2, attempt - 1), maxDelay);
                    logger.info("Attempting PLC reconnect (attempt " + attempt + "), waiting " + delay + "ms...");
                    Thread.sleep(delay);

                    if (plcReader.connect()) {
                        logger.info("PLC reconnection successful after " + attempt + " attempts");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Reconnection thread interrupted");
                    break;
                }
            }
            isReconnecting = false;
        }).start();
    }

    private void readPLCInputs() {
        if (!plcReader.isConnected()) {
            logger.warning("Cannot read PLC: PLC not connected");
            return;
        }

        try {
            StringBuilder logBuilder = new StringBuilder("Read inputs - ");
            
            for (PlcPoint point : monitoredPoints) {
                boolean currentValue = plcReader.readInputBit(point.byteIndex, point.bitIndex);
                
                if (point.pointId <= 0) {
                    point.pointId = DBUtil.getPointId(point.name);
                    logger.info("Got pointId for " + point.name + ": " + point.pointId);
                }

                if (point.pointId > 0) {
                    Boolean oldStatus = DBUtil.getCurrentStatus(point.pointId);
                    logger.info("Point " + point.name + " - currentValue: " + currentValue + ", oldStatus from DB: " + oldStatus);
                    
                    boolean updateSuccess = DBUtil.updateCurrentStatus(point.pointId, currentValue);
                    logger.info("Update current status for " + point.name + ": " + (updateSuccess ? "success" : "failed"));
                    
                    if (oldStatus == null) {
                        logger.info("First time reading " + point.name + ", inserting initial log");
                        boolean logSuccess = DBUtil.insertStatusLog(point.pointId, false, currentValue);
                        logger.info("Insert log for " + point.name + ": " + (logSuccess ? "success" : "failed"));
                        sendTelemetry(point.name, currentValue);
                    } else if (currentValue != oldStatus) {
                        logger.info("Status changed for " + point.name + ": " + oldStatus + " -> " + currentValue);
                        boolean logSuccess = DBUtil.insertStatusLog(point.pointId, oldStatus, currentValue);
                        logger.info("Insert log for " + point.name + ": " + (logSuccess ? "success" : "failed"));
                        sendTelemetry(point.name, currentValue);
                    }
                    
                    point.lastValue = currentValue;
                } else {
                    logger.warning("Skipping " + point.name + " due to invalid pointId: " + point.pointId);
                }

                logBuilder.append(point.name).append(": ").append(currentValue).append(", ");
            }

            logger.info(logBuilder.toString());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to read PLC inputs: " + e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                logger.warning("Connection reset detected, attempting reconnect...");
                attemptPLCReconnect();
            }
        }
    }

    private void initMqttClient() {
        gson = new Gson();
        try {
            mqttClient = new MqttClient(TB_MQTT_BROKER, MqttClient.generateClientId(), new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(TB_DEVICE_TOKEN);
            options.setPassword(new char[0]);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.warning("MQTT connection lost: " + cause.getMessage());
                    attemptMqttReconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);
            logger.info("Connected to ThingsBoard MQTT broker successfully");
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to connect to ThingsBoard MQTT broker: " + e.getMessage(), e);
            attemptMqttReconnect();
        }
    }

    private void disconnectMqttClient() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                logger.info("Disconnected from ThingsBoard MQTT broker");
            } catch (MqttException e) {
                logger.log(Level.SEVERE, "Error disconnecting from MQTT broker: " + e.getMessage(), e);
            }
        }
    }

    private void attemptMqttReconnect() {
        if (isMqttReconnecting) {
            return;
        }
        isMqttReconnecting = true;

        new Thread(() -> {
            int attempt = 0;
            long baseDelay = 5000;
            long maxDelay = 60000;

            while (!isMqttClientConnected()) {
                try {
                    attempt++;
                    long delay = Math.min(baseDelay * (long) Math.pow(2, attempt - 1), maxDelay);
                    logger.info("Attempting MQTT reconnect (attempt " + attempt + "), waiting " + delay + "ms...");
                    Thread.sleep(delay);

                    if (mqttClient != null && !mqttClient.isConnected()) {
                        mqttClient.reconnect();
                    } else {
                        initMqttClient();
                    }

                    if (isMqttClientConnected()) {
                        logger.info("MQTT reconnection successful after " + attempt + " attempts");
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "MQTT reconnection attempt failed: " + e.getMessage(), e);
                }
            }
            isMqttReconnecting = false;
        }).start();
    }

    private boolean isMqttClientConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public void sendTelemetry(String pointName, boolean value) {
        if (!isMqttClientConnected()) {
            logger.warning("Cannot send telemetry: MQTT not connected");
            return;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty(pointName, value);
            
            String json = gson.toJson(payload);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1);
            message.setRetained(false);

            mqttClient.publish(TB_TELEMETRY_TOPIC, message);
            logger.info("Sent telemetry to ThingsBoard: " + json);
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to send telemetry: " + e.getMessage(), e);
            attemptMqttReconnect();
        }
    }

    public void sendTelemetryBatch(java.util.Map<String, Boolean> telemetryData) {
        if (!isMqttClientConnected()) {
            logger.warning("Cannot send telemetry: MQTT not connected");
            return;
        }

        try {
            String json = gson.toJson(telemetryData);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1);
            message.setRetained(false);

            mqttClient.publish(TB_TELEMETRY_TOPIC, message);
            logger.info("Sent batch telemetry to ThingsBoard: " + json);
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to send batch telemetry: " + e.getMessage(), e);
            attemptMqttReconnect();
        }
    }
}