package com.plc.mqtt.util;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttPublisher {

    private static final Logger logger = Logger.getLogger(MqttPublisher.class.getName());

    private static final int DEFAULT_QOS = 1;

    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;
    private MqttClient mqttClient;

    public MqttPublisher(String brokerUrl) {
        this(brokerUrl, null, null, null);
    }

    public MqttPublisher(String brokerUrl, String clientId) {
        this(brokerUrl, clientId, null, null);
    }

    public MqttPublisher(String brokerUrl, String clientId, String username, String password) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId != null ? clientId : "PLC_Gateway_" + UUID.randomUUID().toString().substring(0, 8);
        this.username = username;
        this.password = password;
        logger.info("Created MqttPublisher with clientId: " + this.clientId + (username != null ? ", username: " + username : ""));
    }

    public boolean connect() {
        if (isConnected()) {
            logger.warning("Already connected to MQTT broker");
            return true;
        }

        try {
            logger.info("Connecting to MQTT broker: " + brokerUrl);
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(brokerUrl, clientId, persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
                options.setPassword(password != null ? password.toCharArray() : new char[0]);
                logger.info("Using MQTT authentication with username: " + username);
            }

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.log(Level.WARNING, "MQTT connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);
            logger.info("MQTT connection established successfully");
            return true;
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to connect to MQTT broker: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean publish(String topic, String json) {
        if (!isConnected()) {
            logger.warning("Cannot publish: MQTT not connected");
            return false;
        }

        if (topic == null || topic.isEmpty()) {
            logger.warning("Cannot publish: topic is null or empty");
            return false;
        }

        if (json == null) {
            logger.warning("Cannot publish: json payload is null");
            return false;
        }

        try {
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(DEFAULT_QOS);
            mqttClient.publish(topic, message);
            logger.fine("Published message to topic '" + topic + "': " + json);
            return true;
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Failed to publish message to topic '" + topic + "': " + e.getMessage(), e);
            return false;
        }
    }

    public void disconnect() {
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                logger.info("MQTT disconnected");
            } catch (MqttException e) {
                logger.log(Level.WARNING, "Error disconnecting MQTT: " + e.getMessage(), e);
            }
            mqttClient = null;
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }
}