package com.longlive.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;

final class IcbcBalanceStore {
  static final String ACTION_UPDATED = "com.longlive.wallet.ICBC_BALANCE_UPDATED";
  private static final String PREFS = "icbc_balance";
  private static final String KEY_LAST4 = "last4";
  private static final String KEY_CENTS = "balance_cents";
  private static final String KEY_UPDATED_AT = "updated_at";
  private static final String KEY_RAW = "raw_notification";

  static final class Balance {
    final String last4;
    final int balanceCents;
    final int updatedAt;
    final String rawNotification;

    Balance(String last4, int balanceCents, int updatedAt, String rawNotification) {
      this.last4 = last4;
      this.balanceCents = balanceCents;
      this.updatedAt = updatedAt;
      this.rawNotification = rawNotification;
    }
  }

  private IcbcBalanceStore() {}

  static boolean saveIfNewer(Context context, Balance balance) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    if (balance.updatedAt < prefs.getInt(KEY_UPDATED_AT, 0)) return false;
    prefs.edit()
        .putString(KEY_LAST4, balance.last4)
        .putInt(KEY_CENTS, balance.balanceCents)
        .putInt(KEY_UPDATED_AT, balance.updatedAt)
        .putString(KEY_RAW, balance.rawNotification)
        .apply();
    return true;
  }

  static Balance load(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    String last4 = prefs.getString(KEY_LAST4, "");
    int updatedAt = prefs.getInt(KEY_UPDATED_AT, 0);
    if (!last4.matches("\\d{4}") || updatedAt <= 0 || !prefs.contains(KEY_CENTS)) return null;
    return new Balance(last4, prefs.getInt(KEY_CENTS, 0), updatedAt,
        prefs.getString(KEY_RAW, ""));
  }

  static void resend(Context context) {
    Balance balance = load(context);
    if (balance != null) PebbleBalanceSender.sendIcbcBalance(context, balance);
  }

  static String displayText(Context context) {
    Balance balance = load(context);
    if (balance == null) return "工商银行：暂无有效动账通知";
    return "工商银行 尾号" + balance.last4 + "\n余额 ¥" +
        new BigDecimal(balance.balanceCents).movePointLeft(2).setScale(2).toPlainString();
  }
}
