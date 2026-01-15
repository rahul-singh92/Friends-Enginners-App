package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class DataDetailActivity extends BaseActivity {

    private static final String TAG = "DataDetailActivity";
    private String collectionName;
    private String documentId;
    private LinearLayout detailContainer;
    private LinearLayout backButton;
    private TextView headerSubtitle;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_detail);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        collectionName = getIntent().getStringExtra("COLLECTION_NAME");
        documentId = getIntent().getStringExtra("DOCUMENT_ID");

        if (collectionName == null || documentId == null || collectionName.isEmpty() || documentId.isEmpty()) {
            Toast.makeText(this, "Invalid parameters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        detailContainer = findViewById(R.id.detail_container);
        backButton = findViewById(R.id.backButton);
        headerSubtitle = findViewById(R.id.headerSubtitle);

        // Set document ID in header
        headerSubtitle.setText("ID: " + documentId);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();

        loadDocumentDetails();
    }

    private void loadDocumentDetails() {
        firestore.collection(collectionName).document(documentId)
                .get()
                .addOnSuccessListener(this::displayDocumentFields)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load document: ", e);
                    Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayDocumentFields(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "Document does not exist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Map<String, Object> fields = document.getData();
        if (fields == null || fields.isEmpty()) {
            Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        detailContainer.removeAllViews();

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            View itemView = inflater.inflate(R.layout.item_field_detail, detailContainer, false);

            TextView labelView = itemView.findViewById(R.id.field_label);
            TextView valueView = itemView.findViewById(R.id.field_value);

            labelView.setText(formatFieldName(entry.getKey()));

            // Format value with proper null handling
            Object value = entry.getValue();
            String valueText = (value != null) ? String.valueOf(value) : "N/A";
            valueView.setText(valueText);

            detailContainer.addView(itemView);
        }
    }

    private String formatFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "Unknown";
        }

        // Replace underscores with spaces & capitalize words
        String formatted = fieldName.replace('_', ' ');
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (char c : formatted.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                capitalize = true;
                result.append(c);
            } else if (capitalize) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
