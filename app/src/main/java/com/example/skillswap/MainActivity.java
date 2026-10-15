package com.example.skillswap;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.skillswap.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SkillAdapter skillAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));

        skillAdapter = new SkillAdapter(SkillRepository.getInstance().getSkills(), skill -> {
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
    }
}