package com.etisalatcash.gateway;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_PERMISSIONS = 42;

    private TextView tvStatus;
    private TextView tvTransactions;
    private Button btnToggleService;
    private EditText etWebhook;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshTransactions();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvTransactions = findViewById(R.id.tvTransactions);
        btnToggleService = findViewById(R.id.btnToggleService);
        etWebhook = findViewById(R.id.etWebhook);

        findViewById(R.id.btnPermissions).setOnClickListener(v -> requestAllPermissions());
        findViewById(R.id.btnBattery).setOnClickListener(v -> requestBatteryExemption());
        findViewById(R.id.btnNewRequest).setOnClickListener(v ->
                startActivity(new Intent(this, PaymentRequestActivity.class)));
        findViewById(R.id.btnSaveWebhook).setOnClickListener(v -> {
            TransactionStore.setWebhookUrl(this, etWebhook.getText().toString());
            Toast.makeText(this, "تم حفظ الرابط ✅", Toast.LENGTH_SHORT).show();
        });
        btnToggleService.setOnClickListener(v -> {
            if (GatewayService.isEnabled(this)) {
                GatewayService.stop(this);
            } else {
                GatewayService.start(this);
            }
            refreshStatus();
        });

        etWebhook.setText(TransactionStore.getWebhookUrl(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshTransactions();
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

    private void requestAllPermissions() {
        List<String> wanted = new ArrayList<>();
        wanted.add(Manifest.permission.RECEIVE_SMS);
        wanted.add(Manifest.permission.READ_SMS);
        wanted.add(Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= 33) {
            wanted.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        List<String> missing = new ArrayList<>();
        for (String p : wanted) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (missing.isEmpty()) {
            Toast.makeText(this, "كل الأذونات ممنوحة بالفعل ✅", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions(missing.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    private void requestBatteryExemption() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Toast.makeText(this, "التطبيق مستثنى بالفعل من توفير البطارية ✅", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void refreshStatus() {
        boolean enabled = GatewayService.isEnabled(this);
        tvStatus.setText(getString(enabled ? R.string.service_running : R.string.service_stopped));
        btnToggleService.setText(enabled ? "إيقاف خدمة المراقبة" : "٢. تشغيل خدمة المراقبة");
    }

    private void refreshTransactions() {
        List<Transaction> list = TransactionStore.list(this);
        if (list.isEmpty()) {
            tvTransactions.setText("لا توجد عمليات بعد");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Transaction t : list) {
            if (shown++ >= 20) {
                sb.append("… (").append(list.size() - 20).append(" more)\n");
                break;
            }
            sb.append(fmt.format(new Date(t.time)))
                    .append("  |  ").append(String.format(Locale.US, "%.2f EGP", t.amount))
                    .append(t.fromPhone != null ? "  |  من: " + t.fromPhone : "")
                    .append(t.reference != null ? "  |  مرجع: " + t.reference : "")
                    .append('\n');
        }
        tvTransactions.setText(sb.toString().trim());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }
            Toast.makeText(this,
                    allGranted ? "تم منح الأذونات ✅" : "بعض الأذونات مرفوضة — التطبيق لن يعمل بدونها ⚠️",
                    Toast.LENGTH_LONG).show();
        }
    }
}
