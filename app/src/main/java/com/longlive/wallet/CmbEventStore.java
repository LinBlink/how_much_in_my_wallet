package com.longlive.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class CmbEventStore {
  static final String ACTION_UPDATED = "com.longlive.wallet.CMB_EVENT_UPDATED";
  private static final String PREFS = "cmb_events";
  private static final String KEY_IDS = "ids";
  private static final String KEY_LAST_TEXT = "last_text";
  private static final int MAX_EVENTS = 16;

  static final class Event {
    final int id;
    final int deltaCents;
    final int occurredAt;

    Event(int id, int deltaCents, int occurredAt) {
      this.id = id;
      this.deltaCents = deltaCents;
      this.occurredAt = occurredAt;
    }
  }

  private CmbEventStore() {}

  static boolean record(Context context, Event event, String displayText) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    List<Integer> ids = ids(prefs);
    if (ids.contains(event.id)) return false;
    ids.add(event.id);
    while (ids.size() > MAX_EVENTS) {
      int removed = ids.remove(0);
      prefs.edit().remove(eventKey(removed)).apply();
    }
    prefs.edit()
        .putString(KEY_IDS, join(ids))
        .putString(eventKey(event.id), event.deltaCents + "," + event.occurredAt)
        .putString(KEY_LAST_TEXT, displayText)
        .apply();
    return true;
  }

  static void resendRecent(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    for (int id : ids(prefs)) {
      String[] parts = prefs.getString(eventKey(id), "").split(",");
      if (parts.length != 2) continue;
      try {
        PebbleBalanceSender.sendCmbDelta(context,
            new Event(id, Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
      } catch (NumberFormatException ignored) {}
    }
  }

  static String lastText(Context context) {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_LAST_TEXT, "暂无招商银行变动通知");
  }

  private static List<Integer> ids(SharedPreferences prefs) {
    List<Integer> out = new ArrayList<>();
    String raw = prefs.getString(KEY_IDS, "");
    if (raw.isEmpty()) return out;
    for (String part : raw.split(",")) {
      try { out.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
    }
    return out;
  }

  private static String join(List<Integer> ids) {
    StringBuilder out = new StringBuilder();
    for (int id : ids) {
      if (out.length() > 0) out.append(',');
      out.append(id);
    }
    return out.toString();
  }

  private static String eventKey(int id) {
    return "event_" + id;
  }
}
