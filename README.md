# S7-1200 PLC MQTT网关

基于Java实现的西门子S7-1200 PLC数据采集与MQTT网关程序。

## 功能特性

- 连接西门子S7-1200 PLC进行数据采集
- 定时读取DB块数据（字节、字、双字）
- 将PLC数据转换为JSON格式发布到MQTT Broker
- 支持异常重连机制
- 完整的日志输出

## 环境准备

### 1. PLC配置

确保您的西门子S7-1200 PLC已正确配置：

- **PLC IP地址**: 默认 `192.168.0.1`（需根据实际配置修改）
- **机架号(Rack)**: 通常为 `0`
- **槽号(Slot)**: S7-1200通常为 `1`
- **DB块配置**: 确保已创建DB1数据块，并包含以下地址：
  - `DB1.DBB0` - 字节类型
  - `DB1.DBW2` - 字类型  
  - `DB1.DBD4` - 双字类型

### 2. EMQX配置

确保EMQX MQTT Broker已正确运行：

- **Broker地址**: 默认 `tcp://192.168.0.100:1883`
- **端口**: 默认 `1883`（MQTT默认端口）

### 3. 网络要求

- PLC和运行程序的电脑需在同一局域网
- PLC的102端口需开放（S7协议默认端口）
- 防火墙需允许访问PLC的102端口和EMQX的1883端口

## 修改配置

### 修改PLC连接参数

编辑 `src/main/java/com/plc/mqtt/S71200MqttGateway.java` 文件：

```java
private static final String PLC_IP = "192.168.0.1";
private static final int PLC_RACK = 0;
private static final int PLC_SLOT = 1;
```

### 修改MQTT连接参数

编辑 `src/main/java/com/plc/mqtt/S71200MqttGateway.java` 文件：

```java
private static final String MQTT_BROKER = "tcp://192.168.0.100:1883";
private static final String MQTT_TOPIC = "plc/s71200/data";
```

### 修改读取间隔

编辑 `src/main/java/com/plc/mqtt/S71200MqttGateway.java` 文件：

```java
private static final int READ_INTERVAL_MS = 1000; // 毫秒
```

## 启动方式

### 方式一：Maven运行

```bash
cd d:\工业物联网\wulian
mvn compile exec:java -Dexec.mainClass="com.plc.mqtt.S71200MqttGateway"
```

### 方式二：IDE运行

1. 打开项目到IntelliJ IDEA或Eclipse
2. 找到 `S71200MqttGateway.java` 文件
3. 右键选择 "Run" 或 "Run main()"

### 方式三：打包运行

```bash
mvn clean package
java -jar target/wulian-0.0.1-SNAPSHOT.jar
```

## 测试验证

### 使用MQTTX测试

1. 下载并安装 [MQTTX](https://mqttx.app/)
2. 创建新连接：
   - Name: PLC Gateway Test
   - Host: `192.168.0.100`
   - Port: `1883`
3. 订阅主题：`plc/s71200/data`
4. 启动网关程序，即可看到数据

### 预期输出

成功运行后，日志将输出类似内容：

```
INFO: Starting S7-1200 MQTT Gateway...
INFO: Connecting to PLC: 192.168.0.1 (Rack: 0, Slot: 1)
INFO: PLC connection established
INFO: Connecting to MQTT broker: tcp://192.168.0.100:1883
INFO: MQTT connection established
INFO: Reading task started, interval: 1000ms
INFO: Read PLC data: {"DB1.DBB0":255,"DB1.DBW2":32767,"DB1.DBD4":2147483647,"timestamp":1699999999999}
```

### MQTT消息格式

发布到 `plc/s71200/data` 主题的JSON格式：

```json
{
    "DB1.DBB0": 255,
    "DB1.DBW2": 32767,
    "DB1.DBD4": 2147483647,
    "timestamp": 1699999999999
}
```

## 常见错误与解决

### 1. PLC连接失败 - 端口102不通

**错误信息**:
```
SEVERE: PLC connection failed: Connect failed
```

**解决方法**:
- 检查PLC IP地址是否正确
- 确保PLC和电脑在同一网段
- 检查防火墙是否阻止了102端口
- 使用telnet测试端口：`telnet 192.168.0.1 102`

### 2. DB块不存在或无权限

**错误信息**:
```
SEVERE: Failed to read byte from DB1.DBB0: DB not found
```

**解决方法**:
- 在TIA Portal中确认DB1数据块已创建
- 确保DB块已下载到PLC
- 检查DB块的访问权限（需允许PUT/GET访问）
- 在PLC属性中启用"允许来自远程对象的PUT/GET访问"

### 3. MQTT连接失败

**错误信息**:
```
SEVERE: MQTT connection failed: Connection refused
```

**解决方法**:
- 检查EMQX Broker地址和端口是否正确
- 确保EMQX服务已启动
- 检查防火墙是否阻止了1883端口
- 使用MQTTX测试连接EMQX

### 4. 数据读取异常

**错误信息**:
```
SEVERE: Failed to read PLC data: ...
```

**解决方法**:
- 确认DB块地址配置正确
- 检查数据类型是否匹配（字节/字/双字）
- 确认偏移地址没有重叠

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── plc/
│   │           └── mqtt/
│   │               ├── S71200MqttGateway.java    # 主类
│   │               └── util/
│   │                   ├── S7PlcReader.java       # PLC读取工具类
│   │                   └── MqttPublisher.java     # MQTT发布工具类
│   └── resources/
└── pom.xml                                        # Maven配置
```

## 依赖说明

| 依赖 | 版本 | 说明 |
|------|------|------|
| s7connector | 2.1 | PLC通信库 |
| org.eclipse.paho.client.mqttv3 | 1.2.5 | MQTT客户端 |
| jna | 5.13.0 | Java本地访问 |
| jna-platform | 5.13.0 | JNA平台支持 |
| gson | 2.9.1 | JSON序列化 |

## 技术支持

如有问题，请检查：
1. PLC的网络配置
2. EMQX Broker状态
3. 防火墙设置
4. DB块配置