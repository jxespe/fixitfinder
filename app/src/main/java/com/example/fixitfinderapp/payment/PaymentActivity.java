package com.example.fixitfinderapp.payment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.view.View;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

import com.example.fixitfinderapp.BaseSwipeActivity;
import com.example.fixitfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends BaseSwipeActivity {

    public static final String EXTRA_SUBSCRIPTION_FLOW = "subscription_flow";
    public static final String EXTRA_SUBSCRIPTION_PLAN = "subscription_plan";
    public static final String PLAN_MONTHLY = "monthly";
    public static final String PLAN_ANNUAL = "annual";

    private android.widget.RadioButton activeSubRadio;

    private boolean subscriptionFlow;
    private String subscriptionPlan = PLAN_MONTHLY;
    private int subscriptionAmountPesos;
    private String subscriptionPriceLabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        parseSubscriptionIntent(getIntent());

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnClose = findViewById(R.id.btnClose);
        Button btnContinue = findViewById(R.id.btnContinue);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        android.widget.RadioButton radioGcash = findViewById(R.id.radioGCash);
        android.widget.RadioButton radioBalance = findViewById(R.id.radioBalance);
        android.widget.RadioButton radioCash = findViewById(R.id.radioCash);
        android.widget.RadioButton radioCard = findViewById(R.id.radioCard);
        android.widget.RadioButton radioMaya = findViewById(R.id.radioMaya);
        android.widget.RadioButton radioBank = findViewById(R.id.radioBank);
        View rowGcash = findViewById(R.id.rowGcash);
        View rowBalance = findViewById(R.id.rowBalance);
        View rowCod = findViewById(R.id.rowCod);
        View rowCard = findViewById(R.id.rowCard);
        View rowMaya = findViewById(R.id.rowMaya);
        View rowBank = findViewById(R.id.rowBank);
        android.widget.LinearLayout listCards = findViewById(R.id.listCards);
        android.widget.LinearLayout listBanks = findViewById(R.id.listBanks);
        android.widget.LinearLayout listGcash = findViewById(R.id.listGcash);
        android.widget.LinearLayout listMaya = findViewById(R.id.listMaya);
        TextView btnAddGcash = findViewById(R.id.btnAddGcash);
        TextView btnAddCard = findViewById(R.id.btnAddCard);
        TextView btnAddMaya = findViewById(R.id.btnAddMaya);
        TextView btnAddBank = findViewById(R.id.btnAddBank);

        TextView tvGcashLabel = findViewById(R.id.tvGcashLabel);
        TextView tvBalanceLabel = findViewById(R.id.tvBalanceLabel);
        TextView tvBalanceSub = findViewById(R.id.tvBalanceSub);
        TextView tvCardLabel = findViewById(R.id.tvCardLabel);
        TextView tvMayaLabel = findViewById(R.id.tvMayaLabel);
        TextView tvBankLabel = findViewById(R.id.tvBankLabel);

        SharedPreferences prefs = getSharedPreferences("payment_prefs", MODE_PRIVATE);
        migrateLegacyCards(prefs);
        migrateLegacyBanks(prefs);
        updatePaymentLabels(prefs, tvGcashLabel, tvBalanceLabel, tvBalanceSub, tvCardLabel, tvMayaLabel, tvBankLabel);
        renderSavedCards(prefs, listCards, radioCard, radioGcash, radioBalance, radioCash, radioMaya, radioBank);
        renderSavedBanks(prefs, listBanks, radioBank, radioGcash, radioBalance, radioCash, radioMaya, radioCard);
        migrateLegacyMaya(prefs);
        renderSavedMaya(prefs, listMaya, radioMaya, radioGcash, radioBalance, radioCash, radioCard, radioBank);
        migrateLegacyGcash(prefs);
        renderSavedGcash(prefs, listGcash, radioGcash, radioBalance, radioCash, radioCard, radioMaya, radioBank);

        if (subscriptionFlow && "cod".equals(prefs.getString("selected_method", ""))) {
            prefs.edit().remove("selected_method").apply();
        }

        View layoutSubscriptionBanner = findViewById(R.id.layoutSubscriptionBanner);
        TextView tvSubscriptionSummary = findViewById(R.id.tvSubscriptionSummary);
        TextView tvTitle = findViewById(R.id.tvTitle);
        applySubscriptionUi(layoutSubscriptionBanner, tvSubscriptionSummary, tvTitle, rowCod, radioCash);
        if (btnContinue != null && subscriptionFlow) {
            btnContinue.setText(R.string.pay_now);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> handleContinueClick(prefs, radioGroup));
        }

        if (rowGcash != null) {
            rowGcash.setOnClickListener(v -> {
                setMainSelected(radioGcash, radioBalance, radioCash, radioCard, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "gcash").apply();
                String current = prefs.getString("gcash_number", "");
                if (current == null || current.isEmpty()) {
                    showGcashDialog(prefs, tvGcashLabel, null);
                }
            });
        }
        if (rowBalance != null) {
            rowBalance.setOnClickListener(v -> {
                setMainSelected(radioBalance, radioGcash, radioCash, radioCard, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "fixit_pay").apply();
                String current = prefs.getString("fixit_balance", "");
                if (current == null || current.isEmpty()) {
                    showBalanceDialog(prefs, tvBalanceSub);
                }
            });
        }
        if (rowCod != null) {
            rowCod.setOnClickListener(v -> {
                setMainSelected(radioCash, radioGcash, radioBalance, radioCard, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "cod").apply();
            });
        }
        if (rowCard != null) {
            rowCard.setOnClickListener(v -> {
                setMainSelected(radioCard, radioGcash, radioBalance, radioCash, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "card").apply();
                String current = prefs.getString("card_number", "");
                if (current == null || current.isEmpty()) {
                    showCardDialog(prefs, tvCardLabel, null);
                } else {
                    renderSavedCards(prefs, listCards, radioCard, radioGcash, radioBalance, radioCash, radioMaya, radioBank);
                }
            });
        }
        if (rowMaya != null) {
            rowMaya.setOnClickListener(v -> {
                setMainSelected(radioMaya, radioGcash, radioBalance, radioCash, radioCard, radioBank);
                prefs.edit().putString("selected_method", "maya").apply();
                String current = prefs.getString("maya_number", "");
                if (current == null || current.isEmpty()) {
                    showMayaDialog(prefs, tvMayaLabel, null);
                }
            });
        }
        if (rowBank != null) {
            rowBank.setOnClickListener(v -> {
                setMainSelected(radioBank, radioGcash, radioBalance, radioCash, radioCard, radioMaya);
                prefs.edit().putString("selected_method", "bank").apply();
                String current = prefs.getString("bank_account", "");
                if (current == null || current.isEmpty()) {
                    showBankDialog(prefs, tvBankLabel, null);
                } else {
                    renderSavedBanks(prefs, listBanks, radioBank, radioGcash, radioBalance, radioCash, radioMaya, radioCard);
                }
            });
        }

        if (radioGcash != null) {
            radioGcash.setOnClickListener(v -> {
                setMainSelected(radioGcash, radioBalance, radioCash, radioCard, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "gcash").apply();
                String current = prefs.getString("gcash_number", "");
                if (current == null || current.isEmpty()) {
                    showGcashDialog(prefs, tvGcashLabel, null);
                }
            });
        }
        if (radioBalance != null) {
            radioBalance.setOnClickListener(v -> {
                setMainSelected(radioBalance, radioGcash, radioCash, radioCard, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "fixit_pay").apply();
                String current = prefs.getString("fixit_balance", "");
                if (current == null || current.isEmpty()) {
                    showBalanceDialog(prefs, tvBalanceSub);
                }
            });
        }
        if (radioCash != null) {
            radioCash.setOnClickListener(v -> {
                setMainSelected(radioCash, radioGcash, radioBalance, radioCard, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "cod").apply();
            });
        }
        if (radioCard != null) {
            radioCard.setOnClickListener(v -> {
                setMainSelected(radioCard, radioGcash, radioBalance, radioCash, radioMaya, radioBank);
                prefs.edit().putString("selected_method", "card").apply();
                String current = prefs.getString("card_number", "");
                if (current == null || current.isEmpty()) {
                    showCardDialog(prefs, tvCardLabel, null);
                }
            });
        }
        if (radioMaya != null) {
            radioMaya.setOnClickListener(v -> {
                setMainSelected(radioMaya, radioGcash, radioBalance, radioCash, radioCard, radioBank);
                prefs.edit().putString("selected_method", "maya").apply();
                String current = prefs.getString("maya_number", "");
                if (current == null || current.isEmpty()) {
                    showMayaDialog(prefs, tvMayaLabel, null);
                }
            });
        }
        if (radioBank != null) {
            radioBank.setOnClickListener(v -> {
                setMainSelected(radioBank, radioGcash, radioBalance, radioCash, radioCard, radioMaya);
                prefs.edit().putString("selected_method", "bank").apply();
                String current = prefs.getString("bank_account", "");
                if (current == null || current.isEmpty()) {
                    showBankDialog(prefs, tvBankLabel, null);
                }
            });
        }

        if (btnAddGcash != null) {
            btnAddGcash.setOnClickListener(v -> showGcashDialog(prefs, tvGcashLabel, null));
        }
        if (btnAddCard != null) {
            btnAddCard.setOnClickListener(v -> showCardDialog(prefs, tvCardLabel, null));
        }
        if (btnAddMaya != null) {
            btnAddMaya.setOnClickListener(v -> showMayaDialog(prefs, tvMayaLabel, null));
        }
        if (btnAddBank != null) {
            btnAddBank.setOnClickListener(v -> showBankDialog(prefs, tvBankLabel, null));
        }
    }

    private void updatePaymentLabels(SharedPreferences prefs,
                                     TextView tvGcashLabel,
                                     TextView tvBalanceLabel,
                                     TextView tvBalanceSub,
                                     TextView tvCardLabel,
                                     TextView tvMayaLabel,
                                     TextView tvBankLabel) {
        String gcash = prefs.getString("gcash_number", "");
        if (tvGcashLabel != null) {
            tvGcashLabel.setText(gcash == null || gcash.isEmpty()
                    ? "GCash"
                    : "GCash (" + maskNumber(gcash) + ")");
        }
        String balance = prefs.getString("fixit_balance", "");
        if (tvBalanceLabel != null) {
            tvBalanceLabel.setText("FixIt Pay");
        }
        if (tvBalanceSub != null) {
            tvBalanceSub.setText(balance == null || balance.isEmpty()
                    ? "Load your wallet balance"
                    : "Balance: ₱" + balance);
        }
        String card = prefs.getString("card_number", "");
        String cardType = prefs.getString("card_type", "Card");
        if (tvCardLabel != null) {
            tvCardLabel.setText(card == null || card.isEmpty()
                    ? "Add credit/debit card"
                    : cardType + " (" + maskNumber(card) + ")");
        }
        String maya = prefs.getString("maya_number", "");
        if (tvMayaLabel != null) {
            tvMayaLabel.setText(maya == null || maya.isEmpty()
                    ? "Maya"
                    : "Maya (" + maskNumber(maya) + ")");
        }
        String bank = prefs.getString("bank_account", "");
        String bankName = prefs.getString("bank_name", "");
        if (tvBankLabel != null) {
            if (bank == null || bank.isEmpty()) {
                tvBankLabel.setText("Bank account");
            } else if (bankName != null && !bankName.isEmpty()) {
                tvBankLabel.setText(bankName + " (" + maskNumber(bank) + ")");
            } else {
                tvBankLabel.setText("Bank (" + maskNumber(bank) + ")");
            }
        }
    }

    private void showGcashDialog(SharedPreferences prefs, TextView tvGcashLabel, String existingEntry) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment_gcash, null);
        EditText edt = view.findViewById(R.id.edtGcashNumber);
        if (existingEntry != null) {
            edt.setText(existingEntry);
        }
        new AlertDialog.Builder(this)
                .setTitle(existingEntry == null ? "Add GCash number" : "Edit GCash number")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String value = edt.getText().toString().trim();
                    if (!value.isEmpty()) {
                        java.util.Set<String> list =
                                new java.util.HashSet<>(prefs.getStringSet("gcash_list", new java.util.HashSet<>()));
                        if (existingEntry != null) {
                            list.remove(existingEntry);
                        }
                        list.add(value);
                        prefs.edit().putStringSet("gcash_list", list).apply();
                        updatePaymentLabels(prefs, tvGcashLabel, null, null, null, null, null);
                        renderSavedGcash(prefs, findViewById(R.id.listGcash),
                                findViewById(R.id.radioGCash),
                                findViewById(R.id.radioBalance),
                                findViewById(R.id.radioCash),
                                findViewById(R.id.radioCard),
                                findViewById(R.id.radioMaya),
                                findViewById(R.id.radioBank));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBalanceDialog(SharedPreferences prefs, TextView tvBalanceSub) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment_balance, null);
        EditText edt = view.findViewById(R.id.edtBalanceAmount);
        new AlertDialog.Builder(this)
                .setTitle("Load FixIt Pay")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String value = edt.getText().toString().trim();
                    if (!value.isEmpty()) {
                        prefs.edit().putString("fixit_balance", value).apply();
                        updatePaymentLabels(prefs, null, null, tvBalanceSub, null, null, null);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMayaDialog(SharedPreferences prefs, TextView tvMayaLabel, String existingEntry) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment_maya, null);
        EditText edt = view.findViewById(R.id.edtMayaNumber);
        if (existingEntry != null) {
            edt.setText(existingEntry);
        }
        new AlertDialog.Builder(this)
                .setTitle(existingEntry == null ? "Add Maya number" : "Edit Maya number")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String value = edt.getText().toString().trim();
                    if (!value.isEmpty()) {
                        prefs.edit().putString("maya_number", value).apply();
                        updatePaymentLabels(prefs, null, null, null, null, tvMayaLabel, null);
                        java.util.Set<String> list =
                                new java.util.HashSet<>(prefs.getStringSet("maya_list", new java.util.HashSet<>()));
                        if (existingEntry != null) {
                            list.remove(existingEntry);
                        }
                        list.add(value);
                        prefs.edit().putStringSet("maya_list", list).apply();
                        renderSavedMaya(prefs, findViewById(R.id.listMaya),
                                findViewById(R.id.radioMaya),
                                findViewById(R.id.radioGCash),
                                findViewById(R.id.radioBalance),
                                findViewById(R.id.radioCash),
                                findViewById(R.id.radioCard),
                                findViewById(R.id.radioBank));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCardDialog(SharedPreferences prefs, TextView tvCardLabel, String existingEntry) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment_card, null);
        EditText edtNumber = view.findViewById(R.id.edtCardNumber);
        EditText edtExpiry = view.findViewById(R.id.edtCardExpiry);
        EditText edtCvv = view.findViewById(R.id.edtCardCvv);
        String existingNumber = "";
        if (existingEntry != null) {
            String[] parts = existingEntry.split("\\|");
            if (parts.length > 1) {
                existingNumber = parts[1];
                edtNumber.setText(existingNumber);
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(existingEntry == null ? "Add card" : "Edit card")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String number = edtNumber.getText().toString().trim();
                    String expiry = edtExpiry.getText().toString().trim();
                    String cvv = edtCvv.getText().toString().trim();
                    if (!number.isEmpty() && !expiry.isEmpty() && !cvv.isEmpty()) {
                        String cardType = detectCardType(number);
                        java.util.Set<String> cards = new java.util.HashSet<>(prefs.getStringSet("cards_list", new java.util.HashSet<>()));
                        if (existingEntry != null) {
                            cards.remove(existingEntry);
                        }
                        cards.add(cardType + "|" + number);
                        prefs.edit()
                                .putString("card_number", number)
                                .putString("card_expiry", expiry)
                                .putString("card_cvv", cvv)
                                .putString("card_type", cardType)
                                .putStringSet("cards_list", cards)
                                .apply();
                        updatePaymentLabels(prefs, null, null, null, tvCardLabel, null, null);
                        renderSavedCards(prefs, findViewById(R.id.listCards), null, null, null, null, null, null);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBankDialog(SharedPreferences prefs, TextView tvBankLabel, String existingEntry) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_payment_bank, null);
        EditText edtBankName = view.findViewById(R.id.edtBankName);
        EditText edtBankAccount = view.findViewById(R.id.edtBankAccount);
        if (existingEntry != null) {
            String[] parts = existingEntry.split("\\|");
            if (parts.length > 0) {
                edtBankName.setText(parts[0]);
            }
            if (parts.length > 1) {
                edtBankAccount.setText(parts[1]);
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(existingEntry == null ? "Add bank account" : "Edit bank account")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String bankName = edtBankName.getText().toString().trim();
                    String account = edtBankAccount.getText().toString().trim();
                    if (!account.isEmpty()) {
                        java.util.Set<String> banks = new java.util.HashSet<>(prefs.getStringSet("banks_list", new java.util.HashSet<>()));
                        if (existingEntry != null) {
                            banks.remove(existingEntry);
                        }
                        banks.add((bankName == null ? "" : bankName) + "|" + account);
                        prefs.edit()
                                .putString("bank_name", bankName)
                                .putString("bank_account", account)
                                .putStringSet("banks_list", banks)
                                .apply();
                        updatePaymentLabels(prefs, null, null, null, null, null, tvBankLabel);
                        renderSavedBanks(prefs, findViewById(R.id.listBanks), null, null, null, null, null, null);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String detectCardType(String number) {
        if (number == null || number.isEmpty()) {
            return "Card";
        }
        if (number.startsWith("4")) {
            return "Visa";
        }
        if (number.startsWith("5")) {
            return "Mastercard";
        }
        if (number.startsWith("34") || number.startsWith("37")) {
            return "AmEx";
        }
        if (number.startsWith("6")) {
            return "Discover";
        }
        return "Card";
    }

    private String maskNumber(String number) {
        if (number == null) {
            return "****";
        }
        String digits = number.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****" + digits;
        }
        String last4 = digits.substring(digits.length() - 4);
        return "****" + last4;
    }

    private void setMainSelected(android.widget.RadioButton selected,
                                 android.widget.RadioButton... others) {
        setMainSelectedInternal(selected, true, others);
    }

    private void setMainSelectedInternal(android.widget.RadioButton selected,
                                         boolean clearSub,
                                         android.widget.RadioButton... others) {
        if (others != null) {
            for (android.widget.RadioButton rb : others) {
                if (rb != null) {
                    rb.setChecked(false);
                }
            }
        }
        if (selected != null) selected.setChecked(true);
        if (clearSub && activeSubRadio != null) {
            activeSubRadio.setChecked(false);
        }
    }

    private void setSubSelected(android.widget.RadioButton subRadio,
                                android.widget.RadioButton mainRadio,
                                android.widget.RadioButton... others) {
        if (activeSubRadio != null && activeSubRadio != subRadio) {
            activeSubRadio.setChecked(false);
        }
        activeSubRadio = subRadio;
        if (mainRadio != null) {
            setMainSelectedInternal(mainRadio, false, others);
        }
        if (subRadio != null) {
            subRadio.setChecked(true);
        }
    }

    private void renderSavedCards(SharedPreferences prefs,
                                  android.widget.LinearLayout listCards,
                                  android.widget.RadioButton selected,
                                  android.widget.RadioButton r1,
                                  android.widget.RadioButton r2,
                                  android.widget.RadioButton r3,
                                  android.widget.RadioButton r4,
                                  android.widget.RadioButton r5) {
        if (listCards == null || prefs == null) {
            return;
        }
        listCards.removeAllViews();
        java.util.Set<String> cards = prefs.getStringSet("cards_list", null);
        if (cards == null || cards.isEmpty()) {
            listCards.setVisibility(View.GONE);
            return;
        }
        listCards.setVisibility(View.VISIBLE);
        for (String entry : cards) {
            if (entry == null) {
                continue;
            }
            String[] parts = entry.split("\\|");
            String type = parts.length > 0 ? parts[0] : "Card";
            String number = parts.length > 1 ? parts[1] : "";
            android.widget.LinearLayout row = buildSubRow(
                    type + " (" + maskNumber(number) + ")",
                    selected,
                    r1,
                    r2,
                    r3,
                    r4,
                    r5,
                    () -> showCardDialog(prefs, findViewById(R.id.tvCardLabel), entry),
                    () -> {
                        java.util.Set<String> next = new java.util.HashSet<>(cards);
                        next.remove(entry);
                        prefs.edit().putStringSet("cards_list", next).apply();
                        renderSavedCards(prefs, listCards, selected, r1, r2, r3, r4, r5);
                    }
            );
            listCards.addView(row);
        }
    }

    private void renderSavedBanks(SharedPreferences prefs,
                                  android.widget.LinearLayout listBanks,
                                  android.widget.RadioButton selected,
                                  android.widget.RadioButton r1,
                                  android.widget.RadioButton r2,
                                  android.widget.RadioButton r3,
                                  android.widget.RadioButton r4,
                                  android.widget.RadioButton r5) {
        if (listBanks == null || prefs == null) {
            return;
        }
        listBanks.removeAllViews();
        java.util.Set<String> banks = prefs.getStringSet("banks_list", null);
        if (banks == null || banks.isEmpty()) {
            listBanks.setVisibility(View.GONE);
            return;
        }
        listBanks.setVisibility(View.VISIBLE);
        for (String entry : banks) {
            if (entry == null) {
                continue;
            }
            String[] parts = entry.split("\\|");
            String bankName = parts.length > 0 ? parts[0] : "Bank";
            String number = parts.length > 1 ? parts[1] : "";
            android.widget.LinearLayout row = buildSubRow(
                    bankName + " (" + maskNumber(number) + ")",
                    selected,
                    r1,
                    r2,
                    r3,
                    r4,
                    r5,
                    () -> showBankDialog(prefs, findViewById(R.id.tvBankLabel), entry),
                    () -> {
                        java.util.Set<String> next = new java.util.HashSet<>(banks);
                        next.remove(entry);
                        prefs.edit().putStringSet("banks_list", next).apply();
                        renderSavedBanks(prefs, listBanks, selected, r1, r2, r3, r4, r5);
                    }
            );
            listBanks.addView(row);
        }
    }

    private void renderSavedMaya(SharedPreferences prefs,
                                 android.widget.LinearLayout listMaya,
                                 android.widget.RadioButton selected,
                                 android.widget.RadioButton r1,
                                 android.widget.RadioButton r2,
                                 android.widget.RadioButton r3,
                                 android.widget.RadioButton r4,
                                 android.widget.RadioButton r5) {
        if (listMaya == null || prefs == null) {
            return;
        }
        listMaya.removeAllViews();
        java.util.Set<String> numbers = prefs.getStringSet("maya_list", null);
        if (numbers == null || numbers.isEmpty()) {
            listMaya.setVisibility(View.GONE);
            return;
        }
        listMaya.setVisibility(View.VISIBLE);
        for (String entry : numbers) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            android.widget.LinearLayout row = buildSubRow(
                    "Maya (" + maskNumber(entry) + ")",
                    selected, r1, r2, r3, r4, r5,
                    () -> showMayaDialog(prefs, findViewById(R.id.tvMayaLabel), entry),
                    () -> {
                        java.util.Set<String> next = new java.util.HashSet<>(numbers);
                        next.remove(entry);
                        prefs.edit().putStringSet("maya_list", next).apply();
                        renderSavedMaya(prefs, listMaya, selected, r1, r2, r3, r4, r5);
                    }
            );
            listMaya.addView(row);
        }
    }

    private void migrateLegacyMaya(SharedPreferences prefs) {
        if (prefs == null) {
            return;
        }
        String legacy = prefs.getString("maya_number", "");
        java.util.Set<String> list = prefs.getStringSet("maya_list", null);
        if (legacy == null || legacy.isEmpty()) {
            return;
        }
        if (list != null && list.contains(legacy)) {
            return;
        }
        java.util.Set<String> next = new java.util.HashSet<>(list == null ? new java.util.HashSet<>() : list);
        next.add(legacy);
        prefs.edit().putStringSet("maya_list", next).apply();
    }

    private void migrateLegacyCards(SharedPreferences prefs) {
        if (prefs == null) {
            return;
        }
        String legacy = prefs.getString("card_number", "");
        String type = prefs.getString("card_type", "Card");
        java.util.Set<String> list = prefs.getStringSet("cards_list", null);
        if (legacy == null || legacy.isEmpty()) {
            return;
        }
        String entry = type + "|" + legacy;
        if (list != null && list.contains(entry)) {
            return;
        }
        java.util.Set<String> next = new java.util.HashSet<>(list == null ? new java.util.HashSet<>() : list);
        next.add(entry);
        prefs.edit().putStringSet("cards_list", next).apply();
    }

    private void migrateLegacyBanks(SharedPreferences prefs) {
        if (prefs == null) {
            return;
        }
        String legacy = prefs.getString("bank_account", "");
        String name = prefs.getString("bank_name", "");
        java.util.Set<String> list = prefs.getStringSet("banks_list", null);
        if (legacy == null || legacy.isEmpty()) {
            return;
        }
        String entry = (name == null ? "" : name) + "|" + legacy;
        if (list != null && list.contains(entry)) {
            return;
        }
        java.util.Set<String> next = new java.util.HashSet<>(list == null ? new java.util.HashSet<>() : list);
        next.add(entry);
        prefs.edit().putStringSet("banks_list", next).apply();
    }

    private android.widget.LinearLayout buildSubRow(String label,
                                                    android.widget.RadioButton selected,
                                                    android.widget.RadioButton r1,
                                                    android.widget.RadioButton r2,
                                                    android.widget.RadioButton r3,
                                                    android.widget.RadioButton r4,
                                                    android.widget.RadioButton r5,
                                                    Runnable onEdit,
                                                    Runnable onRemove) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(12, 8, 12, 8);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF6B7280);
        android.widget.LinearLayout.LayoutParams tvParams =
                new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Edit");
        btnEdit.setTextSize(12f);
        btnEdit.setTextColor(0xFF1D4ED8);
        btnEdit.setPadding(12, 6, 12, 6);
        btnEdit.setOnClickListener(v -> {
            if (onEdit != null) {
                onEdit.run();
            }
        });

        TextView btnRemove = new TextView(this);
        btnRemove.setText("Remove");
        btnRemove.setTextSize(12f);
        btnRemove.setTextColor(0xFFB91C1C);
        btnRemove.setPadding(12, 6, 12, 6);
        btnRemove.setOnClickListener(v -> {
            if (onRemove != null) {
                onRemove.run();
            }
        });

        android.widget.RadioButton rb = new android.widget.RadioButton(this);
        rb.setOnClickListener(v -> {
            setSubSelected(rb, selected, r1, r2, r3, r4, r5);
            SharedPreferences prefs = getSharedPreferences("payment_prefs", MODE_PRIVATE);
            prefs.edit().putString("selected_method", "sub_option").apply();
        });

        row.setOnClickListener(v -> {
            setSubSelected(rb, selected, r1, r2, r3, r4, r5);
            SharedPreferences prefs = getSharedPreferences("payment_prefs", MODE_PRIVATE);
            prefs.edit().putString("selected_method", "sub_option").apply();
        });
        row.addView(tv);
        row.addView(btnEdit);
        row.addView(btnRemove);
        row.addView(rb);
        return row;
    }

    private void renderSingleMethod(SharedPreferences prefs,
                                    android.widget.LinearLayout container,
                                    String key,
                                    String label,
                                    android.widget.RadioButton selected,
                                    android.widget.RadioButton r1,
                                    android.widget.RadioButton r2,
                                    android.widget.RadioButton r3,
                                    android.widget.RadioButton r4,
                                    android.widget.RadioButton r5,
                                    Runnable onEdit,
                                    Runnable onRemove) {
        if (container == null || prefs == null) {
            return;
        }
        container.removeAllViews();
        String value = prefs.getString(key, "");
        if (value == null || value.isEmpty()) {
            return;
        }
        android.widget.LinearLayout row = buildSubRow(
                label + " (" + maskNumber(value) + ")",
                selected, r1, r2, r3, r4, r5, onEdit, onRemove
        );
        container.addView(row);
    }

    private void renderSavedGcash(SharedPreferences prefs,
                                  android.widget.LinearLayout listGcash,
                                  android.widget.RadioButton selected,
                                  android.widget.RadioButton r1,
                                  android.widget.RadioButton r2,
                                  android.widget.RadioButton r3,
                                  android.widget.RadioButton r4,
                                  android.widget.RadioButton r5) {
        if (listGcash == null || prefs == null) {
            return;
        }
        listGcash.removeAllViews();
        java.util.Set<String> numbers = prefs.getStringSet("gcash_list", null);
        if (numbers == null || numbers.isEmpty()) {
            return;
        }
        for (String entry : numbers) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            android.widget.LinearLayout row = buildSubRow(
                    "GCash (" + maskNumber(entry) + ")",
                    selected, r1, r2, r3, r4, r5,
                    () -> showGcashDialog(prefs, findViewById(R.id.tvGcashLabel), entry),
                    () -> {
                        java.util.Set<String> next = new java.util.HashSet<>(numbers);
                        next.remove(entry);
                        prefs.edit().putStringSet("gcash_list", next).apply();
                        renderSavedGcash(prefs, listGcash, selected, r1, r2, r3, r4, r5);
                    }
            );
            listGcash.addView(row);
        }
    }

    private void migrateLegacyGcash(SharedPreferences prefs) {
        if (prefs == null) {
            return;
        }
        String legacy = prefs.getString("gcash_number", "");
        java.util.Set<String> list = prefs.getStringSet("gcash_list", null);
        if (legacy == null || legacy.isEmpty() || (list != null && list.contains(legacy))) {
            return;
        }
        java.util.Set<String> next = new java.util.HashSet<>(list == null ? new java.util.HashSet<>() : list);
        next.add(legacy);
        prefs.edit().putStringSet("gcash_list", next).apply();
    }

    private void parseSubscriptionIntent(Intent intent) {
        subscriptionFlow = intent != null && intent.getBooleanExtra(EXTRA_SUBSCRIPTION_FLOW, false);
        String plan = intent != null ? intent.getStringExtra(EXTRA_SUBSCRIPTION_PLAN) : null;
        if (!subscriptionFlow) {
            subscriptionPlan = PLAN_MONTHLY;
            subscriptionAmountPesos = 0;
            subscriptionPriceLabel = "";
            return;
        }
        if (PLAN_ANNUAL.equals(plan)) {
            subscriptionPlan = PLAN_ANNUAL;
            subscriptionAmountPesos = 1499;
            subscriptionPriceLabel = getString(R.string.subscription_price_annual);
        } else {
            subscriptionPlan = PLAN_MONTHLY;
            subscriptionAmountPesos = 149;
            subscriptionPriceLabel = getString(R.string.subscription_price_monthly);
        }
    }

    private void applySubscriptionUi(View banner,
                                     TextView tvSum,
                                     TextView tvTitle,
                                     View rowCod,
                                     android.widget.RadioButton radioCash) {
        if (!subscriptionFlow) {
            if (banner != null) {
                banner.setVisibility(View.GONE);
            }
            return;
        }
        if (banner != null) {
            banner.setVisibility(View.VISIBLE);
        }
        if (tvSum != null) {
            String planTitle = PLAN_ANNUAL.equals(subscriptionPlan)
                    ? getString(R.string.subscription_plan_annual)
                    : getString(R.string.subscription_plan_monthly);
            tvSum.setText(planTitle + "\n" + subscriptionPriceLabel);
        }
        if (tvTitle != null) {
            tvTitle.setText(R.string.payment_screen_title);
        }
        if (rowCod != null) {
            rowCod.setVisibility(View.GONE);
        }
        if (radioCash != null) {
            radioCash.setVisibility(View.GONE);
        }
    }

    private void handleContinueClick(SharedPreferences prefs, RadioGroup radioGroup) {
        String method = prefs.getString("selected_method", "");
        int selected = radioGroup != null ? radioGroup.getCheckedRadioButtonId() : -1;
        boolean hasSelection = selected != -1 || (method != null && !method.isEmpty());
        if (!hasSelection) {
            Toast.makeText(this, "Please select a payment method.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (subscriptionFlow) {
            showSubscriptionConfirmDialog(method, prefs);
        } else {
            Toast.makeText(this, "Payment method selected.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showSubscriptionConfirmDialog(String method, SharedPreferences prefs) {
        String methodLabel = humanizePaymentMethod(method);
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_subscription_title)
                .setMessage(getString(R.string.confirm_subscription_message,
                        subscriptionPriceLabel, methodLabel))
                .setPositiveButton(R.string.pay_now, (d, w) -> persistSubscription(method, prefs))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String humanizePaymentMethod(String method) {
        if (method == null || method.isEmpty()) {
            return getString(R.string.payment_method_generic);
        }
        switch (method) {
            case "gcash":
                return "GCash";
            case "maya":
                return "Maya";
            case "card":
                return getString(R.string.payment_method_card);
            case "bank":
                return getString(R.string.payment_method_bank);
            case "fixit_pay":
                return "FixIt Pay";
            case "cod":
                return "Cash on delivery";
            default:
                return method;
        }
    }

    private void persistSubscription(String method, SharedPreferences prefs) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("subscriptionActive", true);
        updates.put("subscriptionPlan", subscriptionPlan);
        updates.put("subscriptionAmountPesos", subscriptionAmountPesos);
        updates.put("subscriptionPriceLabel", subscriptionPriceLabel);
        updates.put("subscriptionPaymentMethod", method);
        updates.put("subscriptionUpdatedAt", FieldValue.serverTimestamp());
        long renewEstimateMs = PLAN_ANNUAL.equals(subscriptionPlan)
                ? 365L * 24 * 60 * 60 * 1000
                : 30L * 24 * 60 * 60 * 1000;
        updates.put("subscriptionRenewsAtEstimate", System.currentTimeMillis() + renewEstimateMs);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(u -> {
                    prefs.edit()
                            .putBoolean("subscription_active", true)
                            .putString("subscription_plan", subscriptionPlan)
                            .apply();
                    Toast.makeText(this, R.string.subscription_activated, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Could not save subscription: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}
