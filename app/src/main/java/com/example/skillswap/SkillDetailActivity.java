package com.example.skillswap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skillswap.databinding.ActivitySkillDetailBinding;

public class SkillDetailActivity extends AppCompatActivity {

    private ActivitySkillDetailBinding binding;

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
    }
}