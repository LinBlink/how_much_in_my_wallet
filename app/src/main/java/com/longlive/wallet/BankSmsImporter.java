package com.longlive.wallet;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

final class BankSmsImporter {
  private static final Uri INBOX = Uri.parse("content://sms/inbox");

  private BankSmsImporter() {}

  static NanjingBankSms importLatest(Context context) {
    String selection = "address=? OR address=?";
    String[] args = {NanjingBankSms.SENDER, "+86" + NanjingBankSms.SENDER};
    try (Cursor cursor = context.getContentResolver().query(
        INBOX, new String[]{"address", "body"}, selection, args, "date DESC")) {
      if (cursor == null) return null;
      int addressColumn = cursor.getColumnIndexOrThrow("address");
      int bodyColumn = cursor.getColumnIndexOrThrow("body");
      while (cursor.moveToNext()) {
        if (!NanjingBankSms.isExpectedSender(cursor.getString(addressColumn))) continue;
        NanjingBankSms parsed = NanjingBankSms.parse(cursor.getString(bodyColumn));
        if (parsed == null) continue;
        parsed.save(context);
        PebbleBalanceSender.send(context, parsed);
        return parsed;
      }
    } catch (SecurityException e) {
      return null;
    }
    return null;
  }
}
