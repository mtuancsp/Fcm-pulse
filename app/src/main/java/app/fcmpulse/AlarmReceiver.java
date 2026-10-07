package app.fcmpulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Được alarm gọi mỗi chu kỳ: gửi heartbeat rồi đặt alarm kế tiếp. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Prefs.enabled(context)) return;

        long now = System.currentTimeMillis();
        long expected = Prefs.nextTrigger(context);
        try {
            if (Scheduler.inQuiet(context, now)) {
                Prefs.log(context, "bỏ qua (đang trong giờ yên tĩnh)");
            } else {
                long late = expected > 0 ? (now - expected) / 1000 : 0;
                Prefs.log(context, "heartbeat, lệch " + late + "s: " + Heartbeat.send(context));
            }
        } catch (Exception e) {
            Prefs.log(context, "lỗi: " + e);
        } finally {
            // Luôn đặt alarm kế tiếp để chuỗi không bị đứt.
            Scheduler.schedule(context);
        }
    }
}
