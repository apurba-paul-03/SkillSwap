package com.example.skillswap;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.skillswap.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SkillAdapter skillAdapter;
    private NetworkStateReceiver networkStateReceiver;
    private List<SkillPost> skillList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));

        // 1. Initialize an empty backing list for the adapter
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

        Intent serviceIntent = new Intent(MainActivity.this, SyncSkillsService.class);
        startService(serviceIntent);

        // 3. Fetch data asynchronously using the callback interface
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
                // Handle error if needed
            }
        });
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