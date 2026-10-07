package app.fcmpulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Đặt lại alarm sau khi khởi động máy hoặc sau khi app được cập nhật. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Prefs.enabled(context)) return;
        String why = Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                ? "khởi động máy" : "cập nhật app";
        Prefs.log(context, "đặt lại alarm sau " + why);
        Scheduler.schedule(context);
    }
}
