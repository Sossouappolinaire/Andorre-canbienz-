package com.cabine.momo;

/**
 * Validation des numéros béninois (plan de numérotation 2024 : 10 chiffres,
 * préfixe 01). Aucune dépendance à une page web : utilisé directement par
 * les écrans Java natifs.
 */
final class BeninPhone {

    private BeninPhone() {
    }

    /** Opérateurs proposés dans la barre de défilement des Paramètres. */
    static final String[] OPERATEURS = {"Moov", "MTN", "Celtis"};

    /**
     * Nettoie la saisie (espaces, points, tirets, indicatif +229/229) et ne
     * garde que les chiffres.
     */
    static String digitsOnly(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("+229")) s = s.substring(4);
        else if (s.startsWith("00229")) s = s.substring(5);
        else if (s.startsWith("229") && s.length() > 10) s = s.substring(3);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
        }
        return b.toString();
    }

    /**
     * Un numéro béninois valide compte exactement 10 chiffres et commence
     * par 01 (plan de numérotation en vigueur depuis 2024).
     */
    static boolean isValid(String raw) {
        String d = digitsOnly(raw);
        return d.length() == 10 && d.startsWith("01");
    }

    /** Numéro au format d'envoi international (+229XXXXXXXXXX). */
    static String toInternational(String raw) {
        String d = digitsOnly(raw);
        return "+229" + d;
    }

    /** Numéro formaté pour l'affichage : 01 90 00 00 00. */
    static String format(String raw) {
        String d = digitsOnly(raw);
        if (d.length() != 10) return raw == null ? "" : raw;
        return d.substring(0, 2) + " " + d.substring(2, 4) + " " + d.substring(4, 6)
                + " " + d.substring(6, 8) + " " + d.substring(8, 10);
    }
}
