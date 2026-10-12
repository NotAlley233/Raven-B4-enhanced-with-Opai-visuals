# Opai visuals

在原 Raven GUI 中展开 **Settings**，开启 **Opai visuals**。**Opai color** 提供 Lavender / Light Pink。默认关闭；关闭开关后恢复 Raven 视觉。Windows x64 原生后端初始化失败时自动沿用 Raven。

同一个设置控制 ClickGUI、灵动岛、ArrayList 和 Target HUD。ArrayList 使用原 HUD 模块的启用状态及筛选选项；Target HUD 使用原目标数据和原禁用逻辑。灵动岛显示玩家、服务器延迟、FPS、模块切换通知以及 Scaffold 方块数。

GUI：左键切换模块，右键展开设置，中键设置绑定，拖动标题移动列，滚轮滚动，Shift + 滚轮横向滚动。右上角 Configurations 对接现有 B4 profiles；右下角铅笔进入 HUD 编辑器，可拖动三个 HUD，滚轮缩放，R 重置，Esc 保存。GUI 配色、开关和 HUD 布局保存至原客户端配置。

## 构建与验证

从 OpenB4 目录运行：

```powershell
.\gradlew.bat build
.\gradlew.bat opaiVisualCheck
.\gradlew.bat opaiMinecraftCheck -PopaiClientCheck
```

需要 Gradle 运行用 JDK 17、编译/游戏用 Java 8。原生 DLL 已随源码和 JAR 提供；更改 C 桥接代码后先执行 `powershell -ExecutionPolicy Bypass -File native/build.ps1 -Jdk <JNI headers JDK>`，需要 Windows x64 MinGW GCC。交付 JAR：`OpenB4/build/libs/RavenB4.jar`。

`opaiVisualCheck` 用真实 WGL/OpenGL 上下文绘制生产 painter，验证交互、GL 状态恢复、灵动岛生命周期、ArrayList 和目标血条，截图写入 `build/opai-verification`。
`opaiMinecraftCheck` 仅在指定参数时加载测试模组，启动隔离的本地超平坦世界，检查 Settings 持久化和实际 Minecraft 渲染，截图及结果写入 `build/opai-client-check`。测试模组不包含在发布 JAR 内。

## 还原范围与来源

按固定参考版本复用原字体、调色板、图标、target 纹理、几何尺寸及动画公式；Minecraft 1.8.9 的 blur 和装备图标沿用 B4 渲染。不把 GPU 冒烟测试等同于逐像素 100% 对照验证；参考项目游戏版本、窗口缩放和渲染环境会影响最终观感。

出处和许可证：`OpenB4/src/main/resources/assets/raven/opai/licenses/THIRD-PARTY.txt`。原生源码随交付保存，可复现构建。

## 本次验证结果

Windows x64 / NVIDIA RTX 5070 Ti 上已实际运行 GPU 验证和 Minecraft 1.8.9 本地超平坦世界验证：Settings 开关、配色重载、四套渲染与 Raven 回退通过。小窗口 Settings 快捷入口会横向滚动到 Misc 列。另修复原项目 ScriptReflection 缺少开发环境标题字段名导致的启动空指针。
