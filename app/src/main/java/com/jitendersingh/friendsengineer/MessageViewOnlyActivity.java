package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.util.Log;
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
import java.util.List;

public class MessageViewOnlyActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout, backButton;
    private FirebaseFirestore db;
    private ListenerRegistration messagesListener;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_viewonly);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_messages);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
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

        listenForMessages();
    }

    private void listenForMessages() {
        messagesListener = db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("MessageViewOnly", "Listen failed", e);
                        Toast.makeText(this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        Log.d("MessageViewOnly", "Received " + queryDocumentSnapshots.size() + " messages");

                        // Clear the list
                        messageList.clear();

                        // Get all documents
                        queryDocumentSnapshots.forEach(doc -> {
                            try {
                                ChatMessage message = doc.toObject(ChatMessage.class);

                                if (message != null && message.getText() != null && !message.getText().isEmpty()) {
                                    messageList.add(message);
                                }
                            } catch (Exception ex) {
                                Log.e("MessageViewOnly", "Error parsing message", ex);
                            }
                        });

                        Log.d("MessageViewOnly", "Valid messages: " + messageList.size());

                        // Show/hide empty state
                        if (messageList.isEmpty()) {
                            emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                            recyclerView.setVisibility(RecyclerView.GONE);
                        } else {
                            emptyStateLayout.setVisibility(LinearLayout.GONE);
                            recyclerView.setVisibility(RecyclerView.VISIBLE);

                            // Update adapter
                            chatAdapter.updateMessages(messageList);

                            // Scroll to bottom
                            if (chatAdapter.getItemCount() > 0) {
                                recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}
