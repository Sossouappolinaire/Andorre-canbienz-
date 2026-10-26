package com.cabine.momo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;

/**
 * Paramètres : barre de défilement Moov / MTN / Celtis + numéro à connecter.
 * Entièrement natif (Java), aucun JavaScript ni fichier HTML.
 *
 * Flux : le propriétaire choisit l'opérateur, saisit le numéro béninois,
 * un code à 6 chiffres est envoyé par SMS au numéro, il le saisit (ou le
 * code est validé tout seul dès réception), puis l'application reçoit et
 * traite automatiquement les messages de ce numéro.
 */
public class NumbersActivity extends Activity {

    private static final int REQ_SMS = 5201;
    private static final int FOND = 0xFF04281B;
    private static final int ACCENT = 0xFF0B7A4B;
    private static final int CARD = 0xFF12181F;
    private static final int MUTED = 0xFFA3ADBB;

    private static NumbersActivity current;

    private ConnectedNumbersStore store;
    private LinearLayout liste;
    private TextView message;

    /** Appelé par CabineSmsReceiver quand un code est validé automatiquement à la réception du SMS. */
    static void notifyConnected(final String operateur) {
        final NumbersActivity a = current;
        if (a == null) return;
        a.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                a.afficherMessage("✅ " + operateur + " connecté — code accepté, vous pouvez maintenant recevoir les messages.");
                a.rafraichir();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = this;
        store = new ConnectedNumbersStore(this);
        getWindow().setStatusBarColor(FOND);
        getWindow().setNavigationBarColor(FOND);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(FOND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(48), pad, pad);
        scroll.addView(root);

        TextView titre = new TextView(this);
        titre.setText("Paramètres");
        titre.setTextColor(Color.WHITE);
        titre.setTextSize(24);
        titre.setTypeface(null, Typeface.BOLD);
        root.addView(titre);

        TextView sous = new TextView(this);
        sous.setText("Numéros à connecter pour lire les messages");
        sous.setTextColor(MUTED);
        sous.setPadding(0, dp(2), 0, dp(20));
        root.addView(sous);

        message = new TextView(this);
        message.setTextColor(0xFF3ECF8E);
        message.setPadding(0, 0, 0, dp(16));
        root.addView(message);

        liste = new LinearLayout(this);
        liste.setOrientation(LinearLayout.VERTICAL);
        root.addView(liste);

        root.addView(sectionTaux());

        setContentView(scroll);
        rafraichir();
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    private void afficherMessage(String m) {
        if (message != null) message.setText(m == null ? "" : m);
    }

    /** Reconstruit la liste : une ligne par opérateur (Moov, MTN, Celtis). */
    private void rafraichir() {
        liste.removeAllViews();
        for (final String operateur : BeninPhone.OPERATEURS) {
            liste.addView(ligneOperateur(operateur));
        }
    }

    private View ligneOperateur(final String operateur) {
        LinearLayout carte = new LinearLayout(this);
        carte.setOrientation(LinearLayout.VERTICAL);
        carte.setBackgroundColor(CARD);
        int p = dp(14);
        carte.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lpCarte = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCarte.bottomMargin = dp(12);
        carte.setLayoutParams(lpCarte);

        LinearLayout entete = new LinearLayout(this);
        entete.setOrientation(LinearLayout.HORIZONTAL);
        entete.setGravity(Gravity.CENTER_VERTICAL);

        TextView nom = new TextView(this);
        nom.setText(operateur);
        nom.setTextColor(Color.WHITE);
        nom.setTypeface(null, Typeface.BOLD);
        nom.setTextSize(16);
        LinearLayout.LayoutParams lpNom = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        entete.addView(nom, lpNom);

        boolean connecte = store.isConnected(operateur);
        String numero = store.getNumber(operateur);

        Button action = new Button(this);
        if (connecte) {
            action.setText("Déconnecter");
            action.setTextColor(Color.WHITE);
            action.setBackgroundColor(0xFFB3321F);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.disconnect(operateur);
                    afficherMessage(operateur + " déconnecté.");
                    rafraichir();
                }
            });
        } else {
            action.setText("Connecter");
            action.setTextColor(Color.WHITE);
            action.setBackgroundColor(ACCENT);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    demarrerConnexion(operateur);
                }
            });
        }
        entete.addView(action);
        carte.addView(entete);

        TextView detail = new TextView(this);
        detail.setTextColor(MUTED);
        detail.setPadding(0, dp(4), 0, 0);
        detail.setText(numero.isEmpty()
                ? "Numéro non renseigné"
                : (connecte ? "✅ Connecté — " : "") + BeninPhone.format(numero));
        carte.addView(detail);

        return carte;
    }

    /** Étape 1 : saisie et validation du numéro béninois, avant l'envoi du code. */
    private void demarrerConnexion(final String operateur) {
        final EditText champ = new EditText(this);
        champ.setHint("Ex. 01 90 00 00 00");
        champ.setInputType(InputType.TYPE_CLASS_PHONE);
        String existant = store.getNumber(operateur);
        if (!existant.isEmpty()) champ.setText(BeninPhone.format(existant));

        new AlertDialog.Builder(this)
                .setTitle("Connecter un numéro " + operateur)
                .setMessage("Entrez un numéro béninois (10 chiffres).")
                .setView(encadrer(champ))
                .setPositiveButton("Continuer", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        String saisie = champ.getText().toString();
                        if (!BeninPhone.isValid(saisie)) {
                            afficherMessage("❌ Numéro béninois invalide : " + saisie
                                    + ". Il doit contenir exactement 10 chiffres.");
                            return;
                        }
                        String digits = BeninPhone.digitsOnly(saisie);
                        store.setNumber(operateur, digits);
                        verifierPermissionPuisEnvoyer(operateur, digits);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    /** Taux de commission de la cabine (Dépôt / Retrait / Crédit), comme dans les réglages d'origine. */
    private View sectionTaux() {
        final RatesConfig taux = new RatesConfig(this);

        LinearLayout carte = new LinearLayout(this);
        carte.setOrientation(LinearLayout.VERTICAL);
        carte.setBackgroundColor(CARD);
        int p = dp(14);
        carte.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lpCarte = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCarte.topMargin = dp(8);
        carte.setLayoutParams(lpCarte);

        TextView h = new TextView(this);
        h.setText("Taux de commission");
        h.setTextColor(Color.WHITE);
        h.setTypeface(null, Typeface.BOLD);
        carte.addView(h);

        final EditText dep = champTaux("Dépôt : F pour 1000 F", taux.depot());
        final EditText ret = champTaux("Retrait : % des frais", taux.retrait());
        final EditText cred = champTaux("Crédit/forfait : F pour 100 F", taux.credit());
        carte.addView(dep);
        carte.addView(ret);
        carte.addView(cred);

        Button enregistrer = new Button(this);
        enregistrer.setText("Enregistrer les taux");
        enregistrer.setBackgroundColor(ACCENT);
        enregistrer.setTextColor(Color.WHITE);
        enregistrer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double d = nombreOuDefaut(dep.getText().toString(), taux.depot());
                double r = nombreOuDefaut(ret.getText().toString(), taux.retrait());
                double c = nombreOuDefaut(cred.getText().toString(), taux.credit());
                taux.set(d, r, c);
                afficherMessage("✅ Taux de commission enregistrés.");
            }
        });
        carte.addView(enregistrer, margeTop());

        return carte;
    }

    private EditText champTaux(String libelle, double valeur) {
        EditText champ = new EditText(this);
        champ.setHint(libelle);
        champ.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champ.setText(String.valueOf(valeur));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        champ.setLayoutParams(lp);
        return champ;
    }

    private LinearLayout.LayoutParams margeTop() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        return lp;
    }

    private double nombreOuDefaut(String s, double defaut) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return defaut;
        }
    }

    private View encadrer(View v) {
        int m = dp(20);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(m, dp(8), m, 0);
        box.addView(v);
        return box;
    }

    private void verifierPermissionPuisEnvoyer(String operateur, String number) {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            enAttenteOperateur = operateur;
            enAttenteNumero = number;
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
            }, REQ_SMS);
            return;
        }
        envoyerCode(operateur, number);
    }

    private String enAttenteOperateur, enAttenteNumero;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_SMS) return;
        boolean accorde = true;
        for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) accorde = false;
        if (!accorde) {
            afficherMessage("❌ Autorisation SMS refusée : impossible de connecter " + enAttenteOperateur + ".");
            return;
        }
        if (enAttenteOperateur != null && enAttenteNumero != null) {
            envoyerCode(enAttenteOperateur, enAttenteNumero);
        }
    }

    /** Étape 2 : envoi du code à 6 chiffres par SMS au numéro saisi. */
    private void envoyerCode(final String operateur, final String number) {
        String code = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        String to = BeninPhone.toInternational(number);
        try {
            SmsManager sm = Build.VERSION.SDK_INT >= 31
                    ? getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            sm.sendTextMessage(to, null, "Cabine Momo : code " + code, null, null);
        } catch (Exception e) {
            afficherMessage("❌ Échec de l'envoi du code : " + (e.getMessage() == null ? "erreur" : e.getMessage()));
            return;
        }
        store.setPendingCode(operateur, code);
        afficherMessage("Code envoyé au " + BeninPhone.format(number) + ". Il se valide automatiquement à sa réception, ou saisissez-le ci-dessous.");
        demanderCode(operateur, number);
    }

    /** Étape 3 : saisie manuelle du code (en plus de la validation automatique à la réception). */
    private void demanderCode(final String operateur, final String number) {
        final EditText champ = new EditText(this);
        champ.setHint("Code reçu par SMS (6 chiffres)");
        champ.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Code de vérification — " + operateur)
                .setView(encadrer(champ))
                .setCancelable(true)
                .setPositiveButton("Valider", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        String saisi = champ.getText().toString().trim();
                        String attendu = store.getPendingCode(operateur);
                        if (attendu != null && attendu.equals(saisi)) {
                            store.setConnected(operateur, true);
                            store.clearPendingCode(operateur);
                            afficherMessage("✅ Code accepté, vous pouvez maintenant recevoir les messages.");
                            Toast.makeText(NumbersActivity.this,
                                    "Code accepté, vous pouvez maintenant recevoir les messages.",
                                    Toast.LENGTH_LONG).show();
                            rafraichir();
                        } else {
                            afficherMessage("❌ Code incorrect.");
                        }
                    }
                })
                .setNegativeButton("Plus tard", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rafraichir();
    }

    @Override
    protected void onDestroy() {
        if (current == this) current = null;
        super.onDestroy();
    }
}
