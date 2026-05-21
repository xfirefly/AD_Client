# AD_Client 数字标牌安卓播放端 技术文档

## 1. 项目概述

AD_Client 是一款数字标牌（广告机）安卓播放端应用，用于在安卓大屏终端上播放包含视频、图片、字幕等多媒体内容的节目。支持局域网和广域网管理，支持 U 盘导入节目内容。

### 1.1 技术栈

| 项目 | 说明 |
|------|------|
| 语言 | Java |
| 最低 SDK | API 24 (Android 7.0) |
| 目标 SDK | API 34 (Android 14) |
| 构建系统 | Gradle |
| JSON 解析 | Gson 2.10.1 |
| 日期时间 | Joda-Time 2.12.5 |
| HTTP 客户端 | OkHttp (内置) + HttpURLConnection |
| 加密 | AES-128-CBC + PBKDF2 |

### 1.2 包结构

```
com.bluberry.adclient      — 核心应用层 (App, MainActivity, ADRender, Msg, CryptLib)
com.bluberry.network        — 网络通信层 (CommandClient, FileClient, IpReceiver, WanServer)
com.bluberry.common         — 通用工具 (HttpTool, HttpRequestHelper, IOUtil, FileUtil, Tools, CryptLib)
com.bluberry.config         — 配置 (PkgConfig)
com.bluberry.setup          — 设置界面 (OptionMenuLayout, ArrowView, ShowOptionMenu)
cs.comm                     — 通信协议与顶层数据模型 (Cmd, Command, AdProgram, Scene)
cs.ipc                      — 媒体项数据模型 (ItemBase, Video, Picture, Subtitle, LocationCalc, AdItemType, Direction)
```

---

## 2. 工程架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    MainActivity (UI)                     │
│  - 全屏沉浸模式                                          │
│  - 按键事件处理 (遥控器/硬件按钮)                          │
│  - 拥有根 ViewGroup (m_Root)                             │
└──────────────┬──────────────────────────────────────────┘
               │ 委托所有渲染
               ▼
┌─────────────────────────────────────────────────────────┐
│                   ADRender (渲染引擎)                     │
│  - 节目解析 (parseJson)                                  │
│  - 场景绘制 (drawScene)                                  │
│  - 媒体播放调度 (Video/Picture/SubtitleUpdater)           │
│  - 场景轮播 (SceneUpdater)                               │
│  - 定时场景检测 (TimingAdChecker)                        │
│  - 截图功能 (TakeScreenshot)                             │
│  - GPIO 硬件按钮读取 (GpioReadThread)                    │
└──────┬────────────┬────────────────┬────────────────────┘
       │            │                │
       ▼            ▼                ▼
┌──────────┐ ┌───────────┐ ┌─────────────────┐
│ Msg 消息  │ │ 数据模型层 │ │   网络通信层      │
│ Handler  │ │ AdProgram │ │ IpReceiver      │
│ 总线     │ │ Scene     │ │ CommandClient   │
│          │ │ Video     │ │ FileClient      │
│          │ │ Picture   │ │ WanServer       │
│          │ │ Subtitle  │ │                 │
└──────────┘ └───────────┘ └─────────────────┘
```

### 2.2 线程模型

应用采用多线程架构，主要线程包括：

| 线程 | 生命周期 | 职责 |
|------|----------|------|
| **主线程 (UI)** | 应用全程 | View 操作、Handler 消息处理 |
| **IpReceiver** | 应用全程 | UDP 广播监听，发现服务器 |
| **AdJsonParser** | 启动时一次 | 扫描本地已有节目目录并解析 |
| **CommandClient** | 每次连接 | TCP 命令通道，接收/处理服务器指令 |
| **FileClient** | 每次传输 | TCP 文件传输（节目 ZIP 或 APK） |
| **SceneUpdater** | 每个节目 | 场景轮播调度 |
| **TimingAdChecker** | 应用全程 | 每 5 秒检测定时场景 |
| **VideoUpdater** | 每个视频层 | 视频播放循环 |
| **PictureUpdater** | 每个图片层 | 图片轮播循环 |
| **BackImageUpdater** | 每个节目 | 背景图片轮播 |
| **GpioReadThread** | 应用全程 | 硬件 GPIO 按钮输入 |
| **Heartbeat** | CommandClient 内 | 每 ~11.6 秒发送心跳 |

---

## 3. 数据模型设计

### 3.1 节目结构层次

```
AdProgram (节目单)
├── ver: int                    — 版本号
├── sceneIntval: int            — 场景轮播间隔（秒）
├── backImageIntval: int        — 背景图轮播间隔（秒）
├── backImage: List<String>     — 背景图片路径列表
└── scene: List<Scene>          — 场景列表
    └── Scene (场景)
        ├── name: String        — 场景名称
        ├── timing: String      — 定时播放时间 (如 "09:30"，"00:00" 表示非定时)
        ├── dir: String         — 文件系统路径
        ├── drawing: boolean    — 是否正在播放
        ├── layers: List<AdItemType> — 图层渲染顺序 (Video/Picture/Subtitle)
        ├── video: List<Video>  — 视频列表
        ├── picture: List<Picture> — 图片列表
        └── subtitle: List<Subtitle> — 字幕列表
```

### 3.2 媒体项基类 (ItemBase)

所有可视媒体项继承自 `ItemBase`，坐标基于 **1920×1080** 设计分辨率：

```java
public class ItemBase {
    int x, y, w, h;           // 1920x1080 坐标系下的位置和尺寸
    float fx, fy, fx1, fy1;   // 浮点坐标（备用）
    String type;               // 类型标识
}
```

坐标转换通过 `LocationCalc` 工具类实现：

```
canvas 坐标 → 1920 坐标:  ToScreenX(canvasX, canvasWidth)  = canvasX * 1920 / canvasWidth
1920 坐标 → canvas 坐标:  ToFormX(screenX, canvasWidth)    = screenX * canvasWidth / 1920
Y 轴同理，基准值为 1080。
```

### 3.3 视频模型 (Video)

```java
public class Video extends ItemBase {
    int intval;                // 播放间隔（秒），-1 表示连续播放
    List<String> filelist;     // 视频文件路径列表，支持多视频轮播
}
```

### 3.4 图片模型 (Picture)

```java
public class Picture extends ItemBase {
    int intval;                // 显示间隔（秒）
    List<String> filelist;     // 图片文件路径列表
}
```

### 3.5 字幕模型 (Subtitle)

```java
public class Subtitle extends ItemBase {
    String fontname;           // 字体名，默认 "Song Ti"
    int fontsize;              // 字号，默认 30
    boolean bold;              // 粗体
    boolean italic;            // 斜体
    boolean underline;         // 下划线
    boolean transparent;       // 背景透明
    int opacity;               // 不透明度 0-100
    Direction direction;       // 滚动方向: ToLeft / ToRight / Static
    int speed;                 // 滚动速度，默认 7
    String text;               // 文本内容
    String fontcolor;          // 字体颜色，默认 "#FFFFFF"
    String backcolor;          // 背景颜色，默认 "#3CA7C7"
}
```

### 3.6 图层类型枚举

```java
public enum AdItemType {
    Video,      // 视频层
    Picture,    // 图片层
    Subtitle,   // 字幕层
    Select      // 选择层（预留）
}

public enum Direction {
    ToLeft,     // 向左滚动
    ToRight,    // 向右滚动
    Static      // 静态居中
}
```

---

## 4. 网络通信协议

### 4.1 三端口架构

| 端口 | 协议 | 用途 |
|------|------|------|
| **41000** | UDP | 服务器广播发现 |
| **41001** | TCP | 命令控制通道 |
| **41002** | TCP | 文件传输通道 |

### 4.2 消息格式

所有命令消息采用统一格式：

```
/<CommandName>/:<Parameter>
```

示例：
- `/server_ip/:192.168.1.100` — 服务器广播自身 IP
- `/send_ad/:` — 发送节目广告
- `/set_time/:1609459200000` — 设置系统时间（毫秒时间戳）
- `/set_volume/:50` — 设置音量
- `/get_screen/:` — 请求截图

### 4.3 命令列表

#### PC → Box（服务器下发命令）

| 命令 | 说明 | 参数 |
|------|------|------|
| `server_ip` | 服务器 IP 广播 | IP 地址 |
| `server_close` | 服务器关闭通知 | 无 |
| `send_ad` | 发送节目文件 | 无（触发 FileClient 连接） |
| `get_screen` | 请求设备截图 | 无 |
| `set_time` | 设置系统时间 | 毫秒时间戳 |
| `set_name` | 设置设备名称 | 名称字符串 |
| `set_id` | 设置设备 ID | ID 字符串 |
| `prev_scene` | 切换到上一个场景 | 无 |
| `next_scene` | 切换到下一个场景 | 无 |
| `set_volume` | 设置音量 | 0-100 |
| `get_volume` | 查询当前音量 | 无 |
| `reboot` | 重启设备 | 无 |
| `install_apk` | 安装 APK 更新 | 无（触发 FileClient 连接） |
| `get_time` | 查询设备时间 | 无 |
| `clear` | 清除所有广告数据 | 无 |

#### Box → PC（设备上报消息）

| 命令 | 说明 | 参数 |
|------|------|------|
| `connect` | 设备上线通知 | 设备名称/ID |
| `heartbeat` | 心跳包 | 无 |
| `send_ad_ok` | 节目接收完成确认 | 无 |
| `screen_image` | 截图数据 | Base64 编码的 JPEG |
| `curr_volume` | 当前音量 | 0-100 |
| `curr_time` | 当前时间 | 时间字符串 |
| `clear` | 清除完成确认 | 无 |
| `set_wan_ip` | WAN 服务器 IP | IP 地址 |

### 4.4 连接流程

```
1. IpReceiver 在 UDP 41000 监听
2. PC 服务器广播 "/server_ip/:<IP>"
3. IpReceiver 收到广播，提取服务器 IP
4. 创建 CommandClient 线程，TCP 连接 41001
5. CommandClient 发送 "/connect/:<设备名>:<设备ID>"
6. 启动 Heartbeat 线程（每 ~11.6 秒发送心跳）
7. 进入命令接收循环
```

### 4.5 WAN 广域网连接

除局域网自动发现外，还支持通过 WAN 连接远程服务器：

1. PC 通过局域网下发 `set_wan_ip` 命令
2. 设备保存 WAN IP 到 SharedPreferences
3. `WanServer` 启动连接线程，连接到远程 IP 的 41001 端口
4. 断线后每 10 秒自动重连
5. 支持通过 `http://file.fooyou.org/apk/ad/custom2ip.json` 查询客户对应的服务器 IP

---

## 5. 数据流详解

### 5.1 完整数据流（PC → 设备 → 播放）

```
┌──────┐   UDP 41000    ┌────────────┐
│  PC  │ ──────────────→ │ IpReceiver │
│ 服务 │                └─────┬──────┘
│ 器   │                      │ 提取 IP
│      │   TCP 41001    ┌─────▼──────────┐
│      │ ──────────────→ │ CommandClient  │
│      │                └─────┬──────────┘
│      │                      │ 收到 send_ad
│      │   TCP 41002    ┌─────▼──────────┐
│      │ ──────────────→ │ FileClient     │
│      │                └─────┬──────────┘
└──────┘                      │ 接收 ZIP
                              ▼
                    ┌──────────────────┐
                    │ 解压到 adDir/N/  │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ 读取 config.json │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ CryptLib.decode  │
                    │ AES-CBC 解密     │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ Gson 反序列化    │
                    │ → AdProgram      │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ ADRender         │
                    │ drawAdProgram()  │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ Video    │  │ Picture  │  │ Subtitle │
        │ View     │  │ View     │  │ View     │
        └──────────┘  └──────────┘  └──────────┘
```

### 5.2 节目文件结构

设备接收到的节目为 ZIP 压缩包，解压后结构如下：

```
adDir/
├── 1/                          — 节目目录（递增编号）
│   ├── config.json             — 加密的节目配置文件
│   ├── video1.mp4              — 视频文件
│   ├── image1.jpg              — 图片文件
│   └── ...
├── 2/                          — 另一个节目
│   ├── config.json
│   └── ...
└── export_subtitles_tk.zip     — U 盘导入的原始 ZIP
```

### 5.3 config.json 解密流程

```
config.json (Base64 编码的 AES 密文)
    │
    ▼
CryptLib.decode(encryptString)
    │
    ├─ 1. Base64 解码 → 原始密文字节
    │
    ├─ 2. 生成 AES 密钥:
    │     password = URLConfig.prefix  ("l5VKJpn'HRa6xYKmgPOfp@N1\"H!0V.?i.lPE")
    │     salt     = URLConfig.fireplayer ("blumedia/fireplayer.php")
    │     算法     = PBKDF2WithHmacSHA1
    │     迭代次数 = 65536
    │     密钥长度 = 128 bit
    │
    ├─ 3. AES/CBC/PKCS5Padding 解密:
    │     IV = "e675f725e675f725" (固定 16 字节)
    │
    └─ 4. 输出明文 JSON 字符串
           │
           ▼
        Gson 反序列化 → AdProgram 对象
```

### 5.4 SharedPreferences 存储

应用使用 `"adclient"` 作为 SharedPreferences 名称，存储以下数据：

| Key | 类型 | 说明 |
|-----|------|------|
| `id` | String | 设备 ID |
| `name` | String | 设备名称 |
| `tvsys` | int | 电视系统/分辨率设置 |
| `sub_onoff` | int | 字幕开关 (0=关, 1=开) |
| `last_scene` | int | 上次播放的场景索引 |
| `wan_ip` | String | WAN 服务器 IP |

---

## 6. 渲染引擎

### 6.1 ADRender 核心流程

#### 初始化 (init)

```
ADRender.init()
├── 注册消息处理器 (adHandler)
├── 注册自身到 App (setAdRender)
├── 启动 IpReceiver 线程
├── 启动 AdJsonParser 线程（扫描本地已有节目）
├── 注册 USB 广播接收器 (MyBroadcastReceiver)
├── 启动 TimingAdChecker 线程
└── 启动 GpioReadThread（GPIO 硬件按钮）
```

#### 节目解析 (parseJson)

```
ADRender.parseJson(dir, add)
├── 读取 dir/config.json 文件
├── CryptLib.decode() 解密
├── Gson 反序列化为 AdProgram
│   ├── 注册 AdItemTypeSerializer (自定义类型适配器)
│   └── 注册 DirectionSerializer (自定义方向适配器)
├── 如果 add=true，添加到 adList
├── 设置 Scene.dir 为文件系统路径
└── 如果 timing="00:00"，立即触发 drawAdProgram()
```

#### 场景绘制 (drawScene)

```
ADRender.drawScene(scene)
├── 销毁当前字幕视图 (destroySubtitle)
├── 销毁当前图片视图 (destroyPicture)
├── 等待当前场景线程结束
├── 遍历 scene.layers 列表（决定 z-order）:
│   ├── Video → 查找对应 Video 对象 → 创建 VideoView → 添加到 m_Root → 启动 VideoUpdater
│   ├── Picture → 查找 Picture 对象 → 创建 ImageView → 添加到 m_Root → 启动 PictureUpdater
│   └── Subtitle → 查找 Subtitle 对象 → 创建 ScrollTextView → 添加到 m_Root
├── 如果有 backImage → 启动 BackImageUpdater
└── 更新当前场景状态
```

#### 场景轮播 (SceneUpdater)

```
SceneUpdater.run()
├── while (App.Alive):
│   ├── 检查 adList 是否为空
│   ├── 获取当前 AdProgram
│   ├── 计算下一个场景索引
│   ├── 如果有定时场景 (timing != "00:00")，跳过
│   ├── sleep(sceneIntval * 1000)
│   ├── 发送 MESSAGE_DRAW_SCENE 消息
│   └── 循环
```

#### 定时场景检测 (TimingAdChecker)

```
TimingAdChecker.run()
├── while (App.Alive):
│   ├── sleep(5000)
│   ├── 获取当前时间 (HH:mm)
│   ├── 遍历所有 AdProgram 的所有 Scene
│   ├── 如果 scene.timing 匹配当前时间
│   └── 发送 MESSAGE_ADD_TIMING_SCENE 消息
```

### 6.2 Handler 消息系统

应用使用基于 `Handler` + `Msg` 枚举的消息总线进行线程间通信：

```
消息发送: App.sendMessage(Msg.MESSAGE_XXX, data)
消息接收: adHandler (主线程 Handler) 处理
```

| 消息类型 | 触发者 | 处理逻辑 |
|----------|--------|----------|
| `MESSAGE_PARSE_AD_DONE` | AdJsonParser | 节目解析完成 |
| `MESSAGE_DRAW_AD` | 多处 | 绘制指定节目 |
| `MESSAGE_PARSE_AD_FINISH` | parseJson | 本轮解析完成 |
| `MESSAGE_DRAW_BG` | BackImageUpdater | 绘制背景图 |
| `MESSAGE_DRAW_SCENE` | SceneUpdater | 绘制指定场景 |
| `MESSAGE_DRAW_IMAGE` | PictureUpdater | 绘制指定图片 |
| `MESSAGE_DRAW_VIDEO` | VideoUpdater | 绘制指定视频 |
| `MESSAGE_PREV_SCENE` | 按键/命令 | 切换上一场景 |
| `MESSAGE_NEXT_SCENE` | 按键/命令 | 切换下一场景 |
| `MESSAGE_RECV_JSON_BEGIN` | FileClient | 开始接收文件 |
| `MESSAGE_RECV_JSON_PROGRESS` | FileClient | 接收进度更新 |
| `MESSAGE_RECV_JSON_END` | FileClient | 接收完成 |
| `MESSAGE_TakeScreenshot` | CommandClient | 截图请求 |
| `MESSAGE_NO_DATA` | 多处 | 无数据提示 |
| `MESSAGE_ADD_TIMING_SCENE` | TimingAdChecker | 添加定时场景 |
| `MESSAGE_CHANGE_SUBTITLE_VISABLE` | OptionMenu | 切换字幕显示 |
| `MESSAGE_DRAW_SCENE_EX` | ADRender | 扩展场景绘制 |
| `MESSAGE_ADD_HDMI` | MainActivity | HDMI 输入 |
| `MESSAGE_STOP_AND_RENAME` | CommandClient | 停止并重命名 |

### 6.3 媒体播放线程

#### VideoUpdater（视频播放）

```
VideoUpdater.run()
├── 获取 Video 对象的 filelist
├── while (App.Alive && 场景仍活跃):
│   ├── 遍历 filelist 中的每个视频文件
│   ├── 通过 Handler 发送 MESSAGE_DRAW_VIDEO 到主线程
│   ├── 等待视频播放完成（通过 MediaPlayer 回调）
│   └── 循环下一个视频
```

#### PictureUpdater（图片轮播）

```
PictureUpdater.run()
├── 获取 Picture 对象的 filelist
├── while (App.Alive && 场景仍活跃):
│   ├── 遍历 filelist 中的每张图片
│   ├── 通过 Handler 发送 MESSAGE_DRAW_IMAGE 到主线程
│   ├── sleep(intval * 1000)
│   └── 循环下一张图片
```

#### ScrollTextView（字幕滚动）

字幕使用自定义 `ScrollTextView` 控件：
- `direction = Static`：文本居中显示
- `direction = ToLeft`：文本从右向左滚动
- `direction = ToRight`：文本从左向右滚动
- 滚动速度由 `speed` 字段控制
- 支持字体、颜色、背景、透明度等配置

---

## 7. UI 系统

### 7.1 MainActivity

- 单 Activity 架构，全屏沉浸模式
- 拥有根 `ViewGroup` (`m_Root`)，所有媒体视图动态添加到此
- 处理按键事件：
  - `MENU` 键：打开/关闭选项菜单（30 秒自动关闭）
  - `REWIND` 键：切换字幕显示/隐藏
  - `NEXT`/`PREV` 键：切换场景
- 截图功能：将根 View 绘制到 Canvas，保存为 JPEG

### 7.2 OptionMenuLayout（选项菜单）

提供设备配置界面：
- **分辨率选择** (ArrowView)：支持 NTSC/PAL/480P/576P/720P/1080I/1080P/4K 等
- **自动播放设置** (ArrowView)：字幕自动显示开关
- **IP 设置**：跳转系统以太网设置
- **ID 设置**：弹出对话框设置设备 ID
- **语言设置**：跳转系统语言设置
- **日期设置**：跳转系统日期设置
- **场景切换**：上一个/下一个按钮
- 显示当前 IP 地址、设备 ID、时间

### 7.3 ArrowView（箭头选择控件）

通用的左右箭头选择器：
- 支持 `List<String>` 数据源
- 左右箭头切换选项
- 支持键盘左右键导航
- 焦点状态高亮
- 单选项时隐藏箭头

### 7.4 ShowOptionMenu

弹出式菜单管理器：
- 使用 `PopupWindow` 实现
- 半透明背景
- 30 秒自动关闭定时器

---

## 8. 网络层详解

### 8.1 IpReceiver（UDP 发现）

```java
// 单例模式
public class IpReceiver implements Runnable {
    public void run() {
        while (App.Alive) {
            // 1. 等待网络可用
            while (!isOnline()) sleep(1000);

            // 2. 打开 UDP Socket 监听 41000 端口
            DatagramSocket socket = new DatagramSocket(Cmd.broadcast_port);

            // 3. 接收广播包
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            // 4. 解析服务器 IP
            String msg = new String(packet.getData());
            if (msg.startsWith("/server_ip")) {
                String serverIP = extractIP(msg);

                // 5. 启动 CommandClient 连接
                CommandClient client = new CommandClient(serverIP);
                new Thread(client).start();
                // 等待 CommandClient 结束后重新监听
            }
        }
    }
}
```

### 8.2 CommandClient（TCP 命令通道）

```java
public class CommandClient implements Runnable {
    public void run() {
        // 1. TCP 连接到服务器 41001 端口
        socket = new Socket(serverIP, Cmd.cmd_port);

        // 2. 发送上线通知
        send(Command.connect, deviceName + ":" + deviceID);

        // 3. 启动心跳线程
        new Thread(new Heartbeat()).start();

        // 4. 命令接收循环
        while (mRun) {
            String message = reader.readLine();
            process(message);
        }
    }

    private void process(String message) {
        // 解析 "/<command>/:<param>" 格式
        String command = parseCommand(message);
        String param = parseParam(message);

        switch (Command.valueOf(command)) {
            case send_ad:
                // 启动 FileClient 接收节目文件
                new Thread(new FileClient(serverIP, FileType.FILE_AD_ZIP)).start();
                break;
            case get_screen:
                // 发送截图请求到主线程
                App.sendMessage(Msg.MESSAGE_TakeScreenshot, null);
                break;
            case set_time:
                // 设置系统时间
                long millis = Long.parseLong(param);
                AlarmManager.setTime(millis);
                break;
            case set_volume:
                // 设置音量
                audioManager.setStreamVolume(STREAM_MUSIC, volume, 0);
                break;
            case reboot:
                // 重启设备（需要 root）
                App.RunAsApp("reboot");
                break;
            // ... 其他命令处理
        }
    }
}
```

### 8.3 FileClient（TCP 文件传输）

```java
public class FileClient implements Runnable {
    public void run() {
        // 1. TCP 连接到服务器 41002 端口
        socket = new Socket(serverIP, Cmd.file_port);

        switch (fileType) {
            case FILE_AD_ZIP:
                // 2a. 接收节目 ZIP
                String nextDir = App.getNextDir();
                Unzip(socket.getInputStream(), nextDir, fileSize);

                // 3a. 解析节目
                App.parseJson(nextDir, true);

                // 4a. 通知服务器传输完成
                CommandClient.send(Command.send_ad_ok, "");
                break;

            case FILE_APK:
                // 2b. 下载 APK 到本地
                FileOutputStream fos = new FileOutputStream(App.getApkFile());
                // ... 复制流

                // 3b. 静默安装（需要 root）
                App.installApk(App.getApkFile());
                break;
        }
    }
}
```

---

## 9. 加密系统

### 9.1 CryptLib

使用 AES-128-CBC 模式加密节目配置文件：

```
加密参数:
├── 算法: AES/CBC/PKCS5Padding
├── 密钥派生: PBKDF2WithHmacSHA1
│   ├── 密码 (password): URLConfig.prefix
│   ├── 盐 (salt): URLConfig.fireplayer
│   ├── 迭代次数: 65536
│   └── 密钥长度: 128 bit
├── 初始向量 (IV): "e675f725e675f725" (固定 16 字节)
└── 编码: Base64

解密流程:
1. Base64 解码 → 密文字节
2. PBKDF2 派生 AES 密钥
3. AES-CBC 解密
4. PKCS5 去填充
5. 输出明文 JSON
```

### 9.2 安全说明

- IV 为固定值，安全性依赖于密钥的保密性
- 密钥通过硬编码的密码和盐派生，适用于设备端解密场景
- 节目配置文件在传输和存储时均为加密状态

---

## 10. 工具类

### 10.1 IOUtil

文件操作工具：
- `copyFile(from, to)` — 文件复制
- `delete(file)` — 递归删除文件/目录
- `readFile(path)` — 读取文件内容为字符串
- `writeFile(path, content)` — 写入字符串到文件
- `readStream(is)` — 读取 InputStream 为字符串

### 10.2 FileUtil

文件解压工具：
- `Unzip(zipPath, targetDir)` — 解压 ZIP 文件到目标目录

### 10.3 HttpTool / HttpRequestHelper

HTTP 客户端封装：
- `getUTF8String(url)` — GET 请求，UTF-8 编码
- `getGBKString(url)` — GET 请求，GBK 编码
- `getBytes(url)` — GET 请求，返回字节数组
- `download(url, path)` — 下载文件到本地
- `post(url, params)` — POST 请求

### 10.4 Tools

系统工具：
- `getDeviceName()` — 获取设备名称
- `getCpuArch()` — 获取 CPU 架构信息
- `getSDCardPath()` — 获取 SD 卡路径
- `getSystemVersion()` — 获取系统版本
- `getScreenResolution()` — 获取屏幕分辨率

### 10.5 print

日志封装：
- `print.d(tag, msg)` — Debug 级别
- `print.e(tag, msg)` — Error 级别
- `print.i(tag, msg)` — Info 级别

---

## 11. U 盘导入流程

```
1. 用户插入 U 盘
2. MyBroadcastReceiver 收到 ACTION_MEDIA_MOUNTED 广播
3. 检查 U 盘根目录是否存在 "export_subtitles_tk.zip"
4. 如果存在:
   a. 发送 MESSAGE_RECV_JSON_BEGIN 消息
   b. 复制 ZIP 到 App.getZipFile()
   c. 解压到 App.getNextDir()
   d. 调用 App.parseJson() 解析节目
   e. 立即开始播放
```

---

## 12. 截图功能

```
1. 收到 get_screen 命令
2. 发送 MESSAGE_TakeScreenshot 到主线程
3. TakeScreenshot 线程:
   a. 创建 Bitmap (屏幕宽高)
   b. 创建 Canvas 绑定 Bitmap
   c. 调用 m_Root.draw(canvas) 绘制当前画面
   d. 压缩为 JPEG (质量 30)
   e. Base64 编码
   f. 通过 CommandClient 发送 screen_image 命令
```

---

## 13. GPIO 硬件按钮

GpioReadThread 读取 `/sys/kernel/debug/gpio` 文件，检测硬件按钮状态：

| 按钮 | 功能 |
|------|------|
| 按钮 1 | 切换字幕显示/隐藏 |
| 按钮 2 | 切换到上一个场景 |
| 按钮 3 | 切换到下一个场景 |

适用于无遥控器的嵌入式场景，通过 GPIO 引脚实现物理按钮控制。

---

## 14. 自动更新

应用支持通过 HTTP 自动更新 APK：

1. Updater 定期检查 `PkgConfig.getUpdateXml()` 返回的更新 XML
2. 如果有新版本，下载 APK 到 `App.getApkFile()`
3. 使用 `pm install -r` 静默安装（需要 root 权限）
4. 安装完成后应用自动重启

---

## 15. 错误处理与容错

- **网络断开**：IpReceiver 检测网络状态，断线后自动重连
- **服务器离线**：CommandClient 结束后 IpReceiver 重新监听
- **WAN 断线**：WanServer 每 10 秒重试连接
- **节目解析失败**：try-catch 包裹，打印堆栈，不影响其他节目
- **文件缺失**：播放时检查文件存在性，跳过缺失文件
- **线程管理**：所有线程通过 `App.Alive` 标志统一控制退出
