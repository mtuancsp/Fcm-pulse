package app.fcmpulse;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Cấu hình + nhật ký, lưu trong SharedPreferences. */
final class Prefs {
    static final String DEFAULT_ACTIONS =
            "com.google.android.intent.action.MCS_HEARTBEAT\n"
                    + "com.google.android.intent.action.GTALK_HEARTBEAT";

    private static final String FILE = "fcmpulse";
    private static final int MAX_LOG_LINES = 80;

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) { return sp(c).getBoolean("enabled", false); }
    static int intervalMin(Context c) { return sp(c).getInt("interval", 15); }
    /** 0 = setAndAllowWhileIdle (tiết kiệm), 1 = setAlarmClock (chính xác). */
    static int mode(Context c) { return sp(c).getInt("mode", 0); }
    static boolean quietEnabled(Context c) { return sp(c).getBoolean("quiet", false); }
    /** Giờ yên tĩnh tính bằng phút trong ngày (0..1439). */
    static int quietStartMin(Context c) { return sp(c).getInt("qsm", 23 * 60); }
    static int quietEndMin(Context c) { return sp(c).getInt("qem", 6 * 60); }
    static String actionsRaw(Context c) { return sp(c).getString("actions", DEFAULT_ACTIONS); }
    static long nextTrigger(Context c) { return sp(c).getLong("next", 0); }
    static long lastHeartbeat(Context c) { return sp(c).getLong("last", 0); }

    static List<String> actions(Context c) {
        List<String> out = new ArrayList<>();
        for (String line : actionsRaw(c).split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    static void saveConfig(Context c, boolean enabled, int interval, int mode,
                           boolean quiet, int quietStartMin, int quietEndMin, String actions) {
        sp(c).edit()
                .putBoolean("enabled", enabled)
                .putInt("interval", interval)
                .putInt("mode", mode)
                .putBoolean("quiet", quiet)
                .putInt("qsm", quietStartMin)
                .putInt("qem", quietEndMin)
                .putString("actions", actions)
                .commit();
    }

    static void setEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean("enabled", enabled).commit();
    }

    static void setNextTrigger(Context c, long t) { sp(c).edit().putLong("next", t).commit(); }
    static void setLastHeartbeat(Context c, long t) { sp(c).edit().putLong("last", t).commit(); }

    // ---- nhật ký -------------------------------------------------------

    static synchronized void log(Context c, String msg) {
        String stamp = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date());
        String old = sp(c).getString("log", "");
        String all = old.isEmpty() ? stamp + "  " + msg : old + "\n" + stamp + "  " + msg;
        String[] lines = all.split("\n");
        if (lines.length > MAX_LOG_LINES) {
            StringBuilder sb = new StringBuilder();
            for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(lines[i]);
            }
            all = sb.toString();
        }
        sp(c).edit().putString("log", all).commit();
    }

    static String logText(Context c) { return sp(c).getString("log", ""); }

    static synchronized void clearLog(Context c) { sp(c).edit().remove("log").commit(); }
}
