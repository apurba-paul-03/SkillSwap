package com.example.skillswap;

import java.util.ArrayList;
import java.util.List;

public class SkillRepository {

    private static SkillRepository instance;
    private final List<SkillPost> skillList;

    private SkillRepository() {
        skillList = new ArrayList<>();
        loadInitialData();
    }

    public static synchronized SkillRepository getInstance() {
        if (instance == null) {
            instance = new SkillRepository();
        }
        return instance;
    }

    private void loadInitialData() {
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

    public List<SkillPost> getSkills() {
        return skillList;
    }

    public void addSkill(SkillPost post) {
        skillList.add(0, post); // Add to the top of the list
    }
}