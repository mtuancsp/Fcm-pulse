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
            next = pushOutOfQuiet(next, Prefs.quietStartMin(c), Prefs.quietEndMin(c));
        }
        return next;
    }

    static boolean inQuiet(Context c, long t) {
        if (!Prefs.quietEnabled(c)) return false;
        return isInWindow(t, Prefs.quietStartMin(c), Prefs.quietEndMin(c));
    }

    /** startMin và endMin là phút trong ngày; khung có thể vắt qua nửa đêm (ví dụ 23:30 đến 06:30). */
    private static boolean isInWindow(long t, int startMin, int endMin) {
        if (startMin == endMin) return false;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        int m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        return startMin < endMin ? (m >= startMin && m < endMin) : (m >= startMin || m < endMin);
    }

    /** Nếu thời điểm t rơi vào khung giờ yên tĩnh thì dời tới đúng giờ kết thúc. */
    private static long pushOutOfQuiet(long t, int startMin, int endMin) {
        if (!isInWindow(t, startMin, endMin)) return t;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        cal.set(Calendar.HOUR_OF_DAY, endMin / 60);
        cal.set(Calendar.MINUTE, endMin % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= t) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }
}
