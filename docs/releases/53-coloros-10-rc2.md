# FCMFix ColorOS 53-coloros-10-rc2

修复 ColorOS 17 上 FCM 连接不上或熄屏后断开的问题。包名、签名不变，versionCode 升至 63，
可直接覆盖 rc1；安装后需重启。**请确认 LSPosed 作用域同时勾选“系统框架”和“电池”。**

## 原因

ColorOS 17 电池组件（`com.oplus.battery`）在 Google 连通性探测失败时，除了此前已处理的
`setUidPolicy(uid, POLICY_REJECT_ALL)` 外，还会广播 `oplus.intent.action.google_restrict_change`
（`restrict_enable=true`）。system_server 收到后：

- `OplusGoogleAlarmRestrict` 把 GMS 等 Google 包的 `RTC_WAKEUP` / `ELAPSED_REALTIME_WAKEUP` 闹钟
  降级为不唤醒类型，熄屏 Doze 时 FCM 心跳与重连闹钟不再触发；
- `AppStandbyControllerExtImpl` 把 Google 包放入 RARE 待机分组（bucket 40），作业、闹钟和联网都会受限。

国内网络环境下该探测很容易失败，因此限制很容易触发。rc1 未覆盖这两层。

## 改动

- 电池作用域：拦截电池进程发出的 `google_restrict_change` 广播，只把 `restrict_enable=true` 改为 `false`，
  名单更新照常下发。闹钟降级、RARE 待机分组以及网络策略监听都不会进入受限状态。
- 系统框架作用域（兜底）：`OplusGoogleRestrictionHelper.isGoogleRestrct()` 返回 `false`，已被降级的
  GMS 闹钟在下一次重排时恢复为唤醒类型。它只被闹钟限制使用。未勾选电池作用域时此项仍可避免心跳闹钟被压住，
  但不能避免 RARE 分组，所以仍需勾选电池作用域。
- 其余逻辑与 rc1 相同。

## 验证

- 44 项单元测试通过；lintDebug / lintRelease 无错误；Debug / Release 构建成功。
- 静态依据：`PLK110_17.0.0.102(CN01)` 的 `Battery.apk`、`oplus-service-jobscheduler.jar`、`oplus-services.jar`。
  尚未真机验证。升级后日志中应出现：
  - `Oplus Battery Google restrict broadcast hooks active`（电池进程）；
  - `Oplus Google alarm restriction hook active`（system_server）；
  - 探测失败时出现 `Oplus Battery Google restrict broadcast cleared`。
- 若已处于受限状态，重启后才会完全恢复。可用 `adb shell dumpsys usagestats` 确认
  `com.google.android.gms` 不在 bucket 40；用 `adb shell dumpsys alarm` 确认 GMS 心跳闹钟为 `*_WAKEUP` 类型。
