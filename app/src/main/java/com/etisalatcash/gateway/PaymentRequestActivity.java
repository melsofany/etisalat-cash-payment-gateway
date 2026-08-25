package com.etisalatcash.gateway;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentRequestActivity extends Activity {

    private static final int REQ_SEND_SMS = 77;

    private EditText etMerchantPhone;
    private EditText etCustomerPhone;
    private EditText etAmount;
    private EditText etNote;
    private TextView tvRequests;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshRequests();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_request);

        etMerchantPhone = findViewById(R.id.etMerchantPhone);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        tvRequests = findViewById(R.id.tvRequests);

        etMerchantPhone.setText(RequestStore.getMerchantPhone(this));

        findViewById(R.id.btnSendSms).setOnClickListener(v -> createAndSend(true));
        findViewById(R.id.btnShare).setOnClickListener(v -> createAndSend(false));

        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRequests();
        IntentFilter f = new IntentFilter(SmsReceiver.ACTION_REFRESH);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(refreshReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(refreshReceiver, f);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(refreshReceiver);
    }

    private void createAndSend(boolean viaSms) {
        String merchant = etMerchantPhone.getText().toString().trim();
        String customer = etCustomerPhone.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (!PaymentRequest.isValidEgyptianPhone(merchant)) {
            toast("اكتب رقم محفظتك بشكل صحيح (01xxxxxxxxx) ⚠️");
            return;
        }
        if (!PaymentRequest.isValidEgyptianPhone(customer)) {
            toast("اكتب رقم محفظة العميل بشكل صحيح (01xxxxxxxxx) ⚠️");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", ""));
        } catch (NumberFormatException e) {
            toast("اكتب مبلغاً صحيحاً ⚠️");
            return;
        }
        if (amount <= 0) {
            toast("المبلغ يجب أن يكون أكبر من صفر ⚠️");
            return;
        }

        RequestStore.setMerchantPhone(this, merchant);

        PaymentRequest r = new PaymentRequest();
        r.id = PaymentRequest.newId();
        r.customerPhone = PaymentRequest.normalizePhone(customer);
        r.amount = amount;
        r.note = note;
        r.createdAt = System.currentTimeMillis();
        RequestStore.save(this, r);

        String message = r.buildMessage(PaymentRequest.normalizePhone(merchant));

        if (viaSms) {
            sendSms(r.customerPhone, message);
        } else {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(share, "مشاركة طلب الدفع"));
        }

        etCustomerPhone.setText("");
        etAmount.setText("");
        etNote.setText("");
        refreshRequests();
    }

    private void sendSms(String phone, String message) {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS);
            toast("امنح إذن إرسال الرسائل ثم اضغط إرسال مرة أخرى");
            return;
        }
        try {
            SmsManager sm = getSystemService(SmsManager.class);
            ArrayList<String> parts = sm.divideMessage(message);
            sm.sendMultipartTextMessage(phone, null, parts, null, null);
            toast("تم إرسال طلب الدفع ✅");
        } catch (Exception e) {
            toast("فشل إرسال الرسالة — جرّب المشاركة بدلاً منها");
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(share, "مشاركة طلب الدفع"));
        }
    }

    private void refreshRequests() {
        List<PaymentRequest> list = RequestStore.list(this);
        if (list.isEmpty()) {
            tvRequests.setText("لا توجد طلبات بعد");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.US);
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (PaymentRequest r : list) {
            if (shown++ >= 20) {
                sb.append("… (").append(list.size() - 20).append(" more)\n");
                break;
            }
            sb.append(r.paid ? "✅ " : "⏳ ")
                    .append(r.id)
                    .append("  |  ").append(String.format(Locale.US, "%.2f EGP", r.amount))
                    .append("  |  ").append(r.customerPhone)
                    .append("  |  ").append(fmt.format(new Date(r.createdAt)))
                    .append(r.note != null && !r.note.isEmpty() ? "  |  " + r.note : "")
                    .append('\n');
        }
        tvRequests.setText(sb.toString().trim());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
