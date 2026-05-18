package edu.ngd.order.wulian.controller;

import com.plc.mqtt.util.MqttPublisher;
import com.plc.mqtt.util.MqttPublisher.MqttReceivedMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    private static final String DEFAULT_SUBSCRIBE_TOPIC = "plc/data/#";

    private final MqttPublisher mqttPublisher = new MqttPublisher(
            "tcp://47.97.154.65:1883", null, "niit", "iiotcsc2413");
    private final List<MqttMessage> messages = new CopyOnWriteArrayList<>();
    private int messageId = 1;

    @PostConstruct
    public void init() {
        mqttPublisher.connect();
        boolean subscribed = mqttPublisher.subscribe(DEFAULT_SUBSCRIBE_TOPIC);
        System.out.println("Auto-subscribed to topic: " + DEFAULT_SUBSCRIBE_TOPIC + " - " + (subscribed ? "success" : "failed"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean connected = mqttPublisher.isConnected();
        return ResponseEntity.ok(new MqttStatus(connected, connected ? "MQTT连接正常" : "MQTT未连接"));
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessages() {
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/topics")
    public ResponseEntity<?> getSubscribedTopics() {
        List<String> topics = mqttPublisher.getSubscribedTopics();
        return ResponseEntity.ok(topics);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribeTopic(@RequestBody SubscribeRequest request) {
        if (request.getTopic() == null || request.getTopic().isEmpty()) {
            return ResponseEntity.badRequest().body("Topic不能为空");
        }

        boolean result = mqttPublisher.subscribe(request.getTopic());
        
        if (result) {
            return ResponseEntity.ok("订阅成功");
        } else {
            return ResponseEntity.internalServerError().body("订阅失败");
        }
    }

    @PostMapping("/subscribe/batch")
    public ResponseEntity<?> subscribeTopics(@RequestBody BatchSubscribeRequest request) {
        if (request.getTopics() == null || request.getTopics().isEmpty()) {
            return ResponseEntity.badRequest().body("Topics数组不能为空");
        }

        boolean result = mqttPublisher.subscribeMultiple(request.getTopics().toArray(new String[0]));
        
        if (result) {
            return ResponseEntity.ok("批量订阅成功");
        } else {
            return ResponseEntity.internalServerError().body("部分订阅失败");
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeTopic(@RequestBody SubscribeRequest request) {
        if (request.getTopic() == null || request.getTopic().isEmpty()) {
            return ResponseEntity.badRequest().body("Topic不能为空");
        }

        boolean result = mqttPublisher.unsubscribe(request.getTopic());
        
        if (result) {
            return ResponseEntity.ok("取消订阅成功");
        } else {
            return ResponseEntity.internalServerError().body("取消订阅失败");
        }
    }

    @GetMapping("/received")
    public ResponseEntity<?> getReceivedMessages() {
        List<MqttReceivedMessage> receivedMessages = mqttPublisher.getReceivedMessages();
        return ResponseEntity.ok(receivedMessages);
    }

    @DeleteMapping("/received")
    public ResponseEntity<?> clearReceivedMessages() {
        mqttPublisher.clearReceivedMessages();
        return ResponseEntity.ok("已清空接收消息");
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publishMessage(@RequestBody PublishRequest request) {
        if (request.getTopic() == null || request.getTopic().isEmpty()) {
            return ResponseEntity.badRequest().body("Topic不能为空");
        }

        boolean result = mqttPublisher.publish(request.getTopic(), request.getPayload());
        
        if (result) {
            MqttMessage message = new MqttMessage(
                    messageId++,
                    request.getTopic(),
                    request.getPayload(),
                    java.time.LocalDateTime.now().toString(),
                    0
            );
            messages.add(0, message);
            
            if (messages.size() > 50) {
                messages.remove(messages.size() - 1);
            }
            
            return ResponseEntity.ok("发布成功");
        } else {
            return ResponseEntity.internalServerError().body("发布失败");
        }
    }

    public static class MqttStatus {
        private boolean connected;
        private String message;

        public MqttStatus(boolean connected, String message) {
            this.connected = connected;
            this.message = message;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class MqttMessage {
        private int id;
        private String topic;
        private String payload;
        private String timestamp;
        private int qos;

        public MqttMessage(int id, String topic, String payload, String timestamp, int qos) {
            this.id = id;
            this.topic = topic;
            this.payload = payload;
            this.timestamp = timestamp;
            this.qos = qos;
        }

        public int getId() {
            return id;
        }

        public String getTopic() {
            return topic;
        }

        public String getPayload() {
            return payload;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public int getQos() {
            return qos;
        }
    }

    public static class PublishRequest {
        private String topic;
        private String payload;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }

    public static class SubscribeRequest {
        private String topic;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class BatchSubscribeRequest {
        private List<String> topics;

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
    }
}
