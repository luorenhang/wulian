package com.plc.mqtt.util;

import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class S7PlcReader {

    private static final Logger logger = Logger.getLogger(S7PlcReader.class.getName());

    private S7Connector connector;
    private String ip;
    private int rack;
    private int slot;

    public S7PlcReader(String ip, int rack, int slot) {
        this.ip = ip;
        this.rack = rack;
        this.slot = slot;
    }

    public boolean connect() {
        try {
            logger.info("Connecting to PLC: " + ip + " (Rack: " + rack + ", Slot: " + slot + ")");
            connector = S7ConnectorFactory
                    .buildTCPConnector()
                    .withHost(ip)
                    .withRack(rack)
                    .withSlot(slot)
                    .build();
            logger.info("PLC connection established successfully");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PLC connection failed: " + e.getMessage(), e);
            return false;
        }
    }

    public void disconnect() {
        if (connector != null) {
            try {
                connector.close();
                logger.info("PLC disconnected");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error disconnecting PLC: " + e.getMessage(), e);
            }
        }
    }

    public boolean isConnected() {
        if (connector == null) {
            return false;
        }
        try {
            connector.read(DaveArea.INPUTS, 1, 1, 0);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "PLC connection check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 读取输入点位的单个位
     * 
     * @param byteIndex 输入字节索引（例如：I0.x 的 byteIndex 为 0，I3.x 的 byteIndex 为 3）
     * @param bitIndex  位索引（0-7）
     * @return 位值，true 表示高电平，false 表示低电平，读取失败返回 false
     */
    public boolean readInputBit(int byteIndex, int bitIndex) {
        if (!isConnected()) {
            logger.warning("Cannot read input bit: PLC not connected");
            return false;
        }

        if (bitIndex < 0 || bitIndex > 7) {
            logger.warning("Invalid bit index: " + bitIndex + ", must be between 0-7");
            return false;
        }

        try {
            byte[] buffer = connector.read(DaveArea.INPUTS, 1, 1, byteIndex);
            if (buffer != null && buffer.length > 0) {
                boolean value = (buffer[0] & (1 << bitIndex)) != 0;
                logger.fine("Read input bit I" + byteIndex + "." + bitIndex + ": " + value);
                return value;
            } else {
                logger.log(Level.SEVERE, "Failed to read input bit I" + byteIndex + "." + bitIndex);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to read input bit I" + byteIndex + "." + bitIndex + ": " + e.getMessage(), e);
            return false;
        }
    }

    public String getIp() {
        return ip;
    }

    public int getRack() {
        return rack;
    }

    public int getSlot() {
        return slot;
    }
}