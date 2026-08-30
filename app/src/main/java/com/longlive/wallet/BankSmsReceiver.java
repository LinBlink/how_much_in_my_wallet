package com.longlive.wallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public final class BankSmsReceiver extends BroadcastReceiver {
  private static final String TAG = "BankSmsReceiver";

  @Override public void onReceive(Context context, Intent intent) {
    if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
    SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
    if (messages == null || messages.length == 0) return;

    String sender = messages[0].getOriginatingAddress();
    if (!NanjingBankSms.isExpectedSender(sender)) {
      Log.d(TAG, "Ignored SMS sender: " + sender);
      return;
    }

    StringBuilder body = new StringBuilder();
    for (SmsMessage message : messages) {
      if (!NanjingBankSms.isExpectedSender(message.getOriginatingAddress())) return;
      body.append(message.getMessageBody());
    }
    NanjingBankSms balance = NanjingBankSms.parse(body.toString());
    if (balance == null) {
      Log.w(TAG, "Ignored unexpected Nanjing Bank SMS format");
      return;
    }

    balance.save(context);
    PebbleBalanceSender.send(context, balance);
    context.sendBroadcast(new Intent(NanjingBankSms.ACTION_UPDATED)
        .setPackage(context.getPackageName()));
  }
}
