# 如何提交问题与日志

收不到推送时，请按下面的步骤收集信息，然后[提交 Issue](https://github.com/Artifical0/fcmfix-coloros/issues/new/choose)。
FCMFix 的日志写在系统 logcat 中，标签为 `fcmfix`，不在 LSPosed 管理器的“模块日志”页面里，需要用下面的脚本导出。

## 1. 先自查

- LSPosed 中 FCMFix 同时勾选了 **系统框架** 和 **电池** 两个作用域，改动后已重启；
- 只启用了一个 FCMFix；
- 目标应用已加入 FCMFix 允许列表，应用自身的通知权限已打开；
- 打开 FCMFix →“打开 FCM Diagnostics”，查看连接状态。

**国内网络说明**：FCMFix 修复的是 GMS 收到消息之后被 ColorOS 拦截的问题，不能让 GMS 连上 FCM 服务器。
如果 FCM Diagnostics 亮屏时也一直是 disconnected，通常是网络问题：`mtalk.google.com` 的 DNS 被污染，
或者 5228–5230 端口被封。这种情况需要能返回正确结果的 DNS、hosts 或代理，模块无法解决。

## 2. 导出报告（需要 Root）

1. 下载 [`collect-report.sh`](../scripts/collect-report.sh)（点击后选“Download raw file”），保存到手机的 `Download` 目录。
2. **重启手机**，开机后 3–5 分钟内，在 Termux 或 `adb shell` 中执行：

   ```sh
   su -c sh /sdcard/Download/collect-report.sh fcmfix-report-boot.txt
   ```

   开机时模块会打印各个 Hook 是否生效，logcat 缓冲区有限，时间长了这些行会被冲掉。
3. **复现问题后**再执行一次，例如熄屏等待 10 分钟，再发一条测试消息：

   ```sh
   su -c sh /sdcard/Download/collect-report.sh fcmfix-report-issue.txt
   ```

两份报告都保存在 `Download` 目录。脚本只读取系统状态和日志，不修改任何设置。

不方便下载脚本时，也可以直接复制 [`collect-report.sh`](../scripts/collect-report.sh) 的内容，保存为同名文件后执行。

## 3. 提交时附上

- 上面两份报告；
- FCM Diagnostics 截图；
- LSPosed 中 FCMFix 作用域截图；
- 网络环境：Wi‑Fi 或移动数据，是否使用代理；
- 测试应用，以及发送测试消息的大致时间。

报告中包含已安装应用的包名，公开发布前可以自行打码，但请保留 `fcmfix`、`GoogleController`、`OplusGoogle` 相关行。

## 报告怎么看

| 报告内容 | 含义 |
| --- | --- |
| “GMS 联网策略”下出现 `REJECT_ALL` | GMS 被禁止联网。通常是电池作用域未生效（没勾选，或勾选后没有重启） |
| 待机分组为 `40`，或 `google_restric_info` 为 `1` 但日志中没有 `restrict broadcast cleared` | ColorOS 17 的 Google 限制未被解除，需要 53-coloros-10-rc2 或更新版本 |
| 日志中有 `hook error` 或 `Unsupported` | 某个 Hook 与当前固件不匹配，请在 Issue 中贴出这些行 |
| 以上均正常，但 FCM Diagnostics 一直 disconnected | 网络问题（DNS 污染或端口被封），见上文国内网络说明 |
