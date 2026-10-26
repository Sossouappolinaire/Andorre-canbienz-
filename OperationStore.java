package com.cabine.momo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Journal des opérations (remplace le tableau E[] / localStorage de l'ancienne
 * page web) : base SQLite locale, native, aucune page web.
 */
final class OperationStore extends SQLiteOpenHelper {

    private static final String DB = "cabine_momo_ops.db";
    private static final int VERSION = 1;

    OperationStore(Context ctx) {
        super(ctx.getApplicationContext(), DB, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE operations (" +
                "id TEXT PRIMARY KEY, date TEXT, at TEXT, sim TEXT, type TEXT, " +
                "amount REAL, frais REAL, profit REAL, source TEXT, note TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS operations");
        onCreate(db);
    }

    /** Ajoute une opération ; ignorée si son id existe déjà (évite les doublons SMS). */
    boolean add(Operation o) {
        ContentValues v = new ContentValues();
        v.put("id", o.id);
        v.put("date", o.date);
        v.put("at", o.at);
        v.put("sim", o.sim);
        v.put("type", o.type);
        v.put("amount", o.amount);
        v.put("frais", o.frais);
        v.put("profit", o.profit);
        v.put("source", o.source);
        v.put("note", o.note);
        long id = getWritableDatabase().insertWithOnConflict("operations", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        return id != -1;
    }

    void delete(String id) {
        getWritableDatabase().delete("operations", "id=?", new String[]{id});
    }

    List<Operation> byDate(String date) {
        return query("date=?", new String[]{date});
    }

    List<Operation> byMonth(String yyyyMM) {
        return query("substr(date,1,7)=?", new String[]{yyyyMM});
    }

    private List<Operation> query(String where, String[] args) {
        List<Operation> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query("operations", null, where, args, null, null, "date DESC, at DESC");
        try {
            while (c.moveToNext()) {
                Operation o = new Operation();
                o.id = c.getString(c.getColumnIndexOrThrow("id"));
                o.date = c.getString(c.getColumnIndexOrThrow("date"));
                o.at = c.getString(c.getColumnIndexOrThrow("at"));
                o.sim = c.getString(c.getColumnIndexOrThrow("sim"));
                o.type = c.getString(c.getColumnIndexOrThrow("type"));
                o.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
                o.frais = c.getDouble(c.getColumnIndexOrThrow("frais"));
                o.profit = c.getDouble(c.getColumnIndexOrThrow("profit"));
                o.source = c.getString(c.getColumnIndexOrThrow("source"));
                o.note = c.getString(c.getColumnIndexOrThrow("note"));
                out.add(o);
            }
        } finally {
            c.close();
        }
        return out;
    }

    /** Totaux (nombre + montant) par type, plus le bénéfice total — même calcul que l'ancienne page web. */
    static final class Totaux {
        double depotMontant, retraitMontant, creditMontant, forfaitMontant, benefice;
        int depotN, retraitN, creditN, forfaitN;
    }

    static Totaux agreger(List<Operation> ops) {
        Totaux t = new Totaux();
        for (Operation o : ops) {
            String type = "forf".equals(o.type) ? "fnet" : o.type;
            if ("dep".equals(type)) { t.depotMontant += o.amount; t.depotN++; }
            else if ("ret".equals(type)) { t.retraitMontant += o.amount; t.retraitN++; }
            else if ("cred".equals(type)) { t.creditMontant += o.amount; t.creditN++; }
            else if ("fnet".equals(type) || "fapp".equals(type)) { t.forfaitMontant += o.amount; t.forfaitN++; }
            t.benefice += o.profit;
        }
        return t;
    }
}
