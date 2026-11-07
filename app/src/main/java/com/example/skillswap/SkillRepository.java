package com.example.skillswap;

import android.util.Log;
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
    }

    public void getSkills(DataLoadCallback callback) {
        callback.onDataLoaded(skillList);

        db.collection("skills")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
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
                            skillList.clear();
                            Log.d(TAG, "Firestore collection is empty.");
                        }
                        callback.onDataLoaded(skillList);
                    } else {
                        Log.e(TAG, "Firestore fetch failed or offline.", task.getException());
                        callback.onDataLoaded(skillList);
                    }
                });
    }

    public void addSkill(SkillPost skill, SaveCallback callback) {
        skillList.add(0, skill);

        if (callback != null) {
            callback.onSuccess();
        }

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
