package app.fcmpulse;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/** Gửi các broadcast "heartbeat" tới Google Play Services (không phải API công khai). */
final class Heartbeat {
    static final String GMS = "com.google.android.gms";

    private Heartbeat() {}

    /** Gửi mọi action đã cấu hình. Trả về chuỗi tóm tắt để ghi nhật ký. */
    static String send(Context c) {
        List<String> actions = Prefs.actions(c);
        if (actions.isEmpty()) return "không có action nào để gửi";

        StringBuilder sb = new StringBuilder();
        int ok = 0;
        for (String a : actions) {
            try {
                Intent i = new Intent(a);
                i.setPackage(GMS);
                c.sendBroadcast(i);
                ok++;
            } catch (Exception e) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(shortName(a)).append(": ").append(e.getClass().getSimpleName());
            }
        }
        Prefs.setLastHeartbeat(c, System.currentTimeMillis());
        String head = "đã gửi " + ok + "/" + actions.size() + " broadcast";
        return sb.length() == 0 ? head : head + " (" + sb + ")";
    }

    /** Mở socket tới mtalk.google.com:5228 để biết máy chủ FCM có tới được không. */
    static String probe() {
        long t0 = System.currentTimeMillis();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("mtalk.google.com", 5228), 5000);
            return "mtalk.google.com:5228 tới được (" + (System.currentTimeMillis() - t0) + " ms)";
        } catch (Exception e) {
            return "mtalk.google.com:5228 KHÔNG tới được: " + e.getClass().getSimpleName();
        }
    }

    static String gmsVersion(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(GMS, 0);
            return pi.versionName == null ? "?" : pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "không cài";
        } catch (Exception e) {
            return "?";
        }
    }

    private static String shortName(String action) {
        int i = action.lastIndexOf('.');
        return i >= 0 ? action.substring(i + 1) : action;
    }
}
