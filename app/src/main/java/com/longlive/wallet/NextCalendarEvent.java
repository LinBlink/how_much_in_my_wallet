package com.longlive.wallet;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

final class NextCalendarEvent {
  static final class Event {
    final int startAt;
    final String title;

    Event(int startAt, String title) {
      this.startAt = startAt;
      this.title = title;
    }
  }

  private NextCalendarEvent() {}

  static Event load(Context context) {
    if (android.os.Build.VERSION.SDK_INT >= 23 &&
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED) return null;

    long now = System.currentTimeMillis();
    long horizon = now + 7L * 24 * 60 * 60 * 1000;
    Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
    android.content.ContentUris.appendId(builder, now);
    android.content.ContentUris.appendId(builder, horizon);
    String[] projection = {
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN
    };
    try (Cursor cursor = context.getContentResolver().query(builder.build(), projection,
        CalendarContract.Instances.BEGIN + " >= ? AND " +
            CalendarContract.Instances.ALL_DAY + " = 0",
        new String[]{String.valueOf(now)}, CalendarContract.Instances.BEGIN + " ASC")) {
      if (cursor == null || !cursor.moveToFirst()) return null;
      long begin = cursor.getLong(1);
      if (begin <= 0 || begin / 1000 > Integer.MAX_VALUE) return null;
      String title = cursor.getString(0);
      return new Event((int) (begin / 1000), shortTitle(title));
    } catch (SecurityException ignored) {
      return null;
    }
  }

  static String displayText(Context context) {
    Event event = load(context);
    if (event == null) return "日历：未来 7 天没有日程";
    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MM-dd HH:mm",
        java.util.Locale.getDefault());
    return "下一条日程\n" + format.format(new java.util.Date(event.startAt * 1000L)) +
        " " + event.title;
  }

  private static String shortTitle(String title) {
    String clean = title == null ? "未命名" : title.trim().replaceAll("\\s+", " ");
    if (clean.isEmpty()) clean = "未命名";
    return clean.length() > 12 ? clean.substring(0, 12) : clean;
  }
}
