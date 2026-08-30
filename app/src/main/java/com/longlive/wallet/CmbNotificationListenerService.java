package com.longlive.wallet;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public final class CmbNotificationListenerService extends NotificationListenerService {
  private static final String CMB_PACKAGE = "cmb.pb";
  private static final String ICBC_PACKAGE = "com.icbc";
  private static final String FLYME_PUSH_PACKAGE = "com.meizu.cloud";

  @Override public void onListenerConnected() {
    super.onListenerConnected();
    StatusBarNotification[] active = getActiveNotifications();
    if (active != null) {
      for (StatusBarNotification notification : active) onNotificationPosted(notification);
    }
    IcbcBalanceStore.resend(this);
  }

  @Override public void onNotificationPosted(StatusBarNotification sbn) {
    Notification notification = sbn.getNotification();
    Bundle extras = notification.extras;
    if (extras == null) return;

    CharSequence big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
    CharSequence normal = extras.getCharSequence(Notification.EXTRA_TEXT);
    String text = String.valueOf(!TextUtils.isEmpty(big) ? big : normal);
    if (isIcbcNotification(sbn.getPackageName(), extras)) {
      handleIcbc(text, (int) Math.min(sbn.getPostTime() / 1000, Integer.MAX_VALUE));
      return;
    }
    if (!isCmbNotification(sbn.getPackageName(), extras)) return;

    CmbNotificationParser.Result parsed = CmbNotificationParser.parse(text);
    if (parsed == null) return;

    String taskId = extras.getString("extra_app_push_task_Id", "");
    String identity = taskId.isEmpty()
        ? sbn.getKey() + "|" + sbn.getPostTime() + "|" + text : taskId;
    int eventId = stableHash(identity);
    int occurredAt = notificationTime(extras, sbn.getPostTime());
    CmbEventStore.Event event = new CmbEventStore.Event(
        eventId, parsed.deltaCents, occurredAt);
    String sign = parsed.deltaCents > 0 ? "+" : "-";
    String display = "招商银行 " + parsed.accountLast4 + " " + sign +
        String.format(java.util.Locale.US, "%.2f", Math.abs(parsed.deltaCents) / 100.0);
    if (!CmbEventStore.record(this, event, display)) return;

    PebbleBalanceSender.sendCmbDelta(this, event);
    sendBroadcast(new Intent(CmbEventStore.ACTION_UPDATED).setPackage(getPackageName()));
  }

  private void handleIcbc(String text, int occurredAt) {
    IcbcNotificationParser.Result parsed = IcbcNotificationParser.parse(text);
    if (parsed == null || occurredAt <= 0) return;
    IcbcBalanceStore.Balance balance = new IcbcBalanceStore.Balance(
        parsed.accountLast4, parsed.balanceCents, occurredAt, text);
    if (!IcbcBalanceStore.saveIfNewer(this, balance)) return;
    PebbleBalanceSender.sendIcbcBalance(this, balance);
    sendBroadcast(new Intent(IcbcBalanceStore.ACTION_UPDATED).setPackage(getPackageName()));
  }

  private static boolean isIcbcNotification(String packageName, Bundle extras) {
    if (!ICBC_PACKAGE.equals(packageName)) return false;
    String title = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE, ""));
    return title.contains("动账通知");
  }

  private static boolean isCmbNotification(String packageName, Bundle extras) {
    if (CMB_PACKAGE.equals(packageName)) return true;
    if (!FLYME_PUSH_PACKAGE.equals(packageName)) return false;
    if (!CMB_PACKAGE.equals(extras.getString("android.originalPackageName", ""))) return false;
    String title = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE, ""));
    String substitute = String.valueOf(extras.getCharSequence("android.substName", ""));
    return title.contains("招商银行") || substitute.contains("招商银行");
  }

  private static int notificationTime(Bundle extras, long fallbackMillis) {
    try {
      long pushed = Long.parseLong(extras.getString("extra_app_push_task_timestamp", "0"));
      if (pushed > 0 && pushed <= Integer.MAX_VALUE) return (int) pushed;
    } catch (NumberFormatException ignored) {}
    long seconds = fallbackMillis / 1000;
    return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
  }

  static int stableHash(String value) {
    int hash = 0x811c9dc5;
    for (int i = 0; i < value.length(); i++) {
      hash ^= value.charAt(i);
      hash *= 0x01000193;
    }
    return hash == 0 ? 1 : hash;
  }
}
