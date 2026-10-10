package com.example.skillswap;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.skillswap.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SkillAdapter skillAdapter;
    private List<SkillPost> skillList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));

        loadMockData();

        skillAdapter = new SkillAdapter(skillList, skill -> {
            // When a card is clicked, show a Toast (Later, we'll launch a DetailActivity here using an Intent)
            Toast.makeText(MainActivity.this, "Clicked: " + skill.getTitle(), Toast.LENGTH_SHORT).show();
        });

        binding.recyclerViewSkills.setAdapter(skillAdapter);
    }

    private void loadMockData() {
        skillList = new ArrayList<>();
        skillList.add(new SkillPost("1", "Android Development Basics", "Programming",
                "Offering weekend crash courses covering Activities, Intents, and RecyclerViews.",
                "Jane Doe", "Central Library", "AVAILABLE"));

        skillList.add(new SkillPost("2", "Acoustic Guitar Lessons", "Music",
                "Learn basic chords, fingerpicking patterns, and popular song rhythms.",
                "Alex Smith", "Student Common Room", "AVAILABLE"));

        skillList.add(new SkillPost("3", "Calculus II Exam Prep", "Academics",
                "Reviewing series convergence, multiple integrals, and past exam questions.",
                "Rahim Ahmed", "Science Building 302", "IN PROGRESS"));
    }
}