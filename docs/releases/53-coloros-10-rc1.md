# FCMFix ColorOS 53-coloros-10-rc1

ColorOS 17（Android 17）适配候选版。包名、签名、推荐作用域不变，versionCode 升至 62，可覆盖安装；
安装后需重启。

## 改动

- 广播 Hook 增加 `broadcastIntentLockedTraced`。Android 15 起广播主体逻辑在该方法内，
  Android 17 的 AOT 编译可能把较短的 `broadcastIntentLocked` 包装层内联，导致原 Hook 不触发。
- 对 AMS / BroadcastController 中的 `broadcastIntentWithFeature`、`broadcastIntentInPackage`、
  `broadcastIntentLocked` 执行 deoptimize，使被内联的 Hook 重新生效。只处理框架侧广播调用链，
  不处理 Binder Stub，额外的解释执行开销限定在这几个方法内。
- system_server 启动日志记录 `sdk`、`ro.build.version.oplusrom.display` 与 `Build.DISPLAY`，
  便于把 Hook 失配日志对应到具体 OTA。
- FCM 信任校验、允许列表、20 秒投递窗口、电池作用域逻辑均未修改。

## 未验证项

截至 2026-09-29，一加 15 的 ColorOS 17 正式版尚未推送（官方计划 10 月 8 日起），本版没有在
ColorOS 17 真机上运行，也没有核对 ColorOS 17 的 `services.jar` / `oplus-services.jar`。
以下 ColorOS 专有 Hook 仍按 ColorOS 16 签名精确匹配，ColorOS 17 若修改签名会失效但保持系统
原行为（fail closed），并在 LSPosed 日志中输出：

- `Unsupported Oplus delivery signature: ...`（`isAppClassifyRestricted`、
  `isAllowStartFromBindService`、`isAllowStartFromStartService`）；
- `hook error <名称>: NoSuchMethodError / NoClassDefFoundError ...`（Hans、OAppNetControlService、
  OplusProxyWakeLock、OplusDeviceIdleHelper、电池进程 `setUidPolicy` 等）。

升级 ColorOS 17 后请提供 `[fcmfix] firmware:` 行及上述日志，或导出
`/system/framework/services.jar` 与 `/system_ext/framework/oplus-services.jar`，用于按实际签名适配。
