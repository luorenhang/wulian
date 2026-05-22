package com.plc.mqtt.util;

import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * S7系列PLC读取工具类
 * 使用s7connector库实现与西门子PLC的通信
 * 支持连接管理和输入点位读取
 */
public class S7PlcReader {

    private static final Logger logger = Logger.getLogger(S7PlcReader.class.getName());

    private S7Connector connector;  // PLC连接器实例
    private String ip;             // PLC设备IP地址
    private int rack;              // PLC机架号
    private int slot;              // PLC插槽号

    /**
     * 构造函数
     * 
     * @param ip PLC设备的IP地址
     * @param rack 机架号（S7-1200通常为0）
     * @param slot 插槽号（CPU通常在插槽1）
     */
    public S7PlcReader(String ip, int rack, int slot) {
        this.ip = ip;
        this.rack = rack;
        this.slot = slot;
    }

    /**
     * 连接到PLC
     * 
     * @return 连接成功返回true，失败返回false
     */
    public boolean connect() {
        try {
            logger.info("Connecting to PLC: " + ip + " (Rack: " + rack + ", Slot: " + slot + ")");
            connector = S7ConnectorFactory
                    .buildTCPConnector()       // 使用TCP连接方式
                    .withHost(ip)              // 设置PLC IP
                    .withRack(rack)            // 设置机架号
                    .withSlot(slot)            // 设置插槽号
                    .build();                  // 构建连接器
            logger.info("PLC connection established successfully");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PLC connection failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 断开与PLC的连接
     */
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

    /**
     * 检查PLC连接状态
     * 通过尝试读取输入区来验证连接是否有效
     * 
     * @return 已连接返回true，未连接返回false
     */
    public boolean isConnected() {
        if (connector == null) {
            return false;
        }
        try {
            connector.read(DaveArea.INPUTS, 1, 1, 0);  // 尝试读取1个字节验证连接
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "PLC connection check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 读取PLC输入区的单个位
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

        // 验证位索引范围
        if (bitIndex < 0 || bitIndex > 7) {
            logger.warning("Invalid bit index: " + bitIndex + ", must be between 0-7");
            return false;
        }

        try {
            // 从PLC输入区读取1个字节
            byte[] buffer = connector.read(DaveArea.INPUTS, 1, 1, byteIndex);
            if (buffer != null && buffer.length > 0) {
                // 提取指定的位值
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

    /**
     * 获取PLC IP地址
     */
    public String getIp() {
        return ip;
    }

    /**
     * 获取PLC机架号
     */
    public int getRack() {
        return rack;
    }

    /**
     * 获取PLC插槽号
     */
    public int getSlot() {
        return slot;
    }
}