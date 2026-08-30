package com.longlive.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NanjingBankSms {
  static final String SENDER = "106980095302";
  static final String BANK_NAME = "南京";
  static final String ACTION_UPDATED = "com.longlive.wallet.BANK_BALANCE_UPDATED";

  private static final String PREFS = "nanjing_bank_balance";
  private static final String KEY_LAST4 = "last4";
  private static final String KEY_CENTS = "balance_cents";
  private static final String KEY_UPDATED_AT = "updated_at";
  private static final String KEY_RAW = "raw_sms";
  private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
  private static final Pattern MESSAGE = Pattern.compile(
      "^【南京银行】您尾号(\\d{4})的账号于(\\d{4})年(\\d{2})月(\\d{2})日" +
      "(\\d{2})时(\\d{2})分(?:支出|收入)([0-9,]+\\.\\d{2})元，" +
      "余额([0-9,]+\\.\\d{2})元，摘要：.+$");

  final String last4;
  final int balanceCents;
  final int updatedAt;
  final String rawMessage;

  private NanjingBankSms(String last4, int balanceCents, int updatedAt,
                         String rawMessage) {
    this.last4 = last4;
    this.balanceCents = balanceCents;
    this.updatedAt = updatedAt;
    this.rawMessage = rawMessage;
  }

  static boolean isExpectedSender(String address) {
    if (address == null) return false;
    String digits = address.replaceAll("[^0-9]", "");
    if (digits.startsWith("86") && digits.length() == SENDER.length() + 2) {
      digits = digits.substring(2);
    }
    return SENDER.equals(digits);
  }

  static NanjingBankSms parse(String body) {
    if (body == null) return null;
    Matcher match = MESSAGE.matcher(body.trim());
    if (!match.matches()) return null;
    try {
      BigDecimal balance = new BigDecimal(match.group(8).replace(",", ""));
      int cents = balance.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
          .intValueExact();
      LocalDateTime transactionTime = LocalDateTime.of(
          Integer.parseInt(match.group(2)), Integer.parseInt(match.group(3)),
          Integer.parseInt(match.group(4)), Integer.parseInt(match.group(5)),
          Integer.parseInt(match.group(6)));
      long epoch = transactionTime.atZone(CHINA_ZONE).toEpochSecond();
      if (epoch <= 0 || epoch > Integer.MAX_VALUE) return null;
      return new NanjingBankSms(match.group(1), cents, (int) epoch, body.trim());
    } catch (ArithmeticException | NumberFormatException | DateTimeException e) {
      return null;
    }
  }

  void save(Context context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString(KEY_LAST4, last4)
        .putInt(KEY_CENTS, balanceCents)
        .putInt(KEY_UPDATED_AT, updatedAt)
        .putString(KEY_RAW, rawMessage)
        .apply();
  }

  static NanjingBankSms load(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    String last4 = prefs.getString(KEY_LAST4, "");
    int updatedAt = prefs.getInt(KEY_UPDATED_AT, 0);
    if (!last4.matches("\\d{4}") || updatedAt <= 0 || !prefs.contains(KEY_CENTS)) return null;
    return new NanjingBankSms(last4, prefs.getInt(KEY_CENTS, 0), updatedAt,
        prefs.getString(KEY_RAW, ""));
  }

  String displayText() {
    return "南京银行 尾号" + last4 + "\n余额 ¥" +
        new BigDecimal(balanceCents).movePointLeft(2).setScale(2).toPlainString();
  }
}
