package com.cabine.momo;

/** Une opération MoMo (dépôt, retrait, crédit, forfait...). */
final class Operation {
    String id;
    String date;   // AAAA-MM-JJ
    String at;     // horodatage ISO 8601, peut être vide
    String sim;    // MTN / Moov / Celtis
    String type;   // dep / ret / cred / fnet / fapp
    double amount;
    double frais;
    double profit;
    String source; // "manuel" ou "sms"
    String note;

    static String libelleType(String type) {
        if ("dep".equals(type)) return "Dépôt";
        if ("ret".equals(type)) return "Retrait";
        if ("cred".equals(type)) return "Achat de crédit";
        if ("fnet".equals(type)) return "Achat de forfait internet";
        if ("fapp".equals(type)) return "Achat de forfait appel";
        if ("forf".equals(type)) return "Achat de forfait internet";
        return type;
    }
}
