package com.longlive.wallet;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public final class CmbNotificationListenerService extends NotificationListenerService {
  private static final String CMB_PACKAGE = "cmb.pb";
  private static final String FLYME_PUSH_PACKAGE = "com.meizu.cloud";

  @Override public void onListenerConnected() {
    super.onListenerConnected();
    StatusBarNotification[] active = getActiveNotifications();
    if (active == null) return;
    for (StatusBarNotification notification : active) onNotificationPosted(notification);
  }

  @Override public void onNotificationPosted(StatusBarNotification sbn) {
    Notification notification = sbn.getNotification();
    Bundle extras = notification.extras;
    if (extras == null || !isCmbNotification(sbn.getPackageName(), extras)) return;

    CharSequence big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
    CharSequence normal = extras.getCharSequence(Notification.EXTRA_TEXT);
    String text = String.valueOf(!TextUtils.isEmpty(big) ? big : normal);
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
