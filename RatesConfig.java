package com.cabine.momo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Taux de commission (correspond à l'écran « Réglages de la cabine » de
 * l'ancienne page web) : Dépôt (F pour 1000 F), Retrait (% des frais),
 * Crédit/forfait (F pour 100 F). Entièrement natif, aucun JavaScript.
 */
final class RatesConfig {

    private static final String PREFS = "cabine_momo_rates";
    private static final double DEFAUT_DEP = 3;
    private static final double DEFAUT_RET = 47;
    private static final double DEFAUT_CRED = 4;

    private final SharedPreferences prefs;

    RatesConfig(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    double depot() {
        return prefs.contains("q_dep") ? prefs.getFloat("q_dep", 0) : DEFAUT_DEP;
    }

    double retrait() {
        return prefs.contains("q_ret") ? prefs.getFloat("q_ret", 0) : DEFAUT_RET;
    }

    double credit() {
        return prefs.contains("q_cred") ? prefs.getFloat("q_cred", 0) : DEFAUT_CRED;
    }

    void set(double depot, double retrait, double credit) {
        prefs.edit()
                .putFloat("q_dep", (float) depot)
                .putFloat("q_ret", (float) retrait)
                .putFloat("q_cred", (float) credit)
                .apply();
    }

    /** Taux applicable pour un type d'opération (fnet/fapp/forf suivent le taux crédit). */
    private double pourType(String type) {
        if ("fnet".equals(type) || "fapp".equals(type) || "forf".equals(type) || "cred".equals(type)) return credit();
        if ("dep".equals(type)) return depot();
        if ("ret".equals(type)) return retrait();
        return 0;
    }

    /** Bénéfice de la cabine pour une opération (même formule que l'ancienne page web). */
    double benefice(String type, double montant, double frais) {
        double r;
        if ("dep".equals(type)) r = montant * pourType(type) / 1000;
        else if ("ret".equals(type)) r = frais * pourType(type) / 100;
        else r = montant * pourType(type) / 100;
        return Math.round(r * 100.0) / 100.0;
    }
}
