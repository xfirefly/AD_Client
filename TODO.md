# AD_Client 数字标牌播放端 — 设计分析与改进计划

---

## 一、设计失误

### 1. 安全问题（严重）

#### 1.1 加密密钥硬编码在源码中

`URLConfig.java` 将 PBKDF2 密码和盐直接写死：

```java
public static String fireplayer = "blumedia/fireplayer.php";      // salt
public static String prefix = "l5VKJpn'HRa6xYKmgPOfp@N1\"H!0V.?i.lPE"; // password
```

**问题**：APK 反编译即可获得密钥，加密形同虚设。任何拿到 APK 的人都能解密所有节目配置。

**改进**：密钥应从安全服务器动态获取，或使用 Android Keystore 系统存储，至少做混淆分拆。

#### 1.2 AES IV 固定

`CryptLib.java` 使用固定 IV `"e675f725e675f725"`：

```java
private static String ivs = "e675f725e675f725";
```

**问题**：CBC 模式下固定 IV 会导致相同明文产生相同密文，易受模式分析攻击。

**改进**：每次加密生成随机 IV，将 IV 附在密文前一起传输。

#### 1.3 大量功能依赖 Root 权限

以下功能都需要 `su` 执行：
- 静默安装 APK (`pm install -r`)
- 重启设备 (`reboot`)
- 设置系统时间 (`AlarmManager.setTime`)
- 读取 GPIO (`/sys/kernel/debug/gpio`)

**问题**：Root 后设备安全性大幅降低，且非所有设备都能 Root。普通商业显示器通常不提供 Root。

**改进**：
- APK 安装：使用 `PackageInstaller` API（无需 Root）
- 时间设置：引导用户手动配置或使用 NTP 同步
- GPIO：改用 Android 标准输入事件或 HID 协议

---

### 2. 架构问题

#### 2.1 线程管理混乱

当前手动创建 12+ 种线程，用 `List<Thread>` 追踪，`join()` 等待结束：

```java
private List<Thread> sceneThreads = new ArrayList<>();
private List<Thread> adThreads = new ArrayList<>();
private List<Thread> appThreads = new ArrayList<>();
```

**问题**：
- 无上限控制，极端情况可能创建数十个线程
- `join()` 在主线程调用会导致 ANR
- 线程生命周期管理依赖手动设置标志位，容易遗漏
- 无线程池，频繁创建销毁开销大

**改进**：使用 `ExecutorService` 线程池，或迁移到 Kotlin Coroutines。

#### 2.2 Handler 消息系统脆弱

`Msg` 枚举用 ordinal 作为 `msg.what`：

```java
public enum Msg {
    MESSAGE_PARSE_AD_DONE,    // 0
    MESSAGE_DRAW_AD,          // 1
    // ... 共 20 个
}
```

**问题**：
- 枚举顺序变动会导致消息错乱
- 无类型安全，`msg.obj` 是 Object，强转可能 ClassCastException
- 消息处理是巨大的 switch-case，难以维护

**改进**：使用 `LiveData`、`EventBus`、或 Kotlin `Channel`/`Flow` 替代。

#### 2.3 单 Activity + 动态 View 添加，无状态管理

所有媒体 View 直接 `addView()` 到根 ViewGroup：

```java
m_Root.addView(videoView, params);
m_Root.addView(imageView, params);
m_Root.addView(subtitleView, params);
```

**问题**：
- 配置变更（旋转等）时所有 View 状态丢失
- 无法利用 Android 生命周期感知组件
- 内存泄漏风险 — View 持有 Activity 引用

**改进**：使用 Fragment 管理每个场景区域，或至少用 ViewModel 保存状态。

#### 2.4 内存泄漏隐患

- `drawableMap` 缓存 Drawable 但无清理策略
- Bitmap 创建后无主动回收
- 匿名内部类（Handler、Runnable）隐式持有外部类引用
- `App` 作为 Application 单例持有 `adRenderInstance` 和 `messageHandler`

**改进**：使用 `WeakReference`，在 `onDestroy` 中主动清理，使用 LeakCanary 检测。

#### 2.5 无错误恢复机制

- 节目解析失败只打印堆栈，不做重试
- 网络断开后重连逻辑分散在多个类中
- MediaPlayer 出错无重试或降级策略
- ZIP 解压失败无回滚

**改进**：实现统一的错误处理策略：重试 → 降级 → 告警。

#### 2.6 坐标系硬编码

所有媒体项坐标基于 1920×1080 设计分辨率：

```java
public static float ToFormX(float x, float canvasWidth) {
    return x * canvasWidth / 1920.0f;
}
```

**问题**：
- 不支持非 16:9 屏幕（如竖屏广告机、超宽屏）
- 坐标转换分散在各处，无统一管理
- 缩放后文字和图片可能变形

**改进**：支持可配置的设计分辨率，添加宽高比保持缩放模式。

---

### 3. 代码质量问题

#### 3.1 无单元测试

整个项目没有任何测试代码。核心逻辑（加密解密、坐标转换、命令解析、JSON 反序列化）都是可测试的纯逻辑，但完全没有覆盖。

#### 3.2 日志系统不统一

同时使用 `Log.d`、`System.out.println`（已清理）、自定义 `print` 类。无日志级别控制，无日志文件持久化，调试困难。

#### 3.3 硬编码字符串大量散布

- 端口号 `41000`/`41001`/`41002` 在 `Cmd.java` 硬编码
- 文件名 `config.json`、`export_subtitles_tk.zip` 散布在代码中
- 字体名 `"Song Ti"` 硬编码
- 路径 `/sys/kernel/debug/gpio` 硬编码

#### 3.4 资源管理不严格

- InputStream/OutputStream 未统一使用 try-with-resources
- Socket 关闭逻辑分散，部分路径可能泄漏
- MediaPlayer 释放不彻底

---

## 二、缺失功能

### 1. 播放功能缺失

| 功能 | 说明 | 优先级 |
|------|------|--------|
| **竖屏模式** | 当前仅支持横屏，餐饮/零售大量使用竖屏广告机 | 高 |
| **多区域布局** | 单场景仅支持线性叠加，不支持分区（如左视频右图片下字幕） | 高 |
| **网页组件** | 支持嵌入 WebView 显示网页/天气/股票等动态内容 | 中 |
| **PDF/文档展示** | 部分场景需要展示菜单、价目表等 PDF 内容 | 中 |
| **音频独立播放** | 背景音乐独立于视频，支持循环播放 | 中 |
| **流媒体支持** | RTSP/RTMP 直播流输入 | 低 |
| **多视频同屏** | 单场景多个视频同时播放 | 低 |

### 2. 管理功能缺失

| 功能 | 说明 | 优先级 |
|------|------|--------|
| **远程监控面板** | 设备状态（CPU/内存/存储/温度）上报和展示 | 高 |
| **播放统计** | 每个素材的播放次数、时长统计，生成报表 | 高 |
| **远程日志** | 设备日志上传，远程排查问题 | 高 |
| **远程截屏** | 当前已有基础实现，但需要优化为按需触发+定时截图 | 中 |
| **设备分组** | 按区域/类型分组管理设备 | 中 |
| **内容审核** | 节目发布前审核流程 | 低 |
| **多用户管理** | 不同管理员管理不同设备组 | 低 |

### 3. 可靠性缺失

| 功能 | 说明 | 优先级 |
|------|------|--------|
| **断网续播** | 网络中断时继续播放本地缓存内容 | 高 |
| **看门狗机制** | 应用崩溃自动重启 | 高 |
| **存储空间管理** | 自动清理过期节目，防止存储占满 | 高 |
| **增量更新** | 当前每次传输完整 ZIP，应支持仅传输变更文件 | 中 |
| **节目完整性校验** | 文件传输后校验 MD5/SHA256 | 中 |
| **降级播放** | 高分辨率视频无法播放时自动降级 | 低 |

### 4. 用户体验缺失

| 功能 | 说明 | 优先级 |
|------|------|--------|
| **开机引导** | 首次使用的配置向导 | 中 |
| **设备信息展示** | 设备 IP/MAC/存储空间等信息页 | 中 |
| **手动输入服务器 IP** | 无广播环境（跨网段）下手动配置 | 中 |
| **播放预览** | 编辑端预览播放效果 | 低 |

---

## 三、可替换的技术模块

### 1. 线程模型 → Kotlin Coroutines

**现状**：手动 `new Thread()` + `List<Thread>` + `join()` + `volatile boolean` 标志位

**替换方案**：
```kotlin
// 现在
Thread sceneUpdater = new Thread(new SceneUpdater());
sceneThreads.add(sceneUpdater);
sceneUpdater.start();

// 替换为
val sceneScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
sceneScope.launch {
    while (isActive) {
        delay(sceneInterval * 1000L)
        withContext(Dispatchers.Main) { drawScene(nextScene) }
    }
}
```

**收益**：结构化并发、自动取消、异常传播、代码量减少 60%+

### 2. Handler 消息总线 → LiveData / SharedFlow

**现状**：`Handler.sendMessage(Msg.ordinal, obj)` + switch-case 处理

**替换方案**：
```kotlin
// 定义事件
sealed class AdEvent {
    data class DrawScene(val scene: Scene) : AdEvent()
    data class DrawImage(val bitmap: Bitmap, val rect: Rect) : AdEvent()
    data class DrawVideo(val path: String, val rect: Rect) : AdEvent()
    object TakeScreenshot : AdEvent()
}

// 发送
private val _events = MutableSharedFlow<AdEvent>()
val events = _events.asSharedFlow()

// 接收
lifecycleScope.launch {
    events.collect { event ->
        when (event) {
            is AdEvent.DrawScene -> renderScene(event.scene)
            // ...
        }
    }
}
```

**收益**：类型安全、生命周期感知、无内存泄漏风险

### 3. JSON 解析 → Kotlin Serialization 或 Moshi

**现状**：Gson + 自定义 TypeAdapter（`AdItemTypeSerializer`、`DirectionSerializer`）

**替换方案**：
```kotlin
@Serializable
data class AdProgram(
    val ver: Int = 0,
    val sceneIntval: Int = 10,
    val scene: List<Scene> = emptyList()
)

@Serializable
enum class AdItemType { Video, Picture, Subtitle }
```

**收益**：编译期代码生成、无反射、性能更好、Kotlin 原生支持

### 4. SharedPreferences → DataStore

**现状**：`App.saveString()` / `App.getString()` 基于 SharedPreferences

**替换方案**：
```kotlin
val Context.dataStore by preferencesDataStore(name = "adclient")

// 读取
val idFlow: Flow<String> = dataStore.data.map { it[deviceIDKey] ?: "" }

// 写入
suspend fun saveID(id: String) {
    dataStore.edit { it[deviceIDKey] = id }
}
```

**收益**：异步安全、支持 Flow 观察、无 ANR 风险（SharedPreferences 的 apply 在某些设备上会卡主线程）

### 5. HTTP 客户端 → OkHttp 统一

**现状**：`HttpRequestHelper` 用 OkHttp，`HttpTool` 用 HttpURLConnection，`Updater` 用 HttpURLConnection。三套 HTTP 实现并存。

**替换方案**：统一使用 OkHttp，配置统一的连接池、超时、拦截器。

**收益**：减少代码重复、统一错误处理、支持 HTTP/2

### 6. 视频播放 → ExoPlayer / Media3

**现状**：原生 `MediaPlayer` + 手动管理 `SurfaceView`

**替换方案**：Google ExoPlayer (Media3)
```kotlin
val player = ExoPlayer.Builder(context).build()
player.setMediaItem(MediaItem.fromUri(videoUri))
player.prepare()
player.play()
```

**收益**：
- 更好的格式兼容性（HLS/DASH/WebM）
- 内置缓冲和自适应码率
- 更可靠的错误处理和恢复
- 支持字幕轨切换
- DRM 支持

### 7. 图片加载 → Coil / Glide

**现状**：手动 `BitmapFactory.decodeFile()` + `imageView.setImageBitmap()`，无缓存、无采样、无线程管理

**替换方案**：
```kotlin
imageView.load(imageFile) {
    memoryCachePolicy(CachePolicy.ENABLED)
    diskCachePolicy(CachePolicy.ENABLED)
    size(screenWidth, screenHeight)  // 采样到目标尺寸
    crossfade(true)
}
```

**收益**：自动内存/磁盘缓存、自动采样防 OOM、自动生命周期管理

### 8. UI 框架 → Jetpack Compose（长期）

**现状**：手写 XML Layout + `findViewById` + 手动 `addView`/`removeView`

**替换方案**：
```kotlin
@Composable
fun SceneView(scene: Scene) {
    Box(Modifier.fillMaxSize()) {
        scene.layers.forEach { layer ->
            when (layer) {
                is Video -> VideoPlayer(layer)
                is Picture -> AsyncImage(layer)
                is Subtitle -> ScrollingText(layer)
            }
        }
    }
}
```

**收益**：声明式 UI、自动重组、更好的状态管理、代码量减少

### 9. 网络协议 → gRPC / MQTT

**现状**：自定义 TCP 文本协议 `"/<command>/:<param>"`，无序列化框架，无协议版本管理

**替换方案**：
- **MQTT**：适合 IoT 设备管理，支持遗嘱消息、QoS、发布/订阅模式
- **gRPC**：强类型、双向流、自动代码生成

**收益**：标准化协议、更好的工具链、支持 TLS 加密、协议演进更容易

### 10. 数据库 → Room

**现状**：无数据库，播放历史、统计信息无处存储

**替换方案**：
```kotlin
@Entity
data class PlayRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sceneName: String,
    val mediaPath: String,
    val startTime: Long,
    val duration: Long
)

@Dao
interface PlayRecordDao {
    @Query("SELECT * FROM PlayRecord WHERE startTime > :since")
    fun getRecordsSince(since: Long): Flow<List<PlayRecord>>
}
```

**收益**：支持播放统计、内容管理、离线缓存

---

## 四、改进优先级建议

### P0 — 必须修复（安全和稳定性）

1. 加密密钥外置，IV 随机化
2. 减少 Root 依赖（PackageInstaller API、NTP 时间同步）
3. 实现看门狗机制（应用崩溃自动重启）
4. 存储空间管理和自动清理
5. 断网续播（本地缓存播放）

### P1 — 重要改进（架构和功能）

6. 线程模型重构为 ExecutorService 或 Coroutines
7. Handler 消息系统替换为 LiveData/SharedFlow
8. 统一 HTTP 客户端为 OkHttp
9. 视频播放升级为 ExoPlayer/Media3
10. 图片加载使用 Coil/Glide
11. 添加播放统计功能
12. 远程日志上报
13. 竖屏模式支持

### P2 — 优化提升

14. 添加单元测试（加密、坐标转换、命令解析）
15. SharedPreferences 迁移到 DataStore
16. 硬编码配置外部化
17. 增量更新支持
18. 设备状态监控上报

### P3 — 长期演进

19. Kotlin 迁移
20. Jetpack Compose UI
21. 网络协议标准化（MQTT/gRPC）
22. Room 数据库支持播放历史
23. 多用户权限管理
