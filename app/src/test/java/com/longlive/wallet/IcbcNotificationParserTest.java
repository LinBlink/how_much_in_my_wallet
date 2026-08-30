package com.longlive.wallet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class IcbcNotificationParserTest {
  @Test public void parsesRealExpenseNotificationAndAbsoluteBalance() {
    IcbcNotificationParser.Result result = IcbcNotificationParser.parse(
        "尾号6280卡8月30日13:18支出(充值财付通-微信零钱充值账户)0.01元，余额0.58元。");
    assertNotNull(result);
    assertEquals("6280", result.accountLast4);
    assertEquals(58, result.balanceCents);
  }

  @Test public void parsesCommaSeparatedBalance() {
    IcbcNotificationParser.Result result = IcbcNotificationParser.parse(
        "尾号6280卡8月30日13:18收入1.00元，余额12,345.67元。");
    assertNotNull(result);
    assertEquals(1234567, result.balanceCents);
  }

  @Test public void rejectsMessagesWithoutCardAndBalance() {
    assertNull(IcbcNotificationParser.parse("中国工商银行手机银行"));
    assertNull(IcbcNotificationParser.parse("尾号6280卡支出0.01元。"));
  }
}
