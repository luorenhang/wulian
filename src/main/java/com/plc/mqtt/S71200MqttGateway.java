package com.plc.mqtt;

import com.plc.mqtt.util.S7PlcReader;
import com.plc.mqtt.util.DBUtil;
import com.plc.mqtt.util.MqttPublisher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * S7-1200 PLC MQTT网关核心类
 * 负责：1. 连接PLC读取输入点位 2. 存储数据到数据库 3. 通过MQTT上报数据到云端
 * 实现了PLC自动重连、MQTT自动重连、状态变化检测等功能
 */
public class S71200MqttGateway {

    private static final Logger logger = Logger.getLogger(S71200MqttGateway.class.getName());

    // ========== PLC连接配置 ==========
    private static final String PLC_IP = "192.168.2.31";
    private static final int PLC_RACK = 0;
    private static final int PLC_SLOT = 1;

    // ========== 采集配置 ==========
    private static final int READ_INTERVAL_MS = 1000;

    // ========== ThingsBoard MQTT配置 ==========
    private static final String TB_MQTT_BROKER = "tcp://localhost:1883";
    private static final String TB_DEVICE_TOKEN = "xu2bKPhPSdcLQhZ5nPIC";
    private static final String TB_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    // ========== 公共MQTT服务器配置 ==========
    private static final String PUBLIC_MQTT_BROKER = "tcp://47.97.154.65:1883";
    private static final String PUBLIC_MQTT_USERNAME = "niit";
    private static final String PUBLIC_MQTT_PASSWORD = "iiotcsc2413";
    private static final String PLC_DATA_TOPIC_PREFIX = "plc/data/";

    // ========== 核心组件实例 ==========
    private S7PlcReader plcReader;
    private Timer readTimer;
    private volatile boolean isReconnecting = false;

    private MqttClient mqttClient;
    private Gson gson;
    private volatile boolean isMqttReconnecting = false;

    private MqttPublisher publicMqttPublisher;

    /**
     * PLC点位数据结构
     * 用于存储需要监控的PLC输入点位信息
     */
    private static class PlcPoint {
        String name;
        int byteIndex;
        int bitIndex;
        String description;
        int pointId;
        boolean lastValue;     // 上次读取的值（用于状态变化检测）

        PlcPoint(String name, int byteIndex, int bitIndex, String description) {
            this.name = name;
            this.byteIndex = byteIndex;
            this.bitIndex = bitIndex;
            this.description = description;
            this.pointId = -1;
            this.lastValue = false;
        }
    }

    /**
     * 需要监控的PLC输入点位列表
     * 格式：点位名称, 字节索引, 位索引, 描述
     */
    private PlcPoint[] monitoredPoints = {
        new PlcPoint("I0.2", 0, 2, "输入点位I0.2"),
        new PlcPoint("I0.6", 0, 6, "输入点位I0.6"),
        new PlcPoint("I1.0", 1, 0, "输入点位I1.0"),
        new PlcPoint("I2.6", 2, 6, "输入点位I2.6"),
        new PlcPoint("I3.1", 3, 1, "输入点位I3.1"),
        new PlcPoint("I3.7", 3, 7, "输入点位I3.7")
    };

    /**
     * 程序入口
     */
    public static void main(String[] args) {
        S71200MqttGateway gateway = new S71200MqttGateway();
        gateway.start();

        // 注册JVM关闭钩子，确保程序退出时正确释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gateway...");
            gateway.stop();
        }));

        // 主线程阻塞等待（保持程序运行）
        synchronized (S71200MqttGateway.class) {
            try {
                S71200MqttGateway.class.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Main thread interrupted");
            }
        }
    }

    /**
     * 启动网关
     * 初始化顺序：数据库点位 → PLC连接 → MQTT客户端 → 定时采集任务
     */
    public void start() {
        logger.info("Starting S7-1200 PLC Monitor...");

        initDatabasePoints();                           // 初始化数据库点位信息

        plcReader = new S7PlcReader(PLC_IP, PLC_RACK, PLC_SLOT);
        if (!plcReader.connect()) {                     // 连接PLC
            logger.severe("Failed to connect to PLC. Exiting...");
            return;
        }

        initMqttClient();                              // 初始化ThingsBoard MQTT客户端
        initPublicMqttPublisher();                     // 初始化公共MQTT发布器

        startReadingTask();                            // 启动定时采集任务
        logger.info("Gateway started successfully");
    }

    /**
     * 停止网关
     * 释放顺序：定时任务 → MQTT连接 → PLC连接
     */
    public void stop() {
        stopReadingTask();                             // 停止定时采集任务
        disconnectMqttClient();                        // 断开ThingsBoard MQTT连接
        if (publicMqttPublisher != null) {
            publicMqttPublisher.disconnect();          // 断开公共MQTT连接
        }
        if (plcReader != null) {
            plcReader.disconnect();                    // 断开PLC连接
        }
        logger.info("Gateway stopped");
    }

    /**
     * 初始化公共MQTT发布器
     * 连接到远程公共MQTT服务器，用于数据上报
     */
    private void initPublicMqttPublisher() {
        publicMqttPublisher = new MqttPublisher(PUBLIC_MQTT_BROKER, null, PUBLIC_MQTT_USERNAME, PUBLIC_MQTT_PASSWORD);
        boolean connected = publicMqttPublisher.connect();
        logger.info("Connected to public MQTT broker: " + connected);
    }

    /**
     * 初始化数据库中的PLC点位信息
     * 将监控点位注册到数据库，获取点位ID用于后续数据存储
     */
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

    /**
     * 启动定时采集任务
     * 使用Timer定时读取PLC输入点位，间隔由READ_INTERVAL_MS控制
     */
    private void startReadingTask() {
        readTimer = new Timer(true);  // 使用守护线程
        readTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    readPLCInputs();  // 读取PLC输入
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error in reading task: " + e.getMessage(), e);
                    attemptPLCReconnect();  // 发生异常时尝试重连PLC
                }
            }
        }, 0, READ_INTERVAL_MS);  // 立即执行，之后每隔READ_INTERVAL_MS执行一次
        logger.info("Reading task started, interval: " + READ_INTERVAL_MS + "ms");
    }

    /**
     * 停止定时采集任务
     */
    private void stopReadingTask() {
        if (readTimer != null) {
            readTimer.cancel();
            readTimer = null;
            logger.info("Reading task stopped");
        }
    }

    /**
     * PLC连接断开时尝试重连
     * 使用指数退避策略：5秒、10秒、20秒...最大60秒
     * 使用isReconnecting标记防止多个重连线程同时执行
     */
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
            long baseDelay = 5000;   // 初始延迟5秒
            long maxDelay = 60000;   // 最大延迟60秒

            while (!plcReader.isConnected()) {
                try {
                    attempt++;
                    // 指数退避：5s, 10s, 20s, 40s, 60s, 60s...
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

    /**
     * 读取PLC输入点位的核心方法
     * 流程：读取点位值 → 查询数据库当前状态 → 更新状态 → 检测变化 → 记录日志 → 发送MQTT遥测
     */
    private void readPLCInputs() {
        if (!plcReader.isConnected()) {
            logger.warning("Cannot read PLC: PLC not connected");
            return;
        }

        try {
            StringBuilder logBuilder = new StringBuilder("Read inputs - ");
            
            // 遍历所有监控点位
            for (PlcPoint point : monitoredPoints) {
                // 1. 读取PLC输入位值
                boolean currentValue = plcReader.readInputBit(point.byteIndex, point.bitIndex);
                
                // 2. 确保点位ID已获取（首次运行时可能未初始化）
                if (point.pointId <= 0) {
                    point.pointId = DBUtil.getPointId(point.name);
                    logger.info("Got pointId for " + point.name + ": " + point.pointId);
                }

                // 3. 如果点位ID有效，进行数据处理
                if (point.pointId > 0) {
                    // 查询数据库中的当前状态
                    Boolean oldStatus = DBUtil.getCurrentStatus(point.pointId);
                    logger.info("Point " + point.name + " - currentValue: " + currentValue + ", oldStatus from DB: " + oldStatus);
                    
                    // 更新数据库中的当前状态
                    boolean updateSuccess = DBUtil.updateCurrentStatus(point.pointId, currentValue);
                    logger.info("Update current status for " + point.name + ": " + (updateSuccess ? "success" : "failed"));
                    
                    // 4. 状态变化检测与日志记录
                    if (oldStatus == null) {
                        // 首次读取，插入初始日志
                        logger.info("First time reading " + point.name + ", inserting initial log");
                        boolean logSuccess = DBUtil.insertStatusLog(point.pointId, false, currentValue);
                        logger.info("Insert log for " + point.name + ": " + (logSuccess ? "success" : "failed"));
                        sendTelemetry(point.name, currentValue);  // 发送遥测数据
                    } else if (currentValue != oldStatus) {
                        // 状态发生变化，记录变更日志
                        logger.info("Status changed for " + point.name + ": " + oldStatus + " -> " + currentValue);
                        boolean logSuccess = DBUtil.insertStatusLog(point.pointId, oldStatus, currentValue);
                        logger.info("Insert log for " + point.name + ": " + (logSuccess ? "success" : "failed"));
                        sendTelemetry(point.name, currentValue);  // 发送遥测数据
                    }
                    
                    point.lastValue = currentValue;  // 更新缓存的上次值
                } else {
                    logger.warning("Skipping " + point.name + " due to invalid pointId: " + point.pointId);
                }

                logBuilder.append(point.name).append(": ").append(currentValue).append(", ");
            }

            logger.info(logBuilder.toString());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to read PLC inputs: " + e.getMessage(), e);
            // 检测到连接重置，触发PLC重连
            if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                logger.warning("Connection reset detected, attempting reconnect...");
                attemptPLCReconnect();
            }
        }
    }

    /**
     * 初始化ThingsBoard MQTT客户端
     * 配置连接参数并设置回调处理连接断开事件
     */
    private void initMqttClient() {
        gson = new Gson();  // 初始化JSON序列化工具
        try {
            // 创建MQTT客户端实例
            mqttClient = new MqttClient(TB_MQTT_BROKER, MqttClient.generateClientId(), new MemoryPersistence());
            
            // 配置MQTT连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(TB_DEVICE_TOKEN);
            options.setPassword(new char[0]);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);        // 启用自动重连

            // 设置MQTT回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.warning("MQTT connection lost: " + cause.getMessage());
                    attemptMqttReconnect();  // 连接断开时触发重连
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 消息发送完成回调
                }
            });

            mqttClient.connect(options);
            logger.info("Connected to ThingsBoard MQTT broker successfully");
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to connect to ThingsBoard MQTT broker: " + e.getMessage(), e);
            attemptMqttReconnect();  // 连接失败时触发重连
        }
    }

    /**
     * 断开ThingsBoard MQTT连接
     */
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

    /**
     * MQTT连接断开时尝试重连
     * 使用指数退避策略防止频繁重连
     */
    private void attemptMqttReconnect() {
        if (isMqttReconnecting) {
            return;  // 已有重连线程在运行
        }
        isMqttReconnecting = true;

        new Thread(() -> {
            int attempt = 0;
            long baseDelay = 5000;   // 初始延迟5秒
            long maxDelay = 60000;   // 最大延迟60秒

            while (!isMqttClientConnected()) {
                try {
                    attempt++;
                    long delay = Math.min(baseDelay * (long) Math.pow(2, attempt - 1), maxDelay);
                    logger.info("Attempting MQTT reconnect (attempt " + attempt + "), waiting " + delay + "ms...");
                    Thread.sleep(delay);

                    // 尝试重连
                    if (mqttClient != null && !mqttClient.isConnected()) {
                        mqttClient.reconnect();
                    } else {
                        initMqttClient();  // 客户端对象可能已失效，重新初始化
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

    /**
     * 检查MQTT客户端是否已连接
     */
    private boolean isMqttClientConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 发送单点位遥测数据到ThingsBoard
     * 同时也会发送到公共MQTT服务器
     * 
     * @param pointName 点位名称
     * @param value 点位值（布尔值）
     */
    public void sendTelemetry(String pointName, boolean value) {
        if (!isMqttClientConnected()) {
            logger.warning("Cannot send telemetry: MQTT not connected");
            return;
        }

        try {
            // 构建JSON payload
            JsonObject payload = new JsonObject();
            payload.addProperty(pointName, value);
            
            String json = gson.toJson(payload);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1);           // QoS=1：至少一次送达
            message.setRetained(false);  // 不保留消息

            // 发布到ThingsBoard
            mqttClient.publish(TB_TELEMETRY_TOPIC, message);
            logger.info("Sent telemetry to ThingsBoard: " + json);
            
            // 同时发送到公共MQTT服务器
            sendToPublicBroker(pointName, value);
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to send telemetry: " + e.getMessage(), e);
            attemptMqttReconnect();  // 发送失败时触发重连
        }
    }

    /**
     * 发送PLC数据到公共MQTT服务器
     * 
     * @param pointName 点位名称
     * @param value 点位值
     */
    private void sendToPublicBroker(String pointName, boolean value) {
        if (publicMqttPublisher != null) {
            String topic = PLC_DATA_TOPIC_PREFIX + pointName;
            String payload = "{\"" + pointName + "\": " + value + "}";
            boolean result = publicMqttPublisher.publish(topic, payload);
            if (result) {
                logger.info("Sent PLC data to public broker: " + topic + " -> " + payload);
            } else {
                logger.warning("Failed to send PLC data to public broker");
            }
        }
    }

    /**
     * 批量发送遥测数据到ThingsBoard
     * 
     * @param telemetryData 遥测数据Map，key为点位名称，value为点位值
     */
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