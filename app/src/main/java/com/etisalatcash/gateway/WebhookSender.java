package com.etisalatcash.gateway;

import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class WebhookSender {

    private static final String TAG = "WebhookSender";

    private WebhookSender() {}

    public static void post(String urlStr, Transaction t) {
        post(urlStr, t, null);
    }

    public static void post(String urlStr, Transaction t, String matchedRequestId) {
        if (urlStr == null || urlStr.isEmpty()) return;
        HttpURLConnection conn = null;
        try {
            org.json.JSONObject json = t.toJson();
            if (matchedRequestId != null) {
                json.put("matchedRequestId", matchedRequestId);
            }
            byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "EtisalatCashGateway/1.0");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int code = conn.getResponseCode();
            Log.i(TAG, "webhook response: " + code);
        } catch (Exception e) {
            Log.w(TAG, "webhook failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
