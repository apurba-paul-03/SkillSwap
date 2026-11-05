package com.example.skillswap;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skillswap.databinding.ItemChatBubbleBinding;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messageList;
    private final String currentUserEmail;

    public ChatAdapter(List<ChatMessage> messageList, String currentUserEmail) {
        this.messageList = messageList;
        this.currentUserEmail = currentUserEmail != null ? currentUserEmail : "";
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBubbleBinding binding = ItemChatBubbleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.binding.tvSenderName.setText(message.getSenderName());
        holder.binding.tvMessageText.setText(message.getText());

        boolean isMe = currentUserEmail.equalsIgnoreCase(message.getSenderEmail())
                || currentUserEmail.equalsIgnoreCase(message.getSenderName());

        if (isMe) {
            holder.binding.layoutChatContainer.setGravity(Gravity.END);
            holder.binding.cardBubble.setCardBackgroundColor(Color.parseColor("#007AFF"));
            holder.binding.tvMessageText.setTextColor(Color.WHITE);
            holder.binding.tvSenderName.setText("You");
        } else {
            holder.binding.layoutChatContainer.setGravity(Gravity.START);
            holder.binding.cardBubble.setCardBackgroundColor(Color.parseColor("#E9ECEF"));
            holder.binding.tvMessageText.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatBubbleBinding binding;

        public ChatViewHolder(ItemChatBubbleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
