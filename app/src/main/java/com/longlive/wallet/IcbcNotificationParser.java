package com.longlive.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IcbcNotificationParser {
  private static final Pattern MESSAGE = Pattern.compile(
      "尾号(\\d{4})卡.*?余额([0-9,]+\\.\\d{2})元[。.]?$", Pattern.DOTALL);

  static final class Result {
    final String accountLast4;
    final int balanceCents;

    Result(String accountLast4, int balanceCents) {
      this.accountLast4 = accountLast4;
      this.balanceCents = balanceCents;
    }
  }

  private IcbcNotificationParser() {}

  static Result parse(String text) {
    if (text == null) return null;
    Matcher match = MESSAGE.matcher(text.trim());
    if (!match.find()) return null;
    try {
      int cents = new BigDecimal(match.group(2).replace(",", ""))
          .movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
      if (cents < 0) return null;
      return new Result(match.group(1), cents);
    } catch (ArithmeticException | NumberFormatException e) {
      return null;
    }
  }
}
