package com.longlive.wallet;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
  private static final int SMS_PERMISSION_REQUEST = 41;

  private TextView status;
  private TextView result;
  private boolean receiverRegistered;
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      refresh();
    }
  };

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    buildUi();
    ensureSmsPermission();
  }

  @Override protected void onStart() {
    super.onStart();
    IntentFilter filter = new IntentFilter(NanjingBankSms.ACTION_UPDATED);
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED);
    } else {
      registerReceiver(receiver, filter);
    }
    receiverRegistered = true;
    refresh();
  }

  @Override protected void onStop() {
    if (receiverRegistered) unregisterReceiver(receiver);
    receiverRegistered = false;
    super.onStop();
  }

  private void buildUi() {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(40, 48, 40, 40);

    TextView title = new TextView(this);
    title.setText("南京银行余额");
    title.setTextSize(28);
    title.setTextColor(Color.DKGRAY);
    box.addView(title);

    TextView hint = new TextView(this);
    hint.setText("仅处理号码 106980095302 发来的南京银行余额短信。短信只在本机解析，不上传。新短信会自动同步到 Pebble 表盘。");
    hint.setTextSize(16);
    hint.setPadding(0, 24, 0, 24);
    box.addView(hint);

    Button permission = button("授予短信权限");
    permission.setOnClickListener(v -> ensureSmsPermission());
    box.addView(permission);

    Button importLatest = button("读取最近短信并发送到手表");
    importLatest.setOnClickListener(v -> importLatestBankSms(true));
    box.addView(importLatest);

    Button resend = button("重新发送已保存余额");
    resend.setOnClickListener(v -> resendSavedBalance());
    box.addView(resend);

    Button appSettings = button("打开系统应用设置");
    appSettings.setOnClickListener(v -> startActivity(new Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:" + getPackageName()))));
    box.addView(appSettings);

    status = new TextView(this);
    status.setPadding(0, 28, 0, 8);
    status.setTextColor(Color.rgb(25, 118, 210));
    box.addView(status);

    result = new TextView(this);
    result.setTextSize(22);
    result.setTextColor(Color.DKGRAY);
    box.addView(result);
    setContentView(box);
  }

  private Button button(String text) {
    Button button = new Button(this);
    button.setText(text);
    return button;
  }

  private void ensureSmsPermission() {
    if (android.os.Build.VERSION.SDK_INT < 23 || hasSmsPermissions()) {
      importLatestBankSms(false);
      return;
    }
    requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS},
        SMS_PERMISSION_REQUEST);
  }

  private boolean hasSmsPermissions() {
    return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
  }

  private void importLatestBankSms(boolean showMessage) {
    if (android.os.Build.VERSION.SDK_INT >= 23 && !hasSmsPermissions()) {
      ensureSmsPermission();
      return;
    }
    NanjingBankSms bank = BankSmsImporter.importLatest(this);
    if (showMessage) {
      toast(bank == null ? "未找到符合格式的南京银行短信" : "已发送余额到手表");
    }
    refresh();
  }

  private void resendSavedBalance() {
    NanjingBankSms bank = NanjingBankSms.load(this);
    if (bank == null) {
      toast("暂无已保存余额");
      return;
    }
    PebbleBalanceSender.send(this, bank);
    toast("已重新发送到手表");
  }

  private void refresh() {
    boolean granted = android.os.Build.VERSION.SDK_INT < 23 || hasSmsPermissions();
    status.setText(granted ? "短信权限：已授予" : "短信权限：未完整授予");
    NanjingBankSms bank = NanjingBankSms.load(this);
    result.setText(bank == null ? "暂无有效余额短信" : bank.displayText());
  }

  @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                    int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != SMS_PERMISSION_REQUEST) return;
    boolean granted = grantResults.length >= 2;
    for (int grant : grantResults) granted &= grant == PackageManager.PERMISSION_GRANTED;
    if (granted) importLatestBankSms(false);
    else toast("需要短信权限才能读取南京银行余额通知");
    refresh();
  }

  private void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
  }
}
