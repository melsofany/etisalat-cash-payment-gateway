package com.etisalatcash.gateway;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class PaymentRequest {
    public String id;
    public String customerPhone;
    public double amount;
    public String note;
    public long createdAt;
    public boolean paid;
    public long paidAt;

    public PaymentRequest() {}

    public PaymentRequest(JSONObject o) {
        id = o.optString("id");
        customerPhone = o.optString("customerPhone");
        amount = o.optDouble("amount");
        note = o.optString("note");
        createdAt = o.optLong("createdAt");
        paid = o.optBoolean("paid");
        paidAt = o.optLong("paidAt");
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id == null ? "" : id);
        o.put("customerPhone", customerPhone == null ? "" : customerPhone);
        o.put("amount", amount);
        o.put("note", note == null ? "" : note);
        o.put("createdAt", createdAt);
        o.put("paid", paid);
        o.put("paidAt", paidAt);
        return o;
    }

    public boolean matches(String fromPhone, double amt) {
        if (paid || fromPhone == null || customerPhone == null) return false;
        return normalizePhone(fromPhone).equals(normalizePhone(customerPhone))
                && Math.abs(amount - amt) < 0.01;
    }

    static String normalizePhone(String p) {
        if (p == null) return "";
        String digits = SmsParser.normalizeDigits(p).replaceAll("[^0-9]", "");
        if (digits.length() > 11) digits = digits.substring(digits.length() - 11);
        return digits;
    }

    public static boolean isValidEgyptianPhone(String p) {
        return normalizePhone(p).matches("01[0-9]{9}");
    }

    public static String newId() {
        int rand = java.util.concurrent.ThreadLocalRandom.current().nextInt(36 * 36);
        return "R" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.US)
                + (rand < 36 ? "0" : "") + Integer.toString(rand, 36).toUpperCase(Locale.US);
    }

    public String buildMessage(String merchantPhone) {
        StringBuilder sb = new StringBuilder();
        sb.append("طلب دفع - محفظة إلكترونية\n");
        sb.append(String.format(Locale.US, "المبلغ: %.2f جنيه\n", amount));
        sb.append("حوّل المبلغ إلى محفظة: ").append(merchantPhone).append('\n');
        sb.append("فودافون كاش: اطلب *9# | اتصالات كاش: اطلب *777#\n");
        if (note != null && !note.trim().isEmpty()) {
            sb.append("بيان: ").append(note.trim()).append('\n');
        }
        sb.append("رقم الطلب: ").append(id);
        return sb.toString();
    }
}
