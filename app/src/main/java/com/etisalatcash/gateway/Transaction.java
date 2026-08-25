package com.etisalatcash.gateway;

import org.json.JSONException;
import org.json.JSONObject;

public class Transaction {
    public long time;
    public String sender;
    public double amount;
    public String fromPhone;
    public String reference;
    public String rawBody;

    public Transaction() {}

    public Transaction(JSONObject o) {
        time = o.optLong("time");
        sender = o.optString("sender");
        amount = o.optDouble("amount");
        fromPhone = o.optString("fromPhone");
        reference = o.optString("reference");
        rawBody = o.optString("rawBody");
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("time", time);
        o.put("sender", sender == null ? "" : sender);
        o.put("amount", amount);
        o.put("fromPhone", fromPhone == null ? "" : fromPhone);
        o.put("reference", reference == null ? "" : reference);
        o.put("rawBody", rawBody == null ? "" : rawBody);
        return o;
    }
}
