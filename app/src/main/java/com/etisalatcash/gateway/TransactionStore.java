package com.etisalatcash.gateway;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class TransactionStore {

    private static final String PREFS = "gateway_prefs";
    private static final String KEY_TX = "transactions";
    private static final String KEY_WEBHOOK = "webhook_url";
    private static final int MAX_ITEMS = 200;

    private TransactionStore() {}

    public static synchronized void save(Context ctx, Transaction t) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr;
        try {
            arr = new JSONArray(prefs.getString(KEY_TX, "[]"));
        } catch (JSONException e) {
            arr = new JSONArray();
        }
        JSONArray next = new JSONArray();
        try {
            next.put(t.toJson());
        } catch (JSONException ignored) {
            return;
        }
        for (int i = 0; i < arr.length() && next.length() < MAX_ITEMS; i++) {
            next.put(arr.optJSONObject(i));
        }
        prefs.edit().putString(KEY_TX, next.toString()).apply();
    }

    public static synchronized List<Transaction> list(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<Transaction> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_TX, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optJSONObject(i) != null) {
                    out.add(new Transaction(arr.getJSONObject(i)));
                }
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static String getWebhookUrl(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_WEBHOOK, "");
    }

    public static void setWebhookUrl(Context ctx, String url) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_WEBHOOK, url == null ? "" : url.trim()).apply();
    }
}
