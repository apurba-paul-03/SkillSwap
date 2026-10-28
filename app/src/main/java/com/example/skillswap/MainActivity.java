package com.example.skillswap;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.skillswap.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private SkillAdapter skillAdapter;
    private NetworkStateReceiver networkStateReceiver;
    private List<SkillPost> skillList;

    // Hold fetched Retrofit locations in memory
    private List<LocationModel> cachedLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cachedLocations = new ArrayList<>();
        binding.recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));

        // 1. Initialize empty backing list for the adapter
        skillList = new ArrayList<>();

        // 2. Setup the adapter with the list and click listener
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

        networkStateReceiver = new NetworkStateReceiver();

        // Start background service
        Intent serviceIntent = new Intent(MainActivity.this, SyncSkillsService.class);
        startService(serviceIntent);

        // 3. Fetch skill data asynchronously from Firebase Firestore / Local Cache
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

        // 4. Fetch external Retrofit meeting locations in background
        fetchExternalLocations();

        // 5. Button click listener to show the meeting spots dialog
        binding.btnSelectLocation.setOnClickListener(v -> {
            if (cachedLocations != null && !cachedLocations.isEmpty()) {
                showLocationDialog(cachedLocations);
            } else {
                Toast.makeText(MainActivity.this, "Locations are still loading from API. Please wait...", Toast.LENGTH_SHORT).show();
            }
        });
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

                    // Format the selection string
                    String venueDetails = selected.getName() + " — City: " + selected.getCity();

                    // Update the TextView on the main screen!
                    binding.tvSelectedLocation.setText("Meeting Spot: " + venueDetails);
                    binding.tvSelectedLocation.setTextColor(getResources().getColor(android.R.color.black));

                    Toast.makeText(MainActivity.this,
                            "Meeting spot updated!",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkStateReceiver);
    }
}