package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class WorkerDetailActivity extends AppCompatActivity {

    ImageView workerImage;
    TextView detailText;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_detail);

        workerImage = findViewById(R.id.worker_image);
        detailText = findViewById(R.id.worker_detail_text);
        firestore = FirebaseFirestore.getInstance();

        String documentId = getIntent().getStringExtra("document_id");

        if (documentId != null && !documentId.isEmpty()) {
            loadWorkerDetailFromFirestore(documentId);
        } else {
            Toast.makeText(this, "Invalid document ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadWorkerDetailFromFirestore(String documentId) {
        firestore.collection("Worker_Detail")
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        StringBuilder details = new StringBuilder();

                        details.append("P. No.: ").append(documentSnapshot.getString("PNo")).append("\n");
                        details.append("ESI No.: ").append(documentSnapshot.getString("ESINo")).append("\n");
                        details.append("UAN No.: ").append(documentSnapshot.getString("UANNo")).append("\n");
                        details.append("PF No.: ").append(documentSnapshot.getString("PFNo")).append("\n");
                        details.append("Name: ").append(documentSnapshot.getString("Name")).append("\n");
                        details.append("Father's Name: ").append(documentSnapshot.getString("FatherName")).append("\n");
                        details.append("Contact: ").append(documentSnapshot.getString("ContactNumber")).append("\n");
                        details.append("DOJ: ").append(documentSnapshot.getString("DateOfJoining")).append("\n");
                        details.append("Department: ").append(documentSnapshot.getString("Department")).append("\n");
                        details.append("Date of Leave: ").append(documentSnapshot.getString("DateOfLeave")).append("\n");

                        detailText.setText(details.toString());

                        // Load image from ImageUrl if it exists
                        String imageUrl = documentSnapshot.getString("ImageURL");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(workerImage);
                        } else {
                            Toast.makeText(this, "No image found for this worker", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Worker data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading worker detail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
