package com.etisalatcash.gateway;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class RequestStore {

    private static final String PREFS = "gateway_prefs";
    private static final String KEY_REQ = "payment_requests";
    private static final String KEY_MERCHANT_PHONE = "merchant_phone";
    private static final int MAX_ITEMS = 200;

    private RequestStore() {}

    public static synchronized void save(Context ctx, PaymentRequest r) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = readArray(prefs);
        JSONArray next = new JSONArray();
        try {
            next.put(r.toJson());
        } catch (JSONException ignored) {
            return;
        }
        for (int i = 0; i < arr.length() && next.length() < MAX_ITEMS; i++) {
            next.put(arr.optJSONObject(i));
        }
        prefs.edit().putString(KEY_REQ, next.toString()).apply();
    }

    public static synchronized List<PaymentRequest> list(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<PaymentRequest> out = new ArrayList<>();
        JSONArray arr = readArray(prefs);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) out.add(new PaymentRequest(o));
        }
        return out;
    }

    /** Marks the oldest matching pending request as paid. Returns it, or null. */
    public static synchronized PaymentRequest markPaid(Context ctx, String fromPhone, double amount, long paidAt) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = readArray(prefs);
        // oldest pending first (stored newest-first, so scan from the end)
        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            PaymentRequest r = new PaymentRequest(o);
            if (!r.matches(fromPhone, amount)) continue;
            r.paid = true;
            r.paidAt = paidAt;
            try {
                arr.put(i, r.toJson());
                prefs.edit().putString(KEY_REQ, arr.toString()).apply();
            } catch (JSONException ignored) {
            }
            return r;
        }
        return null;
    }

    private static JSONArray readArray(SharedPreferences prefs) {
        try {
            return new JSONArray(prefs.getString(KEY_REQ, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static String getMerchantPhone(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MERCHANT_PHONE, "");
    }

    public static void setMerchantPhone(Context ctx, String phone) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MERCHANT_PHONE, phone == null ? "" : phone.trim()).apply();
    }
}
