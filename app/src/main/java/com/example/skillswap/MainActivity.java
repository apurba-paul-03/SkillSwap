package com.example.skillswap;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.skillswap.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private SkillAdapter skillAdapter;
    private SystemEventsReceiver systemEventsReceiver;
    private List<SkillPost> skillList;
    private ActivityResultLauncher<Intent> createSkillLauncher;

    // Hold fetched Retrofit locations in memory
    private List<LocationModel> cachedLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Display logged-in user email or guest status
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null || userEmail.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null) {
                userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            } else {
                userEmail = "Guest Student";
            }
        }
        binding.tvUserEmail.setText(userEmail);

        // Sign Out Listener
        binding.btnLogout.setOnClickListener(v -> {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
            Toast.makeText(MainActivity.this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        cachedLocations = new ArrayList<>();
        binding.recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));

        // 2. Initialize empty backing list for the adapter
        skillList = new ArrayList<>();

        // 3. Setup the adapter with the list and click listener
        skillAdapter = new SkillAdapter(skillList, skill -> {
            Intent intent = new Intent(MainActivity.this, SkillDetailActivity.class);
            intent.putExtra("TITLE", skill.getTitle());
            intent.putExtra("CATEGORY", skill.getCategory());
            intent.putExtra("DESC", skill.getDescription());
            intent.putExtra("AUTHOR", skill.getAuthorName());
            intent.putExtra("LOC", skill.getLocation());
            intent.putExtra("STATUS", skill.getStatus());
            startActivity(intent);
        });

        binding.recyclerViewSkills.setAdapter(skillAdapter);

        // Initialize ActivityResultLauncher for CreateSkillActivity
        createSkillLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadSkillsData();
                    }
                }
        );

        // Floating Action Button click listener to open CreateSkillActivity
        binding.fabAddSkill.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateSkillActivity.class);
            createSkillLauncher.launch(intent);
        });

        // Initialize System Broadcast Receiver
        systemEventsReceiver = new SystemEventsReceiver();

        // 4. Start Foreground Service for background skill sync with Notification Tray card
        requestNotificationPermission();
        Intent serviceIntent = new Intent(MainActivity.this, SyncSkillsService.class);
        try {
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service: " + e.getMessage());
            startService(serviceIntent);
        }

        // 5. Fetch skill data asynchronously from Firebase Firestore / Local Cache
        loadSkillsData();

        // 6. Fetch external Retrofit meeting locations in background
        fetchExternalLocations();

        // 7. Button click listener to show the meeting spots dialog
        binding.btnSelectLocation.setOnClickListener(v -> {
            if (cachedLocations != null && !cachedLocations.isEmpty()) {
                showLocationDialog(cachedLocations);
            } else {
                Toast.makeText(MainActivity.this, "Locations are still loading from API. Please wait...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSkillsData() {
        SkillRepository.getInstance().getSkills(new SkillRepository.DataLoadCallback() {
            @Override
            public void onDataLoaded(List<SkillPost> skills) {
                runOnUiThread(() -> {
                    skillList.clear();
                    skillList.addAll(skills);
                    skillAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading skills: " + e.getMessage());
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void fetchExternalLocations() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMeetingLocations().enqueue(new Callback<List<LocationModel>>() {
            @Override
            public void onResponse(Call<List<LocationModel>> call, Response<List<LocationModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedLocations = response.body();
                    Log.d("RetrofitSuccess", "Fetched " + cachedLocations.size() + " external meeting locations successfully!");
                } else {
                    Log.w("RetrofitWarning", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<LocationModel>> call, Throwable t) {
                Log.e("RetrofitError", "External API call failed: " + t.getMessage());
            }
        });
    }

    private void showLocationDialog(List<LocationModel> locations) {
        String[] locationNames = new String[locations.size()];
        for (int i = 0; i < locations.size(); i++) {
            LocationModel loc = locations.get(i);
            locationNames[i] = loc.getName() + " (" + loc.getCity() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select External Meeting Spot")
                .setItems(locationNames, (dialog, which) -> {
                    LocationModel selected = locations.get(which);
                    String venueDetails = selected.getName() + " — City: " + selected.getCity();

                    binding.tvSelectedLocation.setText("Meeting Spot: " + venueDetails);
                    binding.tvSelectedLocation.setTextColor(getResources().getColor(android.R.color.black));

                    Toast.makeText(MainActivity.this, "Meeting spot updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register System Broadcast Receiver for Battery, Airplane Mode, and Connectivity events
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(systemEventsReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (systemEventsReceiver != null) {
            try {
                unregisterReceiver(systemEventsReceiver);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
