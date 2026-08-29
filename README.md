# 钱包余额读取（Android）

这是一个 Android 专用辅助 App，用 Android 无障碍服务读取**当前已经显示在屏幕上的**微信或支付宝余额。它不登录账户、不保存密码，也不会模拟支付。

## 使用

1. 安装 Debug APK。
2. 打开 App，进入“无障碍设置”，启用“钱包余额读取”。
3. 点击“打开微信余额页面”或“打开支付宝余额页面”，在对应 App 中手动导航到余额页面。
4. 返回本 App，点击“读取当前页面”。
5. 点击“复制结果 JSON”可将结果交给后续 Pebble companion 集成。

读取结果保存在 App 私有 SharedPreferences 中，格式为：

```json
{"wechat":"123.45","alipay":"67.89","updatedAt":1710000000000}
```

## 重要限制

余额识别依赖页面无障碍文本。微信/支付宝改版、金额被拆成多个文本节点、余额隐藏或需要人脸/密码验证时，读取可能失败。App 不会绕过验证，也不会在后台自动点击支付按钮。

当前工作区没有 Android SDK，因此构建需在安装 Android Studio/SDK 的机器上执行：

```text
gradlew.bat assembleDebug
```
