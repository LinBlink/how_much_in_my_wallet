package com.longlive.wallet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CmbNotificationParserTest {
  @Test public void parsesRealIncomeNotification() {
    CmbNotificationParser.Result result = CmbNotificationParser.parse(
        "您账户9653于08月30日13:07收款人民币0.01");
    assertNotNull(result);
    assertEquals("9653", result.accountLast4);
    assertEquals(1, result.deltaCents);
  }

  @Test public void parsesRealExpenseNotification() {
    CmbNotificationParser.Result result = CmbNotificationParser.parse(
        "您账户9653于08月30日13:05在【财付通-微信支付-微信零钱充值账户】" +
        "发生快捷支付扣款，人民币1,234.56");
    assertNotNull(result);
    assertEquals("9653", result.accountLast4);
    assertEquals(-123456, result.deltaCents);
  }

  @Test public void rejectsAmbiguousOrUnrelatedNotifications() {
    assertNull(CmbNotificationParser.parse("招商银行为您提供金融服务"));
    assertNull(CmbNotificationParser.parse("您账户9653人民币10.00"));
  }

  @Test public void eventHashIsStableAndNonzero() {
    int first = CmbNotificationListenerService.stableHash("WG-task-1");
    assertEquals(first, CmbNotificationListenerService.stableHash("WG-task-1"));
    assertNotEquals(first, CmbNotificationListenerService.stableHash("WG-task-2"));
    assertNotEquals(0, first);
  }
}
