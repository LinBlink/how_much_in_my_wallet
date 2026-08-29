package com.longlive.wallet;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;
import java.util.regex.*;

public class WalletAccessibilityService extends AccessibilityService {
  static WalletAccessibilityService instance;
  static final String WX="wechat_balance", ALI="alipay_balance";
  @Override public void onServiceConnected(){ instance=this; }
  @Override public void onDestroy(){ instance=null; super.onDestroy(); }
  @Override public void onAccessibilityEvent(AccessibilityEvent e){ if(e.getPackageName()!=null && (e.getPackageName().toString().contains("tencent.mm")||e.getPackageName().toString().contains("Alipay"))) readNow(); }
  @Override public void onInterrupt(){}
  public void readNow(){ AccessibilityNodeInfo root=getRootInActiveWindow(); if(root==null)return; ArrayList<String> texts=new ArrayList<>(); collect(root,texts); String pkg=root.getPackageName()==null?"":root.getPackageName().toString(); String key=pkg.contains("tencent.mm")?WX:pkg.contains("Alipay")?ALI:null; if(key==null)return; String val=findMoney(texts); if(val==null)return; getSharedPreferences("wallet",0).edit().putString(key,val).putLong(key+"_at",System.currentTimeMillis()).apply(); sendBroadcast(new Intent("com.longlive.wallet.BALANCE_UPDATED")); }
  void collect(AccessibilityNodeInfo n, ArrayList<String> out){ if(n==null)return; CharSequence t=n.getText(); if(t!=null)out.add(t.toString()); for(int i=0;i<n.getChildCount();i++)collect(n.getChild(i),out); }
  String findMoney(ArrayList<String> xs){ Pattern labeled=Pattern.compile("(?:¥|￥|人民币|余额|可用余额|零钱|总资产)\\s*[:：]?\\s*([0-9][0-9,]*\\.?[0-9]{0,2})"); Pattern bare=Pattern.compile("^\\s*[¥￥]?\\s*([0-9][0-9,]*\\.[0-9]{1,2})\\s*$"); for(String s:xs){Matcher m=labeled.matcher(s);if(m.find())return m.group(1).replace(",","");} for(String s:xs){Matcher m=bare.matcher(s);if(m.matches())return m.group(1).replace(",","");} return null; }
  static String resultJson(Context c){ android.content.SharedPreferences p=c.getSharedPreferences("wallet",0); return "{\"wechat\":\""+p.getString(WX,"")+"\",\"alipay\":\""+p.getString(ALI,"")+"\",\"updatedAt\":"+Math.max(p.getLong(WX+"_at",0),p.getLong(ALI+"_at",0))+"}"; }
}
