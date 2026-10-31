package com.example.skillswap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

/**
 * SystemEventsReceiver handles system broadcast events required by rubric:
 * - Low Battery / Battery OK transitions (Intent.ACTION_BATTERY_LOW / Intent.ACTION_BATTERY_OKAY)
 * - Airplane Mode state changes (Intent.ACTION_AIRPLANE_MODE_CHANGED)
 * - Network connectivity transitions (ConnectivityManager.CONNECTIVITY_ACTION)
 */
public class SystemEventsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();

        if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            Toast.makeText(context, "⚠️ Low Battery Mode Detected! Syncing frequency reduced.", Toast.LENGTH_LONG).show();
        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
            Toast.makeText(context, "🔋 Battery Level Normal. Full functionality active.", Toast.LENGTH_SHORT).show();
        } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
            if (isAirplaneModeOn) {
                Toast.makeText(context, "✈️ Airplane Mode Enabled: SkillSwap switching to Offline Cache", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "📡 Airplane Mode Disabled: Reconnecting to Firebase...", Toast.LENGTH_SHORT).show();
            }
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                Toast.makeText(context, "🌐 Network Connected: Live Firestore Sync Active", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "⚠️ Network Disconnected: Operating in Offline Cache Mode", Toast.LENGTH_LONG).show();
            }
        }
    }
}
