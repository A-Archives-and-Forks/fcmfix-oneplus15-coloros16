# FCMFix ColorOS 53-coloros-10-rc1

ColorOS 17（Android 17）适配候选版。包名、签名、推荐作用域不变，versionCode 升至 62，可覆盖安装；
安装后需重启。

适配依据为一加 15 国行全量包 `PLK110_17.0.0.102(CN01)` 的静态核对，详见
[ColorOS 17 Hook 核对](../oneplus15-coloros17-fcm-analysis.md)。

## 改动

- 广播 Hook 增加 `broadcastIntentLockedTraced`，并对 AMS / BroadcastController 中
  `broadcastIntentWithFeature`、`broadcastIntentInPackage`、`broadcastIntentLocked` 执行 deoptimize，
  避免 Android 17 AOT 内联导致 Hook 不触发。不处理 Binder Stub。
- 锁屏快速冻结：ColorOS 17 将 `HansCGroup.FastFreezeEnter(int)` 改为 `fastFreezeEnter(int, String)`，
  两种签名均精确匹配，FCM 投递窗口内的 UID 不再被锁屏快冻。
- 新增 Osense CpnProxy 广播代理（`BroadcastProxyAction.enqueueProxyBroadcastLocked`）的防御性放行，
  只放行按 `BroadcastRecord` 核验来源的可信 FCM。
- ColorOS 17 新增的无 Intent `isAppClassifyRestricted` 重载不再产生误导性的“Unsupported”日志。
- system_server 启动日志记录 `sdk`、`ro.build.version.oplusrom.display` 与 `Build.DISPLAY`。
- FCM 信任校验、允许列表、20 秒投递窗口、电池作用域逻辑均未修改。其余 ColorOS 专有签名经核对与 ColorOS 16 一致。

## 验证情况

- 44 项单元测试通过；lintDebug / lintRelease 无错误；Debug / Release 构建成功。
- 仅做了固件静态核对，**尚未在 ColorOS 17 真机上运行**。升级后请确认 LSPosed 日志中：
  - `[fcmfix] firmware:` 显示 `sdk=37` 与 ColorOS 17 版本；
  - 出现 `Oplus Hans fast-freezer hook active`、`Oplus CpnProxy broadcast hook active`、
    `broadcast callers deoptimized: N`（N > 0）；
  - 没有 `hook error` 与 `Unsupported Oplus delivery signature`。
- 真机回归清单同 [发布加固记录](../release-hardening.md)：普通后台、锁屏冻结、force-stop 后的推送，
  以及非允许列表与伪造 RECEIVE 的拒绝。
