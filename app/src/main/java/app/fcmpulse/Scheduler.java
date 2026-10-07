package app.fcmpulse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Đặt / huỷ alarm lặp lại. Mỗi lần alarm nổ sẽ tự đặt alarm kế tiếp. */
final class Scheduler {
    private Scheduler() {}

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class);
        return PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent showIntent(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        return PendingIntent.getActivity(c, 2, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Đặt alarm kế tiếp theo cấu hình hiện tại. */
    static void schedule(Context c) {
        Context app = c.getApplicationContext();
        AlarmManager am = app.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(app);
        am.cancel(pi);

        long at = computeNext(app, System.currentTimeMillis());
        Prefs.setNextTrigger(app, at);

        if (Prefs.mode(app) == 1) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showIntent(app)), pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static void cancel(Context c) {
        Context app = c.getApplicationContext();
        app.getSystemService(AlarmManager.class).cancel(alarmIntent(app));
        Prefs.setNextTrigger(app, 0);
    }

    static long computeNext(Context c, long now) {
        long next = now + Prefs.intervalMin(c) * 60_000L;
        if (Prefs.quietEnabled(c)) {
            next = pushOutOfQuiet(next, Prefs.quietStart(c), Prefs.quietEnd(c));
        }
        return next;
    }

    static boolean inQuiet(Context c, long t) {
        if (!Prefs.quietEnabled(c)) return false;
        return isInWindow(t, Prefs.quietStart(c), Prefs.quietEnd(c));
    }

    private static boolean isInWindow(long t, int startH, int endH) {
        if (startH == endH) return false;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        return startH < endH ? (h >= startH && h < endH) : (h >= startH || h < endH);
    }

    /** Nếu thời điểm t rơi vào khung giờ yên tĩnh thì dời tới đầu giờ kết thúc. */
    private static long pushOutOfQuiet(long t, int startH, int endH) {
        if (!isInWindow(t, startH, endH)) return t;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, endH);
        if (cal.getTimeInMillis() <= t) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }
}
