package app.fcmpulse;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 1440;
    private static final int QUIET_STEP_MIN = 30;
    private static final String[] MODES = {
            "Tiết kiệm (không biểu tượng, có thể trễ khi Doze)",
            "Chính xác (alarm đồng hồ, hiện biểu tượng báo thức)"
    };

    private Switch swEnabled;
    private Switch swQuiet;
    private EditText etInterval;
    private Spinner spMode;
    private Spinner spQuietStart;
    private Spinner spQuietEnd;
    private EditText etActions;
    private TextView tvStatus;
    private TextView tvLog;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean loading;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadFromPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    // ---- giao diện -----------------------------------------------------

    private void buildUi() {
        setTitle("FCM Pulse");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(24));
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView intro = new TextView(this);
        intro.setText("Định kỳ gửi heartbeat tới Google Play Services để kết nối FCM không bị "
                + "nguội. Không cần root, không có dịch vụ chạy nền thường trực.");
        intro.setTextSize(13);
        root.addView(intro);

        swEnabled = new Switch(this);
        swEnabled.setText("Bật heartbeat định kỳ");
        swEnabled.setTextSize(16);
        swEnabled.setPadding(0, dp(14), 0, dp(6));
        root.addView(swEnabled);

        root.addView(label("Chu kỳ (phút, từ " + MIN_INTERVAL + " đến " + MAX_INTERVAL + ")"));
        etInterval = new EditText(this);
        etInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        etInterval.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        etInterval.setHint("15");
        root.addView(etInterval);

        TextView ivHint = new TextView(this);
        ivHint.setText("Chu kỳ ngắn tốn pin hơn. Với kiểu Tiết kiệm, Android thường giới hạn "
                + "khoảng 9 phút một lần khi máy ở Doze.");
        ivHint.setTextSize(12);
        root.addView(ivHint);

        root.addView(label("Kiểu alarm"));
        spMode = spinner(MODES);
        root.addView(spMode);

        swQuiet = new Switch(this);
        swQuiet.setText("Giờ yên tĩnh (không gửi, để máy ngủ sâu)");
        swQuiet.setPadding(0, dp(14), 0, dp(4));
        root.addView(swQuiet);

        LinearLayout captions = new LinearLayout(this);
        captions.setOrientation(LinearLayout.HORIZONTAL);
        TextView fromCap = new TextView(this);
        fromCap.setText("Từ");
        fromCap.setTextSize(12);
        TextView toCap = new TextView(this);
        toCap.setText("Đến");
        toCap.setTextSize(12);
        LinearLayout.LayoutParams capHalf =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        captions.addView(fromCap, capHalf);
        captions.addView(toCap, capHalf);
        root.addView(captions);

        int slots = 24 * 60 / QUIET_STEP_MIN;
        String[] times = new String[slots];
        for (int i = 0; i < slots; i++) times[i] = fmtMin(i * QUIET_STEP_MIN);
        spQuietStart = spinner(times);
        spQuietEnd = spinner(times);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams half =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(spQuietStart, half);
        row.addView(spQuietEnd, half);
        root.addView(row);

        root.addView(label("Action broadcast gửi tới Google Play Services (mỗi dòng một action)"));
        etActions = new EditText(this);
        etActions.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etActions.setMinLines(2);
        etActions.setTextSize(12);
        etActions.setTypeface(Typeface.MONOSPACE);
        root.addView(etActions);

        root.addView(button("Lưu và áp dụng", v -> {
            if (applyConfig(swEnabled.isChecked())) {
                Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
            }
        }));

        root.addView(label("Công cụ"));
        root.addView(button("Gửi heartbeat ngay", v -> {
            String r = Heartbeat.send(this);
            Prefs.log(this, "thủ công: " + r);
            refresh();
        }));
        root.addView(button("Kiểm tra kết nối FCM (mtalk:5228)", v -> {
            Toast.makeText(this, "Đang kiểm tra...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                String r = Heartbeat.probe();
                Prefs.log(getApplicationContext(), r);
                runOnUiThread(this::refresh);
            }).start();
        }));
        root.addView(button("Bỏ tối ưu hóa pin cho app này", v -> requestIgnoreBattery()));
        root.addView(button("Mở cài đặt ứng dụng", v -> openAppSettings()));

        TextView hint = new TextView(this);
        hint.setText("Để ColorOS không xoá alarm: trong cài đặt ứng dụng, đặt Pin = Không hạn chế / "
                + "Cho phép hoạt động nền, bật Tự khởi động, và khoá app trong màn hình đa nhiệm "
                + "(tên mục có thể khác tuỳ bản ColorOS).");
        hint.setTextSize(12);
        hint.setPadding(0, dp(8), 0, 0);
        root.addView(hint);

        root.addView(label("Trạng thái"));
        tvStatus = new TextView(this);
        tvStatus.setTextSize(13);
        root.addView(tvStatus);

        root.addView(label("Nhật ký (mới nhất ở trên)"));
        tvLog = new TextView(this);
        tvLog.setTextSize(11);
        tvLog.setTypeface(Typeface.MONOSPACE);
        tvLog.setTextIsSelectable(true);
        root.addView(tvLog);
        root.addView(button("Xoá nhật ký", v -> {
            Prefs.clearLog(this);
            refresh();
        }));

        setContentView(sv);
    }

    private void loadFromPrefs() {
        loading = true;
        swEnabled.setChecked(Prefs.enabled(this));
        etInterval.setText(String.valueOf(Prefs.intervalMin(this)));
        spMode.setSelection(Prefs.mode(this) == 1 ? 1 : 0);
        swQuiet.setChecked(Prefs.quietEnabled(this));
        int maxSlot = 24 * 60 / QUIET_STEP_MIN - 1;
        spQuietStart.setSelection(Math.min(maxSlot, Prefs.quietStartMin(this) / QUIET_STEP_MIN));
        spQuietEnd.setSelection(Math.min(maxSlot, Prefs.quietEndMin(this) / QUIET_STEP_MIN));
        etActions.setText(Prefs.actionsRaw(this));
        loading = false;

        swEnabled.setOnCheckedChangeListener((b, checked) -> {
            if (loading) return;
            if (!applyConfig(checked)) {
                loading = true;
                swEnabled.setChecked(!checked);
                loading = false;
            }
        });
    }

    // ---- hành động -----------------------------------------------------

    /** Trả về -1 nếu ô chu kỳ trống hoặc ngoài khoảng cho phép. */
    private int readInterval() {
        try {
            int v = Integer.parseInt(etInterval.getText().toString().trim());
            return (v >= MIN_INTERVAL && v <= MAX_INTERVAL) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Lưu cấu hình và đặt hoặc huỷ alarm. Trả về false nếu dữ liệu nhập không hợp lệ. */
    private boolean applyConfig(boolean enabled) {
        int interval = readInterval();
        if (interval < 0) {
            Toast.makeText(this, "Chu kỳ phải là số từ " + MIN_INTERVAL + " đến " + MAX_INTERVAL
                    + " phút", Toast.LENGTH_LONG).show();
            return false;
        }
        Prefs.saveConfig(this,
                enabled,
                interval,
                spMode.getSelectedItemPosition(),
                swQuiet.isChecked(),
                spQuietStart.getSelectedItemPosition() * QUIET_STEP_MIN,
                spQuietEnd.getSelectedItemPosition() * QUIET_STEP_MIN,
                etActions.getText().toString());
        if (enabled) {
            Scheduler.schedule(this);
            Prefs.log(this, "bật, chu kỳ " + interval + " phút");
        } else {
            Scheduler.cancel(this);
            Prefs.log(this, "tắt");
        }
        refresh();
        return true;
    }

    private void requestIgnoreBattery() {
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Toast.makeText(this, "Đã bỏ tối ưu hóa pin", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            openAppSettings();
        }
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được cài đặt", Toast.LENGTH_SHORT).show();
        }
    }

    private void refresh() {
        PowerManager pm = getSystemService(PowerManager.class);
        boolean ignoring = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        boolean on = Prefs.enabled(this);

        StringBuilder sb = new StringBuilder();
        sb.append("Trạng thái: ").append(on ? "ĐANG BẬT" : "tắt").append('\n');
        sb.append("Chu kỳ: ").append(Prefs.intervalMin(this)).append(" phút\n");
        sb.append("Giờ yên tĩnh: ");
        if (Prefs.quietEnabled(this)) {
            sb.append(fmtMin(Prefs.quietStartMin(this))).append(" đến ")
                    .append(fmtMin(Prefs.quietEndMin(this))).append('\n');
        } else {
            sb.append("tắt\n");
        }
        sb.append("Heartbeat gần nhất: ").append(fmt(Prefs.lastHeartbeat(this))).append('\n');
        sb.append("Alarm kế tiếp: ").append(on ? fmt(Prefs.nextTrigger(this)) : "—").append('\n');
        sb.append("Google Play Services: ").append(Heartbeat.gmsVersion(this)).append('\n');
        sb.append("Bỏ tối ưu hóa pin: ").append(ignoring ? "đã bỏ" : "CHƯA bỏ");
        tvStatus.setText(sb.toString());

        String raw = Prefs.logText(this);
        if (raw.isEmpty()) {
            tvLog.setText("(trống)");
        } else {
            String[] lines = raw.split("\n");
            StringBuilder out = new StringBuilder();
            for (int i = lines.length - 1; i >= 0; i--) {
                out.append(lines[i]);
                if (i > 0) out.append('\n');
            }
            tvLog.setText(out.toString());
        }
    }

    // ---- tiện ích ------------------------------------------------------

    private static String fmt(long t) {
        if (t <= 0) return "—";
        return new SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(new Date(t));
    }

    private static String fmtMin(int minuteOfDay) {
        return String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(14), 0, dp(4));
        return t;
    }

    private Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private Spinner spinner(String[] items) {
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, items));
        return sp;
    }
}
