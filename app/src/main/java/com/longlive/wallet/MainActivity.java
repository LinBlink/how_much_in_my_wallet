package com.longlive.wallet;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
  TextView result;
  @Override public void onCreate(Bundle b) { super.onCreate(b); buildUi(); }
  void buildUi() {
    LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(32,32,32,32);
    TextView title = new TextView(this); title.setText("钱包余额读取"); title.setTextSize(26); title.setTextColor(Color.DKGRAY); box.addView(title);
    TextView hint = new TextView(this); hint.setText("仅读取已显示在微信/支付宝页面上的文字，不保存账号、密码或支付凭证。\n首次使用请开启无障碍权限。"); hint.setPadding(0,20,0,20); box.addView(hint);
    Button access = button("打开无障碍设置"); access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); box.addView(access);
    Button wx = button("打开微信余额页面"); wx.setOnClickListener(v -> open("com.tencent.mm")); box.addView(wx);
    Button ali = button("打开支付宝余额页面"); ali.setOnClickListener(v -> open("com.eg.android.AlipayGphone")); box.addView(ali);
    Button read = button("读取当前页面"); read.setOnClickListener(v -> { if (WalletAccessibilityService.instance != null) WalletAccessibilityService.instance.readNow(); else Toast.makeText(this,"请先开启无障碍服务",Toast.LENGTH_LONG).show(); }); box.addView(read);
    Button json = button("复制结果 JSON"); json.setOnClickListener(v -> { String s=WalletAccessibilityService.resultJson(this); ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("wallet",s)); Toast.makeText(this,"已复制: "+s,Toast.LENGTH_SHORT).show(); }); box.addView(json);
    result = new TextView(this); result.setTextSize(18); result.setPadding(0,24,0,0); box.addView(result); setContentView(box); refresh();
  }
  Button button(String s) { Button b=new Button(this); b.setText(s); return b; }
  void open(String pkg) { try { Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i==null)throw new Exception(); startActivity(i); } catch(Exception e) { Toast.makeText(this,"未安装该应用",Toast.LENGTH_SHORT).show(); } }
  void refresh() { result.setText(WalletAccessibilityService.resultJson(this)); }
}
