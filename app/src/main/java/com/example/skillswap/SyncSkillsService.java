package com.example.skillswap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SyncSkillsService extends Service {

    private static final String TAG = "SyncSkillsService";
    private static final String CHANNEL_ID = "skillswap_sync_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SyncSkillsService Created");
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "SkillSwap Data Sync Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Notification tray updates for SkillSwap background synchronization");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(String message) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SkillSwap Active Sync")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SyncSkillsService Started: Active Foreground Notification displayed.");

        // Start Foreground Service with Notification Tray status card
        Notification notification = buildNotification("Syncing latest peer skill posts & meeting spots...");
        startForeground(NOTIFICATION_ID, notification);

        new Thread(() -> {
            try {
                // Simulate network/database sync processing delay
                Thread.sleep(4000);
                Log.d(TAG, "Sync complete: SkillSwap Firestore & Retrofit cache synced.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Sync interrupted", e);
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf(startId);
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SyncSkillsService Destroyed");
    }
}
