package com.example.skillswap;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class SyncSkillsService extends Service {

    private static final String TAG = "SyncSkillsService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SyncSkillsService Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SyncSkillsService Started: Checking for new skill posts...");

        // Simulate a background sync task (e.g., fetching new records or checking updates)
        new Thread(() -> {
            try {
                // Simulate network/database processing delay
                Thread.sleep(3000);
                Log.d(TAG, "Sync complete: Repository is up to date.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Sync interrupted", e);
            } finally {
                // Stop the service once the background task is finished
                stopSelf(startId);
            }
        }).start();

        // START_NOT_STICKY ensures the service isn't needlessly restarted if killed by the system
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We are using a started service, not a bound service
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SyncSkillsService Destroyed");
    }
}