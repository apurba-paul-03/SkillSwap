package com.example.skillswap;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skillswap.databinding.ActivitySkillDetailBinding;

public class SkillDetailActivity extends AppCompatActivity {

    private ActivitySkillDetailBinding binding;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

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

        binding.tvDetailTitle.setText(title);
        binding.tvDetailCategory.setText(category);
        binding.tvDetailDescription.setText(description);
        binding.tvDetailAuthor.setText("By: " + author);
        binding.tvDetailLocation.setText(location);
        binding.tvDetailStatus.setText(status);

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
