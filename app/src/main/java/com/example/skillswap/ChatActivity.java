package com.example.skillswap;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerViewChat;
    private EditText etChatMessage;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private String authorName;
    private String skillTitle;
    private String currentUserEmail = "Student";
    private String chatId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        authorName = getIntent().getStringExtra("AUTHOR_NAME");
        if (authorName == null || authorName.trim().isEmpty()) {
            authorName = "Tutor";
        }
        skillTitle = getIntent().getStringExtra("SKILL_TITLE");
        if (skillTitle == null || skillTitle.trim().isEmpty()) {
            skillTitle = "Skill Swap Session";
        }

        TextView tvHeader = findViewById(R.id.tvChatHeader);
        TextView tvSubheader = findViewById(R.id.tvChatSubheader);
        tvHeader.setText("1-on-1 Chat with " + authorName);
        tvSubheader.setText("Session: " + skillTitle);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Private Chat: " + authorName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                currentUserEmail = user.getEmail();
            }
        } catch (Exception ignored) {}

        // Compute 1-on-1 private chat room ID using sorted learner & tutor pair
        String cleanUser = currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : currentUserEmail;
        String userA = cleanUser.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String userB = authorName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String userPair = userA.compareTo(userB) < 0 ? userA + "_" + userB : userB + "_" + userA;
        chatId = skillTitle.replaceAll("[^a-zA-Z0-9]", "_") + "_" + userPair;

        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firestore initialization exception: " + e.getMessage());
            db = null;
        }

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etChatMessage = findViewById(R.id.etChatMessage);
        Button btnSend = findViewById(R.id.btnSend);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserEmail);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);

        // Listen for real-time messages from Firebase Firestore
        listenForRealtimeMessages();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void listenForRealtimeMessages() {
        if (db == null) return;
        try {
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Firestore listen error", error);
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            messageList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                ChatMessage msg = doc.toObject(ChatMessage.class);
                                messageList.add(msg);
                            }
                            chatAdapter.notifyDataSetChanged();
                            recyclerViewChat.scrollToPosition(messageList.size() - 1);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Firestore Realtime listener exception", e);
        }
    }

    private void sendMessage() {
        String text = etChatMessage.getText() != null ? etChatMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        ChatMessage newMsg = new ChatMessage(
                currentUserEmail,
                currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : currentUserEmail,
                text,
                System.currentTimeMillis()
        );

        // Instantly display message locally in 1-on-1 thread
        messageList.add(newMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerViewChat.scrollToPosition(messageList.size() - 1);
        etChatMessage.setText("");

        // Push to Firebase Firestore in real-time
        if (db != null) {
            try {
                // Ensure parent chat room document exists for tutor inquiry discovery
                Map<String, Object> chatMeta = new HashMap<>();
                chatMeta.put("skillTitle", skillTitle);
                chatMeta.put("lastMessage", text);
                chatMeta.put("lastUpdated", System.currentTimeMillis());
                db.collection("chats").document(chatId).set(chatMeta);

                db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .add(newMsg)
                        .addOnSuccessListener(doc -> Log.d(TAG, "1-on-1 Message sent to Firestore successfully."))
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore send message failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore send Exception", e);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
