package com.longlive.wallet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NanjingBankSmsTest {
  private static final String SAMPLE = "【南京银行】您尾号9434的账号于2026年08月08日13时37分" +
      "支出274.00元，余额1152.29元，摘要：财付通快捷支付-微信转账";

  @Test public void parsesSampleExactly() {
    NanjingBankSms sms = NanjingBankSms.parse(SAMPLE);
    assertNotNull(sms);
    assertEquals("9434", sms.last4);
    assertEquals(115229, sms.balanceCents);
    assertEquals(1786167420, sms.updatedAt);
  }

  @Test public void rejectsWrongBankOrLooseAmount() {
    assertNull(NanjingBankSms.parse(SAMPLE.replace("南京银行", "其他银行")));
    assertNull(NanjingBankSms.parse(SAMPLE.replace("1152.29", "1152")));
  }

  @Test public void acceptsOnlyConfiguredSender() {
    assertTrue(NanjingBankSms.isExpectedSender("106980095302"));
    assertTrue(NanjingBankSms.isExpectedSender("+86 106980095302"));
    assertFalse(NanjingBankSms.isExpectedSender("106980095301"));
  }
}
