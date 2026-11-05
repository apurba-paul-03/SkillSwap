package com.example.skillswap;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
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
import java.util.List;
import java.util.Locale;

public class SkillDetailActivity extends AppCompatActivity {

    private ActivitySkillDetailBinding binding;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

    private double totalRatingPoints = 9.6;
    private int reviewCount = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySkillDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String title = getIntent().getStringExtra("TITLE");
        String category = getIntent().getStringExtra("CATEGORY");
        String description = getIntent().getStringExtra("DESC");
        String author = getIntent().getStringExtra("AUTHOR");
        String location = getIntent().getStringExtra("LOC");
        String status = getIntent().getStringExtra("STATUS");
        String currentUserEmail = getIntent().getStringExtra("CURRENT_USER_EMAIL");

        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    currentUserEmail = user.getEmail();
                }
            } catch (Exception ignored) {}
        }

        binding.tvDetailTitle.setText(title);
        binding.tvDetailCategory.setText(category);
        binding.tvDetailDescription.setText(description);
        binding.tvDetailAuthor.setText("By: " + author);
        binding.tvDetailLocation.setText(location);
        binding.tvDetailStatus.setText(status);

        String currentHandle = currentUserEmail != null && currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : currentUserEmail;
        boolean isMyPost = author != null && (author.equalsIgnoreCase(currentHandle) || author.equalsIgnoreCase(currentUserEmail));

        if (isMyPost) {
            binding.btnChatTutor.setText("View Student Inquiries & Chat");
        } else {
            binding.btnChatTutor.setText("Chat with Tutor (1-on-1)");
        }

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
                showStudentInquiriesDialog(title, author);
            } else {
                Intent chatIntent = new Intent(SkillDetailActivity.this, ChatActivity.class);
                chatIntent.putExtra("AUTHOR_NAME", author);
                chatIntent.putExtra("SKILL_TITLE", title);
                startActivity(chatIntent);
            }
        });

        // Rating & Peer Review Listener
        binding.btnRateSession.setOnClickListener(v -> showRatingDialog());
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
            addPeerReview(stars, comment);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addPeerReview(double ratingStars, String comment) {
        totalRatingPoints += ratingStars;
        reviewCount++;

        double avgRating = totalRatingPoints / reviewCount;
        binding.tvRatingScore.setText(String.format(Locale.getDefault(), "⭐ %.1f", avgRating));
        binding.tvReviewCount.setText(String.format(Locale.getDefault(), "(%d Peer Reviews)", reviewCount));

        // Get reviewer name from logged in user or fallback
        String reviewerName = "Student";
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                reviewerName = user.getEmail().split("@")[0];
            }
        } catch (Exception ignored) {}

        // Create new review view dynamically
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

        String starSymbols = ratingStars >= 5 ? "⭐⭐⭐⭐⭐" : ratingStars >= 4 ? "⭐⭐⭐⭐" : "⭐⭐⭐";
        reviewTv.setText(String.format(Locale.getDefault(), "\"%s\" — %s (%s)", comment, reviewerName, starSymbols));

        binding.layoutReviewsContainer.addView(reviewTv, 0);

        Toast.makeText(this, "Thank you! Your peer review has been recorded.", Toast.LENGTH_SHORT).show();
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
