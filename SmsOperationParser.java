package com.cabine.momo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconnaissance automatique des SMS MoMo (dépôt, retrait, crédit, forfait),
 * portage Java fidèle de la logique « smsParse » de l'ancienne page web.
 * Aucune dépendance à une page web ou à du JavaScript.
 */
final class SmsOperationParser {

    private SmsOperationParser() {
    }

    /** Règles : début du SMS → type d'opération. */
    private static final Pattern R_TRANSFERT = Pattern.compile("^\\s*transfert\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_DEPOT_RECU = Pattern.compile("^\\s*d[ée]p[ôo]t\\s+re[çc]u", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_RETRAIT = Pattern.compile("^\\s*retrait\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern R_FORFAIT = Pattern.compile("forfait|pass\\b|bundle", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_INTERNET = Pattern.compile("internet|data|\\bgo\\b|\\bmo\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_APPEL = Pattern.compile("appel|voix|minute", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_CREDIT = Pattern.compile("cr[ée]dit|recharge|airtime", Pattern.CASE_INSENSITIVE);

    private static final Pattern R_AMOUNT = Pattern.compile("(\\d[\\d\\s.]*)\\s*F(?:CFA)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_FRAIS = Pattern.compile("frais\\s*:?\\s*(\\d[\\d\\s.]*)\\s*F", Pattern.CASE_INSENSITIVE);

    private static final Pattern R_TXID = Pattern.compile("\\bID\\s*:?\\s*(\\d{6,})", Pattern.CASE_INSENSITIVE);

    private static final Pattern R_CELTIS = Pattern.compile("celtis", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_MOOV = Pattern.compile("moov", Pattern.CASE_INSENSITIVE);
    private static final Pattern R_MTN = Pattern.compile("mtn|momo", Pattern.CASE_INSENSITIVE);

    static final class Resultat {
        String type = "";   // dep / ret / cred / fnet / fapp / ""
        Double amount;      // montant principal, null si introuvable
        double frais;
        String sim = "";    // MTN / Moov / Celtis / ""
        String txid = "";   // identifiant de transaction, si présent dans le SMS
        String raw;
    }

    /** Petit identifiant stable dérivé du texte, utilisé quand le SMS ne contient pas d'ID. */
    static String hash(String s) {
        int h = 0;
        if (s != null) for (int i = 0; i < s.length(); i++) h = h * 31 + s.charAt(i);
        return String.valueOf(Math.abs(h));
    }

    private static Double nombre(String s) {
        if (s == null) return null;
        String d = s.replaceAll("[^\\d]", "");
        if (d.isEmpty()) return null;
        try {
            return Double.parseDouble(d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String detecterSim(String sender, String body) {
        String t = (sender == null ? "" : sender) + " " + (body == null ? "" : body);
        if (R_CELTIS.matcher(t).find()) return "Celtis";
        if (R_MOOV.matcher(t).find()) return "Moov";
        if (R_MTN.matcher(t).find()) return "MTN";
        return "";
    }

    static Resultat analyser(String body, String sender) {
        String b = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (b.isEmpty()) return null;

        Resultat r = new Resultat();
        r.raw = b;

        if (R_TRANSFERT.matcher(b).find()) r.type = "dep";
        else if (R_DEPOT_RECU.matcher(b).find()) r.type = "ret";
        else if (R_RETRAIT.matcher(b).find()) r.type = "ret";

        if (r.type.isEmpty()) {
            if (R_FORFAIT.matcher(b).find() && R_INTERNET.matcher(b).find()) r.type = "fnet";
            else if (R_FORFAIT.matcher(b).find() && R_APPEL.matcher(b).find()) r.type = "fapp";
            else if (R_CREDIT.matcher(b).find()) r.type = "cred";
        }

        Matcher am = R_AMOUNT.matcher(b);
        r.amount = am.find() ? nombre(am.group(1)) : null;

        Matcher fm = R_FRAIS.matcher(b);
        Double frais = fm.find() ? nombre(fm.group(1)) : null;
        r.frais = frais == null ? 0 : frais;

        r.sim = detecterSim(sender, b);

        Matcher im = R_TXID.matcher(b);
        r.txid = im.find() ? im.group(1) : "";

        return r;
    }
}
