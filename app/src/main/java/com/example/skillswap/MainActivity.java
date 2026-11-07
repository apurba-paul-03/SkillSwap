package com.example.skillswap;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.skillswap.databinding.ActivityMainBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private String selectedCategory = "All";
    private String currentSearchQuery = "";
    private String currentUserEmail = "Guest Student";
    private boolean isTutorMode = false;

    private static String cachedQuoteText = null;
    private static String cachedQuoteAuthor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved theme preference
        SharedPreferences prefs = getSharedPreferences("skillswap_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("is_dark_mode", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.switchDarkMode.setChecked(isDark);
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("is_dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // 1. Display logged-in user email or guest status
        String email = getIntent().getStringExtra("USER_EMAIL");
        if (email != null && !email.trim().isEmpty()) {
            currentUserEmail = email;
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null) {
            currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        } else {
            currentUserEmail = "Guest Student";
        }
        binding.tvUserEmail.setText(currentUserEmail);

        // Sign Out Listener
        binding.btnLogout.setOnClickListener(v -> {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
            Toast.makeText(MainActivity.this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        binding.recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));

        // 2. Initialize empty backing list for the adapter
        skillList = new ArrayList<>();

        // 3. Setup the adapter with the list and click listener
        skillAdapter = new SkillAdapter(skillList, skill -> {
            Intent intent = new Intent(MainActivity.this, SkillDetailActivity.class);
            intent.putExtra("ID", skill.getId());
            intent.putExtra("TITLE", skill.getTitle());
            intent.putExtra("CATEGORY", skill.getCategory());
            intent.putExtra("DESC", skill.getDescription());
            intent.putExtra("AUTHOR", skill.getAuthorName());
            intent.putExtra("LOC", skill.getLocation());
            intent.putExtra("STATUS", skill.getStatus());
            intent.putExtra("IMAGE_URL", skill.getImageUrl());
            intent.putExtra("CURRENT_USER_EMAIL", currentUserEmail);
            startActivity(intent);
        });

        binding.recyclerViewSkills.setAdapter(skillAdapter);

        // 4. Mode Switcher Listener (🎓 Student Mode vs 👨‍🏫 Tutor Mode)
        binding.toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeTutor) {
                    isTutorMode = true;
                    applyTutorModeUI();
                } else {
                    isTutorMode = false;
                    applyStudentModeUI();
                }
            }
        });

        // 5. Setup Live Search Listener
        binding.searchViewSkills.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                if (selectedCategory.contains("Saved")) {
                    loadSavedSkillsFromRoom();
                } else {
                    skillAdapter.filter(currentSearchQuery, selectedCategory);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                if (selectedCategory.contains("Saved")) {
                    loadSavedSkillsFromRoom();
                } else {
                    skillAdapter.filter(currentSearchQuery, selectedCategory);
                }
                return true;
            }
        });

        // 6. Setup Category Filter Chip Listener
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedCategory = chip.getText().toString();
                } else {
                    selectedCategory = "All";
                }
            } else {
                selectedCategory = "All";
            }

            if (selectedCategory.contains("Saved")) {
                loadSavedSkillsFromRoom();
            } else {
                binding.tvFeedTitle.setText("Available Skills to Learn");
                skillAdapter.updateList(skillList);
                skillAdapter.filter(currentSearchQuery, selectedCategory);
            }
        });

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
            intent.putExtra("USER_EMAIL", currentUserEmail);
            createSkillLauncher.launch(intent);
        });

        // Initialize System Broadcast Receiver
        systemEventsReceiver = new SystemEventsReceiver();

        // Start Foreground Service for background skill sync with Notification Tray card
        requestNotificationPermission();
        Intent serviceIntent = new Intent(MainActivity.this, SyncSkillsService.class);
        try {
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service: " + e.getMessage());
            startService(serviceIntent);
        }

        // Fetch skill data asynchronously from Firebase Firestore / Local Cache
        loadSkillsData();

        // Fetch Daily Motivational Study Quote from Retrofit REST API
        fetchMotivationalQuote();

        // Setup Firebase Realtime Database Presence Counter
        setupRealtimeDatabasePresence();
    }

    private void setupRealtimeDatabasePresence() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference activeUsersRef = database.getReference("active_users");

            String userKey = currentUserEmail != null && !currentUserEmail.isEmpty() ?
                    currentUserEmail.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase() : "guest_user";

            DatabaseReference myPresenceRef = activeUsersRef.child(userKey);
            myPresenceRef.setValue(true);
            myPresenceRef.onDisconnect().removeValue();

            activeUsersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    long activeCount = snapshot.getChildrenCount();
                    long displayCount = Math.max(1, activeCount);
                    binding.tvActiveStudentsCount.setText("🟢 Live Students Online: " + displayCount);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Realtime Database listener cancelled", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Firebase Realtime Database exception", e);
            binding.tvActiveStudentsCount.setText("🟢 Live Students Online: 1");
        }
    }

    private void loadSavedSkillsFromRoom() {
        binding.tvFeedTitle.setText("Saved Bookmarked Skills (Room DB ❤️)");
        new Thread(() -> {
            List<SavedSkillEntity> savedEntities = AppDatabase.getInstance(this).savedSkillDao().getAllSavedSkills();
            List<SkillPost> savedPosts = new ArrayList<>();
            for (SavedSkillEntity entity : savedEntities) {
                savedPosts.add(entity.toSkillPost());
            }
            runOnUiThread(() -> {
                if (savedPosts.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No saved skills yet. Tap 'Bookmark Skill' on any session page!", Toast.LENGTH_LONG).show();
                }
                skillAdapter.updateList(savedPosts);
            });
        }).start();
    }

    private void fetchMotivationalQuote() {
        if (cachedQuoteText != null && cachedQuoteAuthor != null) {
            binding.tvQuoteText.setText(cachedQuoteText);
            binding.tvQuoteAuthor.setText(cachedQuoteAuthor);
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getRandomQuote().enqueue(new Callback<QuoteModel>() {
            @Override
            public void onResponse(Call<QuoteModel> call, Response<QuoteModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    QuoteModel quoteObj = response.body();
                    cachedQuoteText = "\"" + quoteObj.getQuote() + "\"";
                    cachedQuoteAuthor = "— " + quoteObj.getAuthor();
                    binding.tvQuoteText.setText(cachedQuoteText);
                    binding.tvQuoteAuthor.setText(cachedQuoteAuthor);
                    Log.d(TAG, "Retrofit API successfully fetched quote: " + quoteObj.getQuote());
                }
            }

            @Override
            public void onFailure(Call<QuoteModel> call, Throwable t) {
                Log.e(TAG, "Retrofit API quote fetch failed, displaying fallback quote", t);
            }
        });
    }

    private void applyStudentModeUI() {
        binding.searchViewSkills.setVisibility(View.VISIBLE);
        binding.scrollCategoryFilter.setVisibility(View.VISIBLE);
        binding.fabAddSkill.setVisibility(View.GONE);
        if (selectedCategory.contains("Saved")) {
            loadSavedSkillsFromRoom();
        } else {
            binding.tvFeedTitle.setText("Available Skills to Learn");
            loadSkillsData();
        }
    }

    private void applyTutorModeUI() {
        binding.searchViewSkills.setVisibility(View.GONE);
        binding.scrollCategoryFilter.setVisibility(View.GONE);
        binding.fabAddSkill.setVisibility(View.VISIBLE);
        binding.tvFeedTitle.setText("My Offered Skills & Dashboard");

        // Filter list to show posts authored by the logged-in user
        String myHandle = currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : currentUserEmail;
        List<SkillPost> mySkills = new ArrayList<>();
        for (SkillPost post : skillList) {
            if (post.getAuthorName() != null &&
                    (post.getAuthorName().equalsIgnoreCase(myHandle) || post.getAuthorName().equalsIgnoreCase(currentUserEmail))) {
                mySkills.add(post);
            }
        }
        skillAdapter.updateList(mySkills);
    }

    private void loadSkillsData() {
        SkillRepository.getInstance().getSkills(new SkillRepository.DataLoadCallback() {
            @Override
            public void onDataLoaded(List<SkillPost> skills) {
                runOnUiThread(() -> {
                    skillList.clear();
                    skillList.addAll(skills);
                    if (isTutorMode) {
                        applyTutorModeUI();
                    } else if (selectedCategory.contains("Saved")) {
                        loadSavedSkillsFromRoom();
                    } else {
                        skillAdapter.updateList(skillList);
                    }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedCategory.contains("Saved")) {
            loadSavedSkillsFromRoom();
        }
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
