package com.example.skillswap;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skillswap.databinding.ActivitySkillDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkillDetailActivity extends AppCompatActivity {

    private static final String TAG = "SkillDetailActivity";
    private ActivitySkillDetailBinding binding;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

    private String skillTitle;
    private String authorName;
    private String currentUserEmail = "";
    private boolean hasAlreadyReviewed = false;
    private boolean isMyPost = false;
    private boolean isBookmarked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySkillDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String postId = getIntent().getStringExtra("ID");
        skillTitle = getIntent().getStringExtra("TITLE");
        String category = getIntent().getStringExtra("CATEGORY");
        String description = getIntent().getStringExtra("DESC");
        authorName = getIntent().getStringExtra("AUTHOR");
        String location = getIntent().getStringExtra("LOC");
        String status = getIntent().getStringExtra("STATUS");
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        currentUserEmail = getIntent().getStringExtra("CURRENT_USER_EMAIL");

        SkillAdapter.loadImageIntoView(binding.imgDetailPhoto, imageUrl);

        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    currentUserEmail = user.getEmail();
                }
            } catch (Exception ignored) {}
        }

        binding.tvDetailTitle.setText(skillTitle);
        binding.tvDetailCategory.setText(category);
        binding.tvDetailDescription.setText(description);
        binding.tvDetailAuthor.setText("By: " + authorName);
        binding.tvDetailLocation.setText(location);
        binding.tvDetailStatus.setText(status);

        String currentHandle = currentUserEmail != null && currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : currentUserEmail;
        isMyPost = authorName != null && (authorName.equalsIgnoreCase(currentHandle) || authorName.equalsIgnoreCase(currentUserEmail));

        // Tutors cannot rate themselves on their own post
        if (isMyPost) {
            binding.btnChatTutor.setText("View Student Inquiries & Chat");
            binding.btnRateSession.setVisibility(View.GONE);
            binding.btnDeleteSkill.setVisibility(View.VISIBLE);
        } else {
            binding.btnChatTutor.setText("Chat with Tutor (1-on-1)");
            binding.btnRateSession.setVisibility(View.VISIBLE);
            binding.btnDeleteSkill.setVisibility(View.GONE);
        }

        binding.btnDeleteSkill.setOnClickListener(v -> {
            new AlertDialog.Builder(SkillDetailActivity.this)
                    .setTitle("Delete Skill Post")
                    .setMessage("Are you sure you want to delete this skill post? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        SkillRepository.getInstance().deleteSkill(postId, skillTitle, new SkillRepository.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                runOnUiThread(() -> {
                                    Toast.makeText(SkillDetailActivity.this, "Skill post deleted successfully.", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                runOnUiThread(() -> Toast.makeText(SkillDetailActivity.this, "Failed to delete skill post", Toast.LENGTH_SHORT).show());
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Room DB Bookmark Setup
        String skillId = skillTitle != null ? skillTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.getDefault()) : "skill_id";
        checkRoomBookmarkStatus(skillId);

        binding.btnSaveSkill.setOnClickListener(v -> toggleRoomBookmark(skillId, skillTitle, category, description, authorName, location, status, imageUrl));

        binding.btnCall.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:+8801700000000")); // Sample phone number
            startActivity(callIntent);
        });

        binding.btnMap.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

        // Initialize ActivityResultLauncher for device Content Provider contact picker
        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        if (contactUri != null) {
                            queryContactContentProvider(contactUri);
                        }
                    }
                }
        );

        binding.btnInviteContact.setOnClickListener(v -> {
            Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            contactPickerLauncher.launch(contactIntent);
        });

        // Chat with Tutor Listener
        binding.btnChatTutor.setOnClickListener(v -> {
            if (isMyPost) {
                showStudentInquiriesDialog(skillTitle, authorName);
            } else {
                Intent chatIntent = new Intent(SkillDetailActivity.this, ChatActivity.class);
                chatIntent.putExtra("AUTHOR_NAME", authorName);
                chatIntent.putExtra("SKILL_TITLE", skillTitle);
                startActivity(chatIntent);
            }
        });

        // Rating & Peer Review Listener
        binding.btnRateSession.setOnClickListener(v -> showRatingDialog());

        // Load reviews from Cloud Firestore persistently
        loadReviewsFromFirestore();
    }

    private void checkRoomBookmarkStatus(String skillId) {
        new Thread(() -> {
            isBookmarked = AppDatabase.getInstance(this).savedSkillDao().isSkillSaved(skillId);
            runOnUiThread(() -> {
                if (isBookmarked) {
                    binding.btnSaveSkill.setText("Saved in Room DB (❤️)");
                } else {
                    binding.btnSaveSkill.setText("Bookmark Skill (Room DB ❤️)");
                }
            });
        }).start();
    }

    private void toggleRoomBookmark(String id, String title, String category, String description, String author, String location, String status, String imageUrl) {
        new Thread(() -> {
            SavedSkillDao dao = AppDatabase.getInstance(this).savedSkillDao();
            if (isBookmarked) {
                SavedSkillEntity entity = new SavedSkillEntity(id, title, category, description, author, location, status, imageUrl);
                dao.deleteSavedSkill(entity);
                isBookmarked = false;
                runOnUiThread(() -> {
                    binding.btnSaveSkill.setText("Bookmark Skill (Room DB ❤️)");
                    Toast.makeText(SkillDetailActivity.this, "Removed from Room DB Saved Skills", Toast.LENGTH_SHORT).show();
                });
            } else {
                SavedSkillEntity entity = new SavedSkillEntity(id, title, category, description, author, location, status, imageUrl);
                dao.insertSavedSkill(entity);
                isBookmarked = true;
                runOnUiThread(() -> {
                    binding.btnSaveSkill.setText("Saved in Room DB (❤️)");
                    Toast.makeText(SkillDetailActivity.this, "Skill saved to offline Room Database!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadReviewsFromFirestore() {
        if (skillTitle == null) return;
        String cleanSkill = skillTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.getDefault());

        try {
            FirebaseFirestore.getInstance()
                    .collection("skills")
                    .document(cleanSkill)
                    .collection("reviews")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            binding.layoutReviewsContainer.removeAllViews();
                            double totalPoints = 0.0;
                            int count = 0;

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Double ratingObj = doc.getDouble("ratingStars");
                                double stars = ratingObj != null ? ratingObj : 5.0;
                                String comment = doc.getString("comment");
                                String reviewerName = doc.getString("reviewerName");
                                String reviewerEmail = doc.getString("reviewerEmail");

                                if (reviewerEmail != null && reviewerEmail.equalsIgnoreCase(currentUserEmail)) {
                                    hasAlreadyReviewed = true;
                                    binding.btnRateSession.setText("Review Submitted (✓)");
                                }

                                totalPoints += stars;
                                count++;

                                renderReviewCard(stars, comment, reviewerName);
                            }

                            if (count > 0) {
                                double avg = totalPoints / count;
                                binding.tvRatingScore.setText(String.format(Locale.getDefault(), "⭐ %.1f", avg));
                                binding.tvReviewCount.setText(String.format(Locale.getDefault(), "(%d Peer Reviews)", count));
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading reviews from Firestore: " + e.getMessage());
        }
    }

    private void renderReviewCard(double stars, String comment, String reviewerName) {
        TextView reviewTv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 12, 0, 0);
        reviewTv.setLayoutParams(params);
        reviewTv.setBackgroundColor(Color.parseColor("#F0F4F8"));
        reviewTv.setPadding(24, 24, 24, 24);
        reviewTv.setTextColor(Color.parseColor("#333333"));
        reviewTv.setTextSize(13);

        String starSymbols;
        if (stars >= 5.0) starSymbols = "⭐⭐⭐⭐⭐";
        else if (stars >= 4.0) starSymbols = "⭐⭐⭐⭐";
        else if (stars >= 3.0) starSymbols = "⭐⭐⭐";
        else if (stars >= 2.0) starSymbols = "⭐⭐";
        else starSymbols = "⭐";

        String nameDisplay = reviewerName != null && !reviewerName.isEmpty() ? reviewerName : "Student";
        reviewTv.setText(String.format(Locale.getDefault(), "\"%s\" — %s (%s)", comment, nameDisplay, starSymbols));

        binding.layoutReviewsContainer.addView(reviewTv, 0);
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate Session & Write Peer Review");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        final String[] ratingOptions = {"⭐⭐⭐⭐⭐ (5.0 - Excellent)", "⭐⭐⭐⭐ (4.0 - Good)", "⭐⭐⭐ (3.0 - Average)", "⭐⭐ (2.0 - Poor)", "⭐ (1.0 - Terrible)"};
        final double[] ratingValues = {5.0, 4.0, 3.0, 2.0, 1.0};
        final int[] selectedRatingIndex = {0};

        builder.setSingleChoiceItems(ratingOptions, 0, (dialog, which) -> selectedRatingIndex[0] = which);

        final EditText etReview = new EditText(this);
        etReview.setHint("Write your feedback about this session...");
        layout.addView(etReview);
        builder.setView(layout);

        builder.setPositiveButton("Submit Review", (dialog, which) -> {
            String comment = etReview.getText().toString().trim();
            if (TextUtils.isEmpty(comment)) {
                comment = "Great skill session!";
            }

            double stars = ratingValues[selectedRatingIndex[0]];
            saveAndAddPeerReview(stars, comment);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveAndAddPeerReview(double ratingStars, String comment) {
        String reviewerName = "Student";
        if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
            reviewerName = currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : currentUserEmail;
            reviewerName = reviewerName.substring(0, 1).toUpperCase(Locale.getDefault()) + reviewerName.substring(1);
        }

        // Render card locally
        renderReviewCard(ratingStars, comment, reviewerName);

        hasAlreadyReviewed = true;
        binding.btnRateSession.setText("Review Submitted (✓)");

        String cleanSkill = skillTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.getDefault());
        String docId = currentUserEmail != null && !currentUserEmail.isEmpty() ? currentUserEmail.replaceAll("[^a-zA-Z0-9]", "_") : "review_" + System.currentTimeMillis();

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("reviewerEmail", currentUserEmail);
        reviewData.put("reviewerName", reviewerName);
        reviewData.put("ratingStars", ratingStars);
        reviewData.put("comment", comment);
        reviewData.put("timestamp", System.currentTimeMillis());

        try {
            FirebaseFirestore.getInstance()
                    .collection("skills")
                    .document(cleanSkill)
                    .collection("reviews")
                    .document(docId)
                    .set(reviewData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SkillDetailActivity.this, "Thank you! Your peer review is saved in Cloud Database.", Toast.LENGTH_SHORT).show();
                        loadReviewsFromFirestore();
                    })
                    .addOnFailureListener(e -> Toast.makeText(SkillDetailActivity.this, "Review saved locally.", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "Firestore review save exception: " + e.getMessage());
        }
    }

    private void showStudentInquiriesDialog(String skillTitle, String tutorName) {
        String cleanSkill = skillTitle != null ? skillTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.getDefault()) : "";
        String cleanTutor = tutorName != null ? tutorName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.getDefault()) : "";

        Toast.makeText(this, "Loading student inquiries from Cloud Firestore...", Toast.LENGTH_SHORT).show();

        try {
            FirebaseFirestore.getInstance().collection("chats")
                    .get()
                    .addOnCompleteListener(task -> {
                        List<String> studentHandles = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String docId = doc.getId().toLowerCase(Locale.getDefault());
                                if (docId.startsWith(cleanSkill)) {
                                    // Extract student handle from docId
                                    String[] parts = docId.split("_");
                                    for (String part : parts) {
                                        if (!part.isEmpty() && !cleanSkill.contains(part) && !cleanTutor.equalsIgnoreCase(part)) {
                                            String formatted = part.substring(0, 1).toUpperCase(Locale.getDefault()) + part.substring(1);
                                            if (!studentHandles.contains(formatted)) {
                                                studentHandles.add(formatted);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (studentHandles.isEmpty()) {
                            Toast.makeText(SkillDetailActivity.this, "No student inquiries received yet for this skill.", Toast.LENGTH_LONG).show();
                        } else {
                            String[] inquiries = new String[studentHandles.size()];
                            for (int i = 0; i < studentHandles.size(); i++) {
                                inquiries[i] = "👤 " + studentHandles.get(i);
                            }

                            new AlertDialog.Builder(SkillDetailActivity.this)
                                    .setTitle("Select Student Inquiry to Open Chat")
                                    .setItems(inquiries, (dialog, which) -> {
                                        String selectedStudentHandle = studentHandles.get(which);
                                        Intent chatIntent = new Intent(SkillDetailActivity.this, ChatActivity.class);
                                        chatIntent.putExtra("AUTHOR_NAME", selectedStudentHandle);
                                        chatIntent.putExtra("SKILL_TITLE", skillTitle);
                                        startActivity(chatIntent);
                                    })
                                    .setNegativeButton("Close", null)
                                    .show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Unable to query Firestore chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Demonstrates accessing device shared data using Android Content Providers.
     * Queries ContactsContract.CommonDataKinds.Phone ContentProvider URI.
     */
    private void queryContactContentProvider(Uri contactUri) {
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "Peer Student";
                String number = numberIndex >= 0 ? cursor.getString(numberIndex) : "";

                new AlertDialog.Builder(this)
                        .setTitle("Content Provider Result")
                        .setMessage("Queried shared device data via ContactsContract Content Provider:\n\n"
                                + "👤 Contact Name: " + name + "\n"
                                + "📞 Phone: " + number)
                        .setPositiveButton("Send SMS Invitation", (dialog, which) -> {
                            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                            smsIntent.setData(Uri.parse("sms:" + number));
                            smsIntent.putExtra("sms_body", "Hey " + name + "! Join me for a SkillSwap session: " + binding.tvDetailTitle.getText());
                            startActivity(smsIntent);
                        })
                        .setNegativeButton("Close", null)
                        .show();
            } else {
                Toast.makeText(this, "No details retrieved from Content Provider.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Content Provider query failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
