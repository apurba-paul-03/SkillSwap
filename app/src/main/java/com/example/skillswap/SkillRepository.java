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

    public interface SaveCallback {
        void onSuccess();
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
                                if (post != null) {
                                    if (post.getId() == null || post.getId().isEmpty()) {
                                        post.setId(document.getId());
                                    }
                                    skillList.add(post);
                                }
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

    public void addSkill(SkillPost skill, SaveCallback callback) {
        // Step 1: Add to local backing list immediately for instant UI update
        skillList.add(0, skill);

        // Step 2: Instantly notify UI callback so activity finishes without waiting for network task
        if (callback != null) {
            callback.onSuccess();
        }

        // Step 3: Attempt Firebase Firestore persistence in background
        try {
            db.collection("skills")
                    .document(skill.getId())
                    .set(skill)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Skill post saved to Firestore successfully: " + skill.getTitle()))
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore save failed, post retained in local list", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore Exception, local cache preserved", e);
        }
    }

    public void deleteSkill(String skillId, String skillTitle, SaveCallback callback) {
        if (skillList != null) {
            for (int i = skillList.size() - 1; i >= 0; i--) {
                SkillPost post = skillList.get(i);
                if (post != null && ((skillId != null && skillId.equalsIgnoreCase(post.getId()))
                        || (skillTitle != null && post.getTitle() != null && skillTitle.equalsIgnoreCase(post.getTitle())))) {
                    
                    String docIdToDelete = post.getId() != null && !post.getId().isEmpty() ? post.getId() : skillId;
                    skillList.remove(i);

                    // Delete from Firestore
                    try {
                        if (docIdToDelete != null && !docIdToDelete.isEmpty()) {
                            db.collection("skills").document(docIdToDelete).delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Skill post deleted from Firestore: " + docIdToDelete))
                                    .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Firestore delete exception", e);
                    }
                }
            }
        }

        if (callback != null) {
            callback.onSuccess();
        }
    }
}
