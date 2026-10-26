package com.cabine.momo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Écran d'accueil natif (Java pur, aucune page HTML) : journal des opérations
 * du jour, ajout manuel, et totaux — portage de l'ancien tableau de bord web.
 */
public class MainActivity extends Activity {

    private static final int FOND = 0xFF04281B;
    private static final int ACCENT = 0xFF0B7A4B;
    private static final int CARD = 0xFF12181F;
    private static final int MUTED = 0xFFA3ADBB;
    private static final String[] TYPES = {"dep", "ret", "cred", "fnet", "fapp"};

    private static MainActivity current;

    private OperationStore store;
    private RatesConfig taux;
    private TextView totauxTxt;
    private LinearLayout liste;
    private Spinner spSim, spType;
    private EditText champMontant, champFrais;
    private LinearLayout ligneFrais;
    private String jourCourant;

    /** Appelé par le récepteur de SMS : rafraîchit l'écran si l'application est ouverte. */
    static void notifySms(JSONObject item) {
        final MainActivity a = current;
        if (a != null) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    a.rafraichir();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = this;
        store = new OperationStore(this);
        taux = new RatesConfig(this);
        jourCourant = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(new Date());

        getWindow().setStatusBarColor(FOND);
        getWindow().setNavigationBarColor(FOND);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(FOND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, dp(48), pad, pad);
        scroll.addView(root);

        LinearLayout entete = new LinearLayout(this);
        entete.setOrientation(LinearLayout.HORIZONTAL);
        entete.setGravity(Gravity.CENTER_VERTICAL);
        TextView titre = new TextView(this);
        titre.setText("Cabine Momo");
        titre.setTextColor(Color.WHITE);
        titre.setTextSize(22);
        titre.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lpTitre = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        entete.addView(titre, lpTitre);
        Button gear = new Button(this);
        gear.setText("⚙");
        gear.setBackgroundColor(CARD);
        gear.setTextColor(Color.WHITE);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NumbersActivity.class));
            }
        });
        entete.addView(gear, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(entete);

        TextView sous = new TextView(this);
        sous.setText("Journal du jour — " + jourCourant);
        sous.setTextColor(MUTED);
        sous.setPadding(0, dp(2), 0, dp(16));
        root.addView(sous);

        totauxTxt = new TextView(this);
        totauxTxt.setTextColor(Color.WHITE);
        totauxTxt.setBackgroundColor(CARD);
        totauxTxt.setPadding(dp(14), dp(14), dp(14), dp(14));
        totauxTxt.setLineSpacing(dp(4), 1f);
        root.addView(totauxTxt, margeBas());

        root.addView(formulaireAjout());

        TextView titreListe = new TextView(this);
        titreListe.setText("Opérations du jour");
        titreListe.setTextColor(Color.WHITE);
        titreListe.setTypeface(null, Typeface.BOLD);
        titreListe.setPadding(0, dp(8), 0, dp(8));
        root.addView(titreListe);

        liste = new LinearLayout(this);
        liste.setOrientation(LinearLayout.VERTICAL);
        root.addView(liste);

        setContentView(scroll);
        rafraichir();
    }

    private LinearLayout.LayoutParams margeBas() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        return lp;
    }

    private View formulaireAjout() {
        LinearLayout carte = new LinearLayout(this);
        carte.setOrientation(LinearLayout.VERTICAL);
        carte.setBackgroundColor(CARD);
        int p = dp(14);
        carte.setPadding(p, p, p, p);

        TextView h = new TextView(this);
        h.setText("Ajouter une opération");
        h.setTextColor(Color.WHITE);
        h.setTypeface(null, Typeface.BOLD);
        carte.addView(h, margeBasPetite());

        spSim = new Spinner(this);
        spSim.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, BeninPhone.OPERATEURS));
        carte.addView(spSim, margeBasPetite());

        String[] libelles = new String[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) libelles[i] = Operation.libelleType(TYPES[i]);
        spType = new Spinner(this);
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, libelles));
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ligneFrais.setVisibility("ret".equals(TYPES[position]) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        carte.addView(spType, margeBasPetite());

        champMontant = new EditText(this);
        champMontant.setHint("Montant (F)");
        champMontant.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        carte.addView(champMontant, margeBasPetite());

        ligneFrais = new LinearLayout(this);
        ligneFrais.setOrientation(LinearLayout.VERTICAL);
        ligneFrais.setVisibility(View.GONE);
        champFrais = new EditText(this);
        champFrais.setHint("Frais de retrait (F)");
        champFrais.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ligneFrais.addView(champFrais, margeBasPetite());
        carte.addView(ligneFrais);

        Button ajouter = new Button(this);
        ajouter.setText("Ajouter");
        ajouter.setBackgroundColor(ACCENT);
        ajouter.setTextColor(Color.WHITE);
        ajouter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ajouterOperationManuelle();
            }
        });
        carte.addView(ajouter);

        return carte;
    }

    private LinearLayout.LayoutParams margeBasPetite() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        return lp;
    }

    private void ajouterOperationManuelle() {
        String montantTxte = champMontant.getText().toString().trim();
        if (montantTxte.isEmpty()) {
            Toast.makeText(this, "Entrez un montant.", Toast.LENGTH_SHORT).show();
            return;
        }
        double montant;
        try {
            montant = Double.parseDouble(montantTxte);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide.", Toast.LENGTH_SHORT).show();
            return;
        }
        String type = TYPES[spType.getSelectedItemPosition()];
        double frais = 0;
        if ("ret".equals(type)) {
            String fTxte = champFrais.getText().toString().trim();
            if (fTxte.isEmpty()) {
                Toast.makeText(this, "Entrez les frais de retrait.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                frais = Double.parseDouble(fTxte);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Frais invalides.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Operation o = new Operation();
        o.id = System.currentTimeMillis() + "-" + (int) (Math.random() * 100000);
        o.date = jourCourant;
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.FRANCE);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        o.at = iso.format(new Date());
        o.sim = BeninPhone.OPERATEURS[spSim.getSelectedItemPosition()];
        o.type = type;
        o.amount = montant;
        o.frais = "ret".equals(type) ? frais : 0;
        o.profit = taux.benefice(type, montant, frais);
        o.source = "manuel";
        o.note = "Saisie manuelle";

        store.add(o);
        champMontant.setText("");
        champFrais.setText("");
        rafraichir();
    }

    private void rafraichir() {
        List<Operation> jour = store.byDate(jourCourant);
        OperationStore.Totaux t = OperationStore.agreger(jour);
        totauxTxt.setText(
                "Dépôts : " + fmt(t.depotMontant) + " F (" + t.depotN + ")\n" +
                "Retraits : " + fmt(t.retraitMontant) + " F (" + t.retraitN + ")\n" +
                "Crédit : " + fmt(t.creditMontant) + " F (" + t.creditN + ")\n" +
                "Forfaits : " + fmt(t.forfaitMontant) + " F (" + t.forfaitN + ")\n" +
                "Bénéfice : " + fmt(t.benefice) + " F");

        liste.removeAllViews();
        if (jour.isEmpty()) {
            TextView vide = new TextView(this);
            vide.setText("Aucune opération aujourd'hui.");
            vide.setTextColor(MUTED);
            liste.addView(vide);
        } else {
            for (Operation o : jour) liste.addView(ligneOperation(o));
        }
    }

    private View ligneOperation(final Operation o) {
        LinearLayout ligne = new LinearLayout(this);
        ligne.setOrientation(LinearLayout.HORIZONTAL);
        ligne.setGravity(Gravity.CENTER_VERTICAL);
        ligne.setBackgroundColor(CARD);
        int p = dp(10);
        ligne.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lpLigne = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLigne.bottomMargin = dp(8);
        ligne.setLayoutParams(lpLigne);

        TextView texte = new TextView(this);
        texte.setTextColor(Color.WHITE);
        String src = "sms".equals(o.source) ? " · SMS auto" : " · manuel";
        texte.setText(o.sim + " · " + Operation.libelleType(o.type) + "\n"
                + fmt(o.amount) + " F · bénéfice " + fmt(o.profit) + " F" + src);
        LinearLayout.LayoutParams lpTexte = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        ligne.addView(texte, lpTexte);

        Button suppr = new Button(this);
        suppr.setText("✕");
        suppr.setBackgroundColor(CARD);
        suppr.setTextColor(0xFFB3321F);
        suppr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.delete(o.id);
                rafraichir();
            }
        });
        ligne.addView(suppr, new LinearLayout.LayoutParams(dp(44), dp(44)));

        return ligne;
    }

    private String fmt(double v) {
        return String.format(Locale.FRANCE, "%,.0f", v).replace(',', ' ');
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rafraichir();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        if (current == this) current = null;
        super.onDestroy();
    }
}
