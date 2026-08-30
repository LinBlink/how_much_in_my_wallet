package com.longlive.wallet;

import android.content.Context;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

final class PebbleBalanceSender {
  private static final UUID WATCHFACE_UUID =
      UUID.fromString("8083f2df-8a08-4dbf-b8fe-922936e6ab1e");
  private static final int BANK_NAME = 10023;
  private static final int BANK_CARD_LAST4 = 10024;
  private static final int BANK_BALANCE_CENTS = 10025;
  private static final int BANK_UPDATED_AT = 10026;
  private static final int CMB_BALANCE_DELTA_CENTS = 10028;
  private static final int CMB_EVENT_ID = 10029;
  private static final int CMB_EVENT_AT = 10030;
  private static final int ICBC_BALANCE_CENTS = 10031;
  private static final int ICBC_UPDATED_AT = 10032;

  private PebbleBalanceSender() {}

  static void send(Context context, NanjingBankSms balance) {
    PebbleDictionary data = new PebbleDictionary();
    data.addString(BANK_NAME, NanjingBankSms.BANK_NAME);
    data.addString(BANK_CARD_LAST4, balance.last4);
    data.addInt32(BANK_BALANCE_CENTS, balance.balanceCents);
    data.addInt32(BANK_UPDATED_AT, balance.updatedAt);
    PebbleKit.sendDataToPebble(context.getApplicationContext(), WATCHFACE_UUID, data);
  }

  static void sendCmbDelta(Context context, CmbEventStore.Event event) {
    PebbleDictionary data = new PebbleDictionary();
    data.addInt32(CMB_BALANCE_DELTA_CENTS, event.deltaCents);
    data.addInt32(CMB_EVENT_ID, event.id);
    data.addInt32(CMB_EVENT_AT, event.occurredAt);
    PebbleKit.sendDataToPebble(context.getApplicationContext(), WATCHFACE_UUID, data);
  }

  static void sendIcbcBalance(Context context, IcbcBalanceStore.Balance balance) {
    PebbleDictionary data = new PebbleDictionary();
    data.addInt32(ICBC_BALANCE_CENTS, balance.balanceCents);
    data.addInt32(ICBC_UPDATED_AT, balance.updatedAt);
    PebbleKit.sendDataToPebble(context.getApplicationContext(), WATCHFACE_UUID, data);
  }
}
