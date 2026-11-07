package com.example.skillswap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skillswap.databinding.ItemSkillCardBinding;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    private final List<SkillPost> fullSkillList;
    private final List<SkillPost> displayedSkillList;
    private final OnItemClickListener listener;
    private String currentQuery = "";
    private String currentCategory = "All";

    public interface OnItemClickListener {
        void onItemClick(SkillPost skill);
    }

    public SkillAdapter(List<SkillPost> skillList, OnItemClickListener listener) {
        this.fullSkillList = new ArrayList<>(skillList != null ? skillList : new ArrayList<>());
        this.displayedSkillList = new ArrayList<>(this.fullSkillList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSkillCardBinding binding = ItemSkillCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SkillViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        if (position < 0 || position >= displayedSkillList.size()) return;
        SkillPost skill = displayedSkillList.get(position);
        if (skill == null) return;

        holder.binding.tvTitle.setText(skill.getTitle() != null ? skill.getTitle() : "Untitled Skill");
        holder.binding.tvCategory.setText(skill.getCategory() != null ? skill.getCategory() : "General");
        holder.binding.tvDescription.setText(skill.getDescription() != null ? skill.getDescription() : "");
        holder.binding.tvAuthor.setText("By: " + (skill.getAuthorName() != null ? skill.getAuthorName() : "Student"));
        holder.binding.tvLocation.setText(skill.getLocation() != null ? skill.getLocation() : "Campus");
        holder.binding.tvStatus.setText(skill.getStatus() != null ? skill.getStatus() : "Available");

        loadImageIntoView(holder.binding.imgSkillCard, skill.getImageUrl());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(skill);
            }
        });
    }

    public static void loadImageIntoView(ImageView imageView, String urlStr) {
        if (imageView == null) return;

        if (urlStr == null || urlStr.trim().isEmpty()) {
            imageView.setVisibility(View.GONE);
            return;
        }

        if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
            new Thread(() -> {
                try {
                    InputStream input = new URL(urlStr).openStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                        } else {
                            imageView.setVisibility(View.GONE);
                        }
                    });
                } catch (Throwable e) {
                    new Handler(Looper.getMainLooper()).post(() -> imageView.setVisibility(View.GONE));
                }
            }).start();
        } else {
            final Context appContext = imageView.getContext().getApplicationContext();
            new Thread(() -> {
                try {
                    Uri uri = Uri.parse(urlStr);
                    InputStream input = appContext.getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                        } else {
                            imageView.setVisibility(View.GONE);
                        }
                    });
                } catch (Throwable e) {
                    new Handler(Looper.getMainLooper()).post(() -> imageView.setVisibility(View.GONE));
                }
            }).start();
        }
    }

    @Override
    public int getItemCount() {
        return displayedSkillList.size();
    }

    public void updateList(List<SkillPost> newSkills) {
        fullSkillList.clear();
        if (newSkills != null) {
            fullSkillList.addAll(newSkills);
        }
        applyFilter();
    }

    public void filter(String query, String category) {
        this.currentQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        if (category != null) {
            this.currentCategory = category;
        }
        applyFilter();
    }

    private void applyFilter() {
        displayedSkillList.clear();

        for (SkillPost skill : fullSkillList) {
            if (skill == null) continue;
            boolean matchesCategory = currentCategory.equalsIgnoreCase("All")
                    || (skill.getCategory() != null && skill.getCategory().equalsIgnoreCase(currentCategory));

            boolean matchesQuery = currentQuery.isEmpty()
                    || (skill.getTitle() != null && skill.getTitle().toLowerCase(Locale.getDefault()).contains(currentQuery))
                    || (skill.getCategory() != null && skill.getCategory().toLowerCase(Locale.getDefault()).contains(currentQuery))
                    || (skill.getDescription() != null && skill.getDescription().toLowerCase(Locale.getDefault()).contains(currentQuery))
                    || (skill.getAuthorName() != null && skill.getAuthorName().toLowerCase(Locale.getDefault()).contains(currentQuery))
                    || (skill.getLocation() != null && skill.getLocation().toLowerCase(Locale.getDefault()).contains(currentQuery));

            if (matchesCategory && matchesQuery) {
                displayedSkillList.add(skill);
            }
        }
        notifyDataSetChanged();
    }

    public static class SkillViewHolder extends RecyclerView.ViewHolder {
        private final ItemSkillCardBinding binding;

        public SkillViewHolder(ItemSkillCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
