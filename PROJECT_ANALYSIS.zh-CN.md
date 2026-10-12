# OpenB4 项目分析

分析日期：2026-10-12。对象：`C:\dev\Raven B4`，主工程 `OpenB4`。

## 结论与证据边界

这是从 Raven B4 客户端恢复的 Minecraft 1.8.9 Forge 客户端模组，具有实际的模块逻辑、Mixin 注入、GUI、配置及 Java 脚本运行系统。它不是仅用于展示的 ClickGUI，也不能据构建成功认定原客户端功能已经全部恢复。部分后台功能被明确移除，部分名字仍存在于分发代码中，但没有对应模块。

本次执行了源码清单与资源解析、核心调用链检查、网络调用点搜索、构建复核和 class 文件版本检查。主工程共有 219 个 Java 文件、47,299 行文本（含空行和注释）；独立脚本解密器不计在内。已检查所有模块注册映射并重点阅读核心公共层与代表性模块；没有逐个模块进行游戏内行为验证，没有运行未知脚本，没有进行依赖二进制安全审计。

2026-10-12 的复核构建输出：`BUILD SUCCESSFUL in 1s`，4 项任务均为 UP-TO-DATE。前一轮首次构建实际执行了 compileJava、processResources、jar、remapJar，输出 `BUILD SUCCESSFUL in 43s`。test 为 NO-SOURCE；因此没有自动化测试通过这一证据。尚未启动游戏验证 Mixin 注入、OpenGL 渲染或服务器交互。

## 工程与构建

| 项目 | 当前内容 |
|---|---|
| 主工程 | OpenB4 |
| 独立工具 | ScriptDecrypting/ScriptDecrypter.java，未纳入主 Gradle 工程 |
| 游戏 | Minecraft 1.8.9 |
| Forge | 1.8.9-11.15.1.2318-1.8.9 |
| MCP | mcp_stable:22-1.8.9 |
| Gradle | wrapper 配置指定 8.8 |
| Loom | 声明 0.10.0.+，本机实际解析为 0.10.0.5 |
| Java | toolchain 指定 8；入口 class major 实测为 52 |
| 内嵌依赖 | libs/mixin-0.7.11-SNAPSHOT.jar |
| 模组标识 | clickgui，显示名称 ClickGUI，版本 1.0.0 |
| 账户显示常量 | UID=1，VERSION=premium-20 |

构建流程：Java 编译及 Mixin 注解处理 → 资源版本替换 → 将 Mixin 本地 JAR 合入普通 JAR → remapJar 重映射为发布 JAR。清除嵌入依赖原有签名和注解处理器服务声明；manifest 指定 MixinTweaker、MixinConfigs、ForceLoadAsMod 等属性。

发布产物：`C:\dev\Raven B4\OpenB4\build\libs\RavenB4.jar`。

- 大小：2,224,976 字节。
- SHA256：`17DF86C60A6D2D9C9AA64619B50D131D7F6CD3E3CDE32834BB2541E28971B563`。
- 已打开 JAR 检查：存在入口类、mcmod.info、clickgui.mixins.json、clickgui.refmap.json、MixinTweaker.class。
- README 的 `.\gradlew.bat build` 不能直接使用：仓库仅有 gradle-wrapper.properties，缺少 wrapper 脚本和 wrapper JAR。本次使用用户 Gradle 缓存中的 8.8 执行。
- Gradle 输出了旧 API 警告，提示当前构建不兼容 Gradle 9。不能直接升级 wrapper 后假定仍然可用。
- 动态插件版本、未完整提交的 wrapper、本地二进制依赖缺少校验记录，使跨机器复现弱于本机复现。

系统默认 java 为 Java 21，这与编译目标 Java 8 是两回事；入口 class 版本 52 已验证。不能据默认 java 版本推断 Gradle 未使用 Java 8 工具链。

## 目录分工

| 包 | Java 文件数 | 职责 |
|---|---:|---|
| 根入口 | 1 | Forge 初始化和客户端 Tick |
| client | 18 | 配置、档案、聊天命令、好友、服务器状态、HUD、披风 |
| event | 1 | 可修改 MotionEvent |
| gui | 17 | 分类面板、设置组件、控制台及 HUD 编辑器 |
| hook | 1 | Hooks，集中承接注入和模块调度 |
| mixin | 15 | Minecraft、实体、输入、网络、字体与渲染注入/访问器 |
| module 及子包 | 120 | 模块基类、设置、各功能模块 |
| script | 15 | 编译、加载、事件、反射桥、包装 API、解密 |
| util | 31 | 旋转、数据包、渲染、方块、物品、动画、线程池 |

资源 assets/clickgui/modules.json 有 112 项声明：111 项有命名分类，另有 1 项通过 failed 分支创建 ShadersModule。运行时脚本和档案可以继续添加模块，因此 112 不代表所有时刻的运行时模块总数。

## 启动与事件链

1. `ClickGuiMod.init()` 调用 `ModuleManager.init()`。
2. ModuleManager 读取 modules.json，根据名称构造具体类型，再加载每项设置并调用 link() 绑定静态设置引用。初始化默认启用 AntiBot、Chat Commands、Sprint。
3. 入口初始化脚本反射及委托事件，创建 ClickGui 和分类组件。
4. ClientConfig.load() 恢复窗口位置、好友/敌人、默认档案名称，并加载档案目录。
5. 注册入口、RenderDispatcher、Keystrokes 等 Forge 监听器与命令。
6. 第一次有玩家和世界的 Tick 调用脚本 reload、默认档案加载、Zoom.find。firstTick 是进程级一次性开关，后续换世界不会重做这段初始化。
7. Tick END 处理 PacketQueue、按键边沿、模块 onUpdate、HUD 排序、IRC 快捷入口及声音队列。

行为不仅由 Forge 事件驱动。Mixin 调用 Hooks，Hooks 再按固定顺序调用模块及脚本。移动链大致为：玩家更新 → preUpdate → RotationManager 仲裁 → 可修改 MotionEvent → 行走更新数据包生成 → postMotion。输入完成后另有 postPlayerInput 与移动方向修正。

Hooks.onUpdateWalkingPlayer 重建了关键行走同步逻辑，涉及冲刺/潜行、坐标/旋转变化阈值、骑乘与服务器上次状态；这属于对原版行为的深度接管，需游戏内测试，不只是普通事件监听。

## 模块系统

Module 提供启停、按键、HUD 动画时间戳和大量默认空行为接口。enable 将模块加入 enabledModules，普通模块注册 Forge 总线并调用 onEnable；disable 执行反向操作，可将后续清理放入 afterDisableQueue。脚本模块走 ScriptManager，档案模块触发 Profiles.enable。

ModuleManager 同时维护 modules、hudOrder、enabledModules、名称索引、小写名称索引、分类索引等。名称查询通常为 HashMap 查询；但很多调用点依赖字符串和强制类型转换，缺少编译期约束。新增模块通常必须同时维护 JSON、构造分支、link() 设置名和相关 Hooks 阶段，遗漏其中一项可能构建正常、运行失效。

分类功能：

- combat：点击、瞄准、目标选择、攻击距离/碰撞箱、击退与攻击状态。KillAura 1,482 行，是最复杂模块之一。
- movement：速度、飞行、跳跃、输入移动、计时器与传送等。
- player：物品栏、自动放置、搭桥、Tower、Freecam、NoFall 等；Scaffold 1,301 行，关联旋转、选槽、队列和渲染。
- network：Blink、Freeze、Backtrack、Fake Lag、Lag Range 等延迟/缓存控制。
- render：ESP、HUD、相机、标签、轨迹、Shader 等。
- minigames：基于服务器状态、聊天、记分板和地图对象推断的小玩法辅助；SpeedBuilders 1,253 行。
- world：AntiBot、Weather。
- other/client：聊天、命令、诊断、界面和全局设置。

两个通用 Module 值得区分：Name Hider 的功能由 Hooks.renderString 实现，因此没有独立子类不代表无功能；Chat Bypass 则只调用基类默认放行的 onSendPacket，本次未找到对应的实际绕过实现。

Hooks 和 RotationManager 仍引用 AutoRod、WoolWars；模块声明和构造映射中没有这两项。当前查询返回 null，通常跳过，不应把这些残留分支算作已恢复功能。

## 旋转、输入与共享状态

RotationManager 的 Provider 数组按固定优先级请求旋转，第一个返回非 null 的模块获胜，最后才查询脚本。部分 Provider 在模块未启用时仍被查询，由模块自身判断是否有过渡行为。serverYaw/serverPitch、玩家真实视角和显示模型旋转被分别管理；有 movement fix、身体/头部插值及搭桥假旋转。

这有明确仲裁顺序，减少多个模块直接抢写旋转，但优先级写死在公共层；扩展时应明确抢占、释放及禁用过渡规则。基类拥有大量与特定模块相关的接口，公共抽象已经偏大。

## 数据包链与线程边界

发送链：NetworkManager.sendPacket 的 HEAD 注入 → Hooks.onSend → 特殊跳过标记 → 脚本发送回调 → 聊天命令/IRC → 模块拦截 → Lag Range/Fake Lag/PacketQueue → finalSendCheck。返回 false 时取消原发送。

接收链：channelRead0 内 processPacket 调用被重定向 → Hooks.onReceive → 记录/脚本/模块拦截 → 原 packet.processPacket(handler)。本地注入点在 Netty 接收路径上，拦截逻辑发生在原版包处理方法可能执行的线程调度之前；不能假设全部回调在游戏主线程。

PacketUtils 用 sendingNoEvent、sendingQueued、sendingSkipScripts 等全局引用表示发送模式。PacketQueue 用 active、holders 以及 List 保存待发包，通过引用计数协调多个持有方；换世界或死亡清理队列，Blink 可维持持有状态。

主要待验证风险：主线程与 Netty 回调共用模块列表、脚本 HashMap、状态字段；部分模块有显式锁，但公共状态没有统一线程所有权。PacketUtils 的全局标记也没有 try/finally 恢复，异常/嵌套发送可能留下错误模式。PacketQueue 没有统一包数、内存或持有时间上限，应测试长时间持有和释放顺序。这些是源码可见的风险条件，未在游戏中复现竞态或队列故障。

## GUI 与渲染

ClickGui 基于 GuiScreen，CategoryComponent 负责分类，ModuleComponent 及 Button/Slider/Bind/Description 组件负责模块交互。提供分类拖动、GUI 比例、背景模糊、水印动画、玩家模型、内置控制台和多个 HUD 编辑器。关闭 GUI 时恢复游戏 GUI 比例、取消 Timer、更新模块设置并按配置保存分类位置。

RenderDispatcher 汇集 RenderWorldLast、RenderTick、名字渲染事件，协调玩家 ESP、Nametags、BedESP、追踪和叠加信息；Mixin 进一步控制相机、实体模型、字体和物品渲染。Render3D、Shader、Blur、GlowShader 等封装 OpenGL 工具。

优化与回归测试应优先关注多模块同时启用的实体遍历、Shader/FBO 生命周期、窗口缩放后资源尺寸、GL 状态恢复，以及关闭/重开界面。这里没有实测帧率证据，不能据源码体积给出性能结论。ClickGui 的 TimerTask 在后台线程改动画字段，关闭界面则由 UI 线程清理，应验证取消时序。

## 配置与档案

数据目录为游戏 mcDataDir 下的 `keystrokes`：config.txt 保存默认档案、分类位置、好友和敌人；profiles/*.json 保存模块状态、按键、设置、HUD 坐标与部分文本；scripts/*.java 为脚本入口。另有 Keystrokes 与小游戏数据保存逻辑。

档案加载会先清空当前档案引用和 modified 标记，并执行 panic 关闭模块，再解析和逐项应用 JSON。读取普通 Slider 时调用 setValueRaw，模式设置则用 setValue。保存时遍历模块、跳过档案和 Manager 类模块，保存 marked-as-saved 设置。

已确认的问题：

1. `Profiles.remove()` 备份 renameTo 失败后会执行 file.delete。备份失败反而可能删除唯一原档案，属于优先修复的数据丢失路径（Profiles.java:125）。
2. `Profile.load()` 在解析成功前 panic，后续逐项修改；中途失败返回 false，不能恢复先前状态。损坏档案可导致原模块已关闭或部分设置已改（Profile.java:44）。
3. Profile.save 和 ClientConfig.save 使用 FileWriter(false) 原地截断写入；没有临时文件+原子替换，写入故障可能破坏已有配置（Profile.java:145；ClientConfig.java:31）。
4. 非模式滑条通过 setValueRaw 绕过 min/max/step 校验，外部编辑配置可以写入界面范围外的值；这是明确行为，但是否造成故障取决于使用点（Profile.java:98）。
5. 配置使用默认平台编码和自定义分隔符解析，迁移机器、特殊文本和损坏文件的处理较弱；多处吞掉异常，用户只见失败或没有反馈。

## Java 脚本系统

ScriptManager 从 scripts 目录读取 .java，名称限制、识别加密行、检查源码，基于 name+source 的 MD5 做复用识别。Script 为源码加固定 imports 并继承 ScriptDefaults，用 ToolProvider 的 JavaCompiler 动态编译到固定临时目录，再用 URLClassLoader 加载和实例化，清理 class 文件；unload 会关闭类加载器。

ScriptDefaults 有 5,302 行，提供世界/实体/物品/向量、渲染、输入、配置、模块、数据包等包装 API，保留明显反编译及常量混淆痕迹。ScriptEvents 连接 Forge 和 Hooks；ScriptDelegated 处理部分委托，ScriptInvoker 用反射/MethodHandle 适配回调，ScriptReflection 封装原版成员访问。

运行脚本需要完整 JDK 的系统编译器；普通 Java 8 JRE 下 ToolProvider.getSystemJavaCompiler 可返回 null。当前 reload 直接返回，缺少明确的用户诊断。因此“模组能加载”与“脚本可编译运行”是不同验收项。

源码检查会解码 Unicode 转义、移除 identifier-ignorable 字符、去注释，再检查禁用字符串与混淆方法名。它仍是字符串黑名单，不是隔离沙箱；没有独立进程、时间预算或可靠终止机制。脚本运行在宿主 JVM，死循环或耗时回调可能卡住调用线程。不应将黑名单当作不可信脚本的安全边界。

ScriptInvoker.findMethod 只比较方法名、参数个数、返回类型，没有核对各参数类型。相同元数的重载可能选错，invoke 的 IllegalArgumentException 也不在对应的两类 catch 内；应改为完整签名匹配（ScriptInvoker.java:95）。脚本集合是 HashMap，多个脚本旋转回调“第一个返回值”的次序没有显式优先级契约。

## 解密工具

ScriptDecrypting/ScriptDecrypter.java 是独立命令行工具，接收输入文件与输出文件。实现 Base64/AES 外层和 Hex/XOR 链内层，使用固定密钥与 UID=1 的文本替换；对应客户端 ScriptCrypto/ScriptEvent 的保护脚本恢复机制。

工具直接取 a[0]、a[1]，依赖引号分隔和合法密文，没有参数数量、输入结构或目标覆盖确认。适合受控输入，不是完善的通用 CLI。本次没有样本输入，未声称完成解密正确性测试。

## 外部通信与功能缺口

README 声明移除 API 和后台引用。当前 Java 源码搜索未发现实际 HttpURLConnection/Socket/WebSocket 创建或执行系统进程的实现；java.net 引用主要用于源码 URI 与类加载定位。这只是源码范围观察，不是对第三方 JAR 或运行时脚本的安全保证。

明确缺口：

- IRC：保留聊天入口、清洗、限流、计数和快捷键，但未见连接/发送后端。connected 没有本项目连接代码设置；通常显示未连接。即使人为置 connected=true，onChat 也仅计数并消费消息。
- VPN Status：canBeEnabled 始终显示错误并返回 false。
- Duels Stats：保留 plancke URL 和页面解析，私有 get 方法直接返回空字符串，查询不能返回真实远端数据（DuelsStatsModule.java 中 get）。
- 脚本 HTTP：ScriptDefaults.Request.fetch 直接返回 null（ScriptDefaults.java:5239）。保留请求类与 setter 不代表有 HTTP 能力，现有脚本若解引用返回值会失败。
- Chat Bypass：通用 Module，没有实际包改写实现。
- UID=1 及 isDev=true 是本地常量行为，不能证明任何真实服务端权限。

## 质量与维护优先级

| 优先级 | 动作 | 验收 |
|---|---|---|
| 高 | 修复备份失败删除原档案 | 模拟备份失败时源文件保持完整 |
| 高 | 档案先解析校验，再提交；保存使用原子替换 | 损坏输入不改当前运行状态，写失败不丢旧文件 |
| 高 | 明确网络/游戏/脚本线程所有权 | 换世界、禁用、重载与接收并发时无错序/集合异常 |
| 高 | 对脚本编译器缺失、长回调和 HTTP 停用提供明确反馈 | 用户能区分不支持、加载失败与运行失败 |
| 中 | 修复反射参数匹配及发送标记 finally 清理 | 重载、异常与嵌套发送走正确路径 |
| 中 | 固定插件版本、补全 wrapper、记录二进制依赖校验 | 干净机器可复现构建 |
| 中 | 对 modules.json 做 schema/设置绑定校验 | 缺失设置和重复模块在构建阶段报错 |
| 中 | 显示停用功能并清理失效分发分支 | UI 展示与可用行为一致 |
| 后续 | 拆分 Hooks、ScriptDefaults、大模块 | 通过原行为测试后再替换公共层 |

不建议先进行大范围重命名或重写架构。先建立有价值的验证：配置解析和原子保存的纯 Java 测试、反射签名测试、旋转优先级/队列所有权测试、Forge 启动与 Mixin 应用日志、进入/退出世界和多模块交互的人工回归。当前缺少自动化用例，源码有 144 个 catch 位置，很多吞异常；应先为关键路径建立可定位日志，避免把“不崩溃”误作“功能正常”。

## 本次操作范围

未修改 Java、资源或构建配置。仅新增本分析报告、112 项模块清单和 219 个源码文件的行数/哈希清单。分析期间一次 PowerShell 查询误用了 shell 花括号路径展开，产生 ParserError/exit 1；改为显式路径后查询成功，未影响文件或构建。模块清单生成时，无名称的 failed 项曾触发非终止的 null 索引错误（命令 exit 0）；已对该项显式重建映射，并逐项验证 112 项名称和全部源码路径。对没有修改的源码不制作形式化回滚副本。
