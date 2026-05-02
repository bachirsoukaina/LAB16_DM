package com.example.lab16_dm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerService extends Service {

    private static final String CHANNEL_ID = "timer_channel_01";
    private static final int NOTIF_ID = 2025;

    private int elapsedSeconds = 0;
    private boolean running = false;
    private ScheduledExecutorService scheduler;
    private NotificationManager nm;

    private final IBinder localBinder = new TimerBinder();

    public class TimerBinder extends Binder {
        public TimerService getInstance() {
            return TimerService.this;
        }
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        buildChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String cmd = (intent != null) ? intent.getAction() : null;
        if ("CMD_STOP".equals(cmd)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!running) {
            running = true;
            startForeground(NOTIF_ID, makeNotification());
            startTicking();
        }
        return START_STICKY;
    }

    private void startTicking() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                elapsedSeconds++;
                nm.notify(NOTIF_ID, makeNotification());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void buildChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Running",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(ch);
        }
    }

    private Notification makeNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer active")
                .setContentText("Elapsed: " + toTimeString(elapsedSeconds))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private String toTimeString(int totalSec) {
        return String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
        }
        stopForeground(true);
        super.onDestroy();
    }
}