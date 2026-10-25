package com.example.skillswap;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SkillRepository {
    private static final String TAG = "SkillRepository";
    private static SkillRepository instance;
    private final FirebaseFirestore db;
    private final List<SkillPost> skillList;

    public interface DataLoadCallback {
        void onDataLoaded(List<SkillPost> skills);
        void onError(Exception e);
    }

    private SkillRepository() {
        db = FirebaseFirestore.getInstance();
        skillList = new ArrayList<>();
        // Step 1: Initialize local mock fallback items immediately
        loadLocalMockData();
    }

    public static synchronized SkillRepository getInstance() {
        if (instance == null) {
            instance = new SkillRepository();
        }
        return instance;
    }

    private void loadLocalMockData() {
        skillList.clear();
        skillList.add(new SkillPost("1", "Java Programming", "Technology", "Learn OOP principles and Java basics.", "Alice Smith", "Online / Campus", "Available"));
        skillList.add(new SkillPost("2", "UI/UX Design", "Design", "Wireframing and prototyping with Figma.", "Bob Jones", "Downtown Hub", "Open"));
        skillList.add(new SkillPost("3", "Spanish Conversation", "Language", "Practice conversational Spanish with a native speaker.", "Carlos Ruiz", "Library Cafe", "Active"));
    }

    public void getSkills(DataLoadCallback callback) {
        // Step 2: Instantly supply local mock cache to the UI so items appear immediately on launch
        callback.onDataLoaded(skillList);

        // Step 3: Fetch fresh records asynchronously from Firebase Firestore
        db.collection("skills")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Step 4: Verify if Firestore has live documents before wiping local cache
                        if (!task.getResult().isEmpty()) {
                            skillList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                SkillPost post = document.toObject(SkillPost.class);
                                skillList.add(post);
                            }
                            Log.d(TAG, "Successfully synced " + skillList.size() + " items from Firestore.");
                        } else {
                            Log.d(TAG, "Firestore collection is empty. Retaining local mock fallback data.");
                        }
                        // Step 5: Push updated list back to the UI adapter
                        callback.onDataLoaded(skillList);
                    } else {
                        Log.e(TAG, "Firestore fetch failed or offline. Preserving local mock data.", task.getException());
                        callback.onDataLoaded(skillList);
                    }
                });
    }
}