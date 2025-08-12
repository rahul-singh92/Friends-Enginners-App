package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log; // <-- Added for logging
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WorkerDetailsActivity extends Activity {

    String name, fatherName;

    TextView textPNo, textESINo, textUANNo, textPFNo, textName, textFatherName, textContact, textDOJ, textDepartment, textDOL;
    ImageView imageView;

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_details);

        name = getIntent().getStringExtra("name");
        fatherName = getIntent().getStringExtra("father_name");

        firestore = FirebaseFirestore.getInstance();

        textPNo = findViewById(R.id.textPNo);
        textESINo = findViewById(R.id.textESINo);
        textUANNo = findViewById(R.id.textUANNo);
        textPFNo = findViewById(R.id.textPFNo);
        textName = findViewById(R.id.textName);
        textFatherName = findViewById(R.id.textFatherName);
        textContact = findViewById(R.id.textContact);
        textDOJ = findViewById(R.id.textDOJ);
        textDepartment = findViewById(R.id.textDepartment);
        textDOL = findViewById(R.id.textDOL);
        imageView = findViewById(R.id.workerImage);

        loadWorkerDetailsFromFirestore();
    }

    private void loadWorkerDetailsFromFirestore() {
        firestore.collection("Worker_Detail")
                .whereEqualTo("Name", name)
                .whereEqualTo("FatherName", fatherName)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        // 🔍 Debug logs
                        Log.d("WorkerDetailsDebug", "Fetched Document ID: " + doc.getId());
                        Log.d("WorkerDetailsDebug", "Fetched Name: " + doc.getString("Name"));
                        Log.d("WorkerDetailsDebug", "Fetched FatherName: " + doc.getString("FatherName"));
                        Log.d("WorkerDetailsDebug", "Fetched ImageURL: " + doc.getString("ImageURL"));

                        textPNo.setText(doc.getString("PNo"));
                        textESINo.setText(doc.getString("ESINo"));
                        textUANNo.setText(doc.getString("UANNo"));
                        textPFNo.setText(doc.getString("PFNo"));
                        textName.setText(doc.getString("Name"));
                        textFatherName.setText(doc.getString("FatherName"));
                        textContact.setText(doc.getString("ContactNumber"));
                        textDOJ.setText(doc.getString("DateOfJoining"));
                        textDepartment.setText(doc.getString("Department"));
                        textDOL.setText(doc.getString("DateOfLeave"));

                        // Load image from ImageUrl
                        String imageUrl = doc.getString("ImageURL");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .into(imageView);
                        } else {
                            Toast.makeText(this, "No image URL found", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Worker not found in Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
