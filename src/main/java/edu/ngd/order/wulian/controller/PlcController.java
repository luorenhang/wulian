package edu.ngd.order.wulian.controller;

import com.plc.mqtt.util.S7PlcReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/plc")
public class PlcController {

    private final S7PlcReader plcReader = new S7PlcReader("192.168.2.31", 0, 1);
    private boolean isConnected = false;

    private static class PlcPoint {
        String name;
        int byteIndex;
        int bitIndex;

        PlcPoint(String name, int byteIndex, int bitIndex) {
            this.name = name;
            this.byteIndex = byteIndex;
            this.bitIndex = bitIndex;
        }
    }

    private PlcPoint[] monitoredPoints = {
        new PlcPoint("I0.2", 0, 2),
        new PlcPoint("I0.6", 0, 6),
        new PlcPoint("I1.0", 1, 0),
        new PlcPoint("I2.6", 2, 6),
        new PlcPoint("I3.1", 3, 1),
        new PlcPoint("I3.7", 3, 7)
    };

    @PostConstruct
    public void init() {
        attemptConnection();
    }

    private void attemptConnection() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                isConnected = plcReader.connect();
                if (isConnected) {
                    System.out.println("PLC connected successfully in controller");
                } else {
                    System.out.println("PLC connection failed in controller");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectPlc() {
        if (!isConnected) {
            isConnected = plcReader.connect();
        }
        return ResponseEntity.ok(new PlcStatus(isConnected, isConnected ? "PLC连接成功" : "PLC连接失败"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getPlcStatus() {
        boolean currentStatus = plcReader.isConnected();
        isConnected = currentStatus;
        return ResponseEntity.ok(new PlcStatus(currentStatus, currentStatus ? "PLC连接正常" : "PLC未连接"));
    }

    @GetMapping("/data")
    public ResponseEntity<?> getPlcData() {
        List<PlcDataItem> data = new ArrayList<>();

        if (plcReader.isConnected()) {
            isConnected = true;
            try {
                int id = 1;
                for (PlcPoint point : monitoredPoints) {
                    boolean value = plcReader.readInputBit(point.byteIndex, point.bitIndex);
                    data.add(new PlcDataItem(id++, point.name, value ? 1 : 0, "BOOL"));
                }
            } catch (Exception e) {
                isConnected = false;
                e.printStackTrace();
            }
        } else {
            int id = 1;
            for (PlcPoint point : monitoredPoints) {
                data.add(new PlcDataItem(id++, point.name, 0, "BOOL"));
            }
        }

        return ResponseEntity.ok(data);
    }

    public static class PlcStatus {
        private boolean connected;
        private String message;

        public PlcStatus(boolean connected, String message) {
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

    public static class PlcDataItem {
        private int id;
        private String tagName;
        private Object value;
        private String dataType;
        private String timestamp;

        public PlcDataItem(int id, String tagName, Object value, String dataType) {
            this.id = id;
            this.tagName = tagName;
            this.value = value;
            this.dataType = dataType;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }

        public int getId() {
            return id;
        }

        public String getTagName() {
            return tagName;
        }

        public Object getValue() {
            return value;
        }

        public String getDataType() {
            return dataType;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}