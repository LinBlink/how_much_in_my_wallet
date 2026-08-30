package com.longlive.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CmbNotificationParser {
  // CMB uses both “您账户9653” and “您尾号9653” in real notifications.
  private static final Pattern ACCOUNT =
      Pattern.compile("(?:您(?:账户|尾号)|尾号)(\\d{4})");
  private static final Pattern AMOUNT = Pattern.compile("人民币([0-9,]+\\.\\d{2})");
  private static final String[] INCOME = {"收款", "入账", "存入", "退款"};
  private static final String[] EXPENSE = {"扣款", "消费", "支出", "转出", "取款"};

  static final class Result {
    final String accountLast4;
    final int deltaCents;

    Result(String accountLast4, int deltaCents) {
      this.accountLast4 = accountLast4;
      this.deltaCents = deltaCents;
    }
  }

  private CmbNotificationParser() {}

  static Result parse(String text) {
    if (text == null) return null;
    String normalized = text.trim();
    Matcher account = ACCOUNT.matcher(normalized);
    Matcher amount = AMOUNT.matcher(normalized);
    if (!account.find() || !amount.find()) return null;

    boolean income = containsAny(normalized, INCOME);
    boolean expense = containsAny(normalized, EXPENSE);
    if (income == expense) return null;
    try {
      int cents = new BigDecimal(amount.group(1).replace(",", ""))
          .movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
      if (cents <= 0) return null;
      return new Result(account.group(1), income ? cents : -cents);
    } catch (ArithmeticException | NumberFormatException e) {
      return null;
    }
  }

  private static boolean containsAny(String text, String[] words) {
    for (String word : words) if (text.contains(word)) return true;
    return false;
  }
}
