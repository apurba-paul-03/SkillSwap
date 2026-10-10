package com.example.skillswap;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skillswap.databinding.ItemSkillCardBinding;
import java.util.List;

public class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    private final List<SkillPost> skillList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SkillPost skill);
    }

    public SkillAdapter(List<SkillPost> skillList, OnItemClickListener listener) {
        this.skillList = skillList;
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
        SkillPost skill = skillList.get(position);
        holder.binding.tvTitle.setText(skill.getTitle());
        holder.binding.tvCategory.setText(skill.getCategory());
        holder.binding.tvDescription.setText(skill.getDescription());
        holder.binding.tvAuthor.setText("By: " + skill.getAuthorName());
        holder.binding.tvLocation.setText(skill.getLocation());
        holder.binding.tvStatus.setText(skill.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(skill);
            }
        });
    }

    @Override
    public int getItemCount() {
        return skillList.size();
    }

    public static class SkillViewHolder extends RecyclerView.ViewHolder {
        private final ItemSkillCardBinding binding;

        public SkillViewHolder(ItemSkillCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}