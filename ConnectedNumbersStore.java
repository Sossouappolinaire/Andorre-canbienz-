package com.cabine.momo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Numéros connectés par opérateur (Moov / MTN / Celtis), entièrement natif
 * (SharedPreferences) : aucune page web, aucun JavaScript.
 */
final class ConnectedNumbersStore {

    private static final String PREFS = "cabine_momo_numbers";

    private final SharedPreferences prefs;

    ConnectedNumbersStore(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    String getNumber(String operateur) {
        return prefs.getString("num_" + operateur, "");
    }

    void setNumber(String operateur, String number) {
        prefs.edit().putString("num_" + operateur, number).apply();
    }

    boolean isConnected(String operateur) {
        return prefs.getBoolean("connected_" + operateur, false);
    }

    void setConnected(String operateur, boolean connected) {
        prefs.edit().putBoolean("connected_" + operateur, connected).apply();
    }

    void disconnect(String operateur) {
        prefs.edit().putBoolean("connected_" + operateur, false).apply();
    }

    boolean anyConnected() {
        for (String op : BeninPhone.OPERATEURS) {
            if (isConnected(op)) return true;
        }
        return false;
    }

    /* --- code de vérification en attente pour un opérateur --- */

    void setPendingCode(String operateur, String code) {
        prefs.edit().putString("pending_" + operateur, code).apply();
    }

    String getPendingCode(String operateur) {
        return prefs.getString("pending_" + operateur, null);
    }

    void clearPendingCode(String operateur) {
        prefs.edit().remove("pending_" + operateur).apply();
    }
}
