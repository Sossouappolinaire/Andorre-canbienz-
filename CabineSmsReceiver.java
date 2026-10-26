package com.cabine.momo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Reçoit les SMS. Seuls les messages MoMo / forfait / crédit (et le code de connexion
 * de l'application) sont gardés : les SMS personnels sont ignorés.
 */
public class CabineSmsReceiver extends BroadcastReceiver {

    private static final Pattern KEEP = Pattern.compile(
            "(?i)(solde|transfert|d[ée]p[ôo]t|retrait|forfait|cr[ée]dit|recharge|cabine momo)");

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs == null || msgs.length == 0) return;

        StringBuilder body = new StringBuilder();
        for (SmsMessage m : msgs) body.append(m.getMessageBody());
        String text = body.toString();
        if (!KEEP.matcher(text).find()) return;

        String sender = msgs[0].getOriginatingAddress();
        long ts = msgs[0].getTimestampMillis();
        JSONObject it = CabineSmsStore.add(ctx, sender, text, ts);

        ConnectedNumbersStore numbers = new ConnectedNumbersStore(ctx);

        // Validation automatique du code de connexion d'un numéro (Moov / MTN / Celtis),
        // sans aucune saisie manuelle ni passage par une page web.
        boolean etaitUnCode = false;
        for (String operateur : BeninPhone.OPERATEURS) {
            String pending = numbers.getPendingCode(operateur);
            if (pending != null && text.contains(pending)) {
                numbers.setConnected(operateur, true);
                numbers.clearPendingCode(operateur);
                NumbersActivity.notifyConnected(operateur);
                etaitUnCode = true;
            }
        }

        // Une fois l'autorisation accordée et au moins un numéro connecté, l'application
        // enregistre elle-même l'opération à partir du SMS reçu (plus besoin de saisie).
        if (!etaitUnCode && numbers.anyConnected()) {
            enregistrerAutomatiquement(ctx, sender, text, ts);
        }

        MainActivity.notifySms(it);
    }

    private void enregistrerAutomatiquement(Context ctx, String sender, String text, long ts) {
        SmsOperationParser.Resultat p = SmsOperationParser.analyser(text, sender);
        if (p == null || p.type.isEmpty() || p.amount == null || p.sim.isEmpty()) return;

        RatesConfig taux = new RatesConfig(ctx);
        Operation o = new Operation();
        o.id = "sms-" + (!TextUtils.isEmpty(p.txid) ? p.txid : SmsOperationParser.hash(p.raw));
        SimpleDateFormat jour = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.FRANCE);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date(ts);
        o.date = jour.format(date);
        o.at = iso.format(date);
        o.sim = p.sim;
        o.type = p.type;
        o.amount = p.amount;
        o.frais = "ret".equals(p.type) ? p.frais : 0;
        o.profit = taux.benefice(p.type, p.amount, p.frais);
        o.source = "sms";
        o.note = "SMS auto";

        new OperationStore(ctx).add(o);
    }
}
