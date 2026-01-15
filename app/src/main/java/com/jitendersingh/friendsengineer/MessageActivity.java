package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private EditText inputMessage;
    private LinearLayout sendButton, backButton;

    private FirebaseFirestore db;
    private ListenerRegistration messagesListener;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_messages);
        inputMessage = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.button_send);
        backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        // Send button handler
        sendButton.setOnClickListener(v -> sendMessage());

        listenForMessages();
    }

    private void listenForMessages() {
        messagesListener = db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        // Clear the list
                        messageList.clear();

                        // Get all documents (not just changes)
                        queryDocumentSnapshots.forEach(doc -> {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null && message.getText() != null) {
                                messageList.add(message);
                            }
                        });

                        // Update adapter
                        chatAdapter.updateMessages(messageList);

                        // Scroll to bottom
                        if (chatAdapter.getItemCount() > 0) {
                            recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Please enter message", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("text", text);
        messageMap.put("timestamp", System.currentTimeMillis());

        db.collection("messages")
                .add(messageMap)
                .addOnSuccessListener(documentReference -> {
                    inputMessage.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null)
            messagesListener.remove();
    }
}