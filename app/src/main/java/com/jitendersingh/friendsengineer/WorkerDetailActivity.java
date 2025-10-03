package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class WorkerDetailActivity extends AppCompatActivity {

    private ImageView workerImage;
    private TextView headerName, headerDepartment;
    private TextView workerNameLarge, workerName, workerFatherName, workerContact;
    private TextView workerPNo, workerESI, workerUAN, workerPF;
    private TextView workerDepartment, workerDOJ, workerDOL;
    private LinearLayout backButton;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_detail);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        workerImage = findViewById(R.id.worker_image);
        headerName = findViewById(R.id.headerName);
        headerDepartment = findViewById(R.id.headerDepartment);
        workerNameLarge = findViewById(R.id.workerNameLarge);
        workerName = findViewById(R.id.workerName);
        workerFatherName = findViewById(R.id.workerFatherName);
        workerContact = findViewById(R.id.workerContact);
        workerPNo = findViewById(R.id.workerPNo);
        workerESI = findViewById(R.id.workerESI);
        workerUAN = findViewById(R.id.workerUAN);
        workerPF = findViewById(R.id.workerPF);
        workerDepartment = findViewById(R.id.workerDepartment);
        workerDOJ = findViewById(R.id.workerDOJ);
        workerDOL = findViewById(R.id.workerDOL);
        backButton = findViewById(R.id.backButton);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

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
                        // Get all fields
                        String name = documentSnapshot.getString("Name");
                        String fatherName = documentSnapshot.getString("FatherName");
                        String contact = documentSnapshot.getString("ContactNumber");
                        String pNo = documentSnapshot.getString("PNo");
                        String esi = documentSnapshot.getString("ESINo");
                        String uan = documentSnapshot.getString("UANNo");
                        String pf = documentSnapshot.getString("PFNo");
                        String department = documentSnapshot.getString("Department");
                        String doj = documentSnapshot.getString("DateOfJoining");
                        String dol = documentSnapshot.getString("DateOfLeave");
                        String imageUrl = documentSnapshot.getString("ImageURL");

                        // Set header
                        headerName.setText(name != null ? name : "Worker Profile");
                        headerDepartment.setText(department != null ? department : "Department");

                        // Set large name
                        workerNameLarge.setText(name != null ? name : "N/A");

                        // Set personal information
                        workerName.setText(name != null ? name : "N/A");
                        workerFatherName.setText(fatherName != null ? fatherName : "N/A");
                        workerContact.setText(contact != null ? contact : "N/A");

                        // Set employment information
                        workerPNo.setText(pNo != null ? pNo : "N/A");
                        workerESI.setText(esi != null ? esi : "N/A");
                        workerUAN.setText(uan != null ? uan : "N/A");
                        workerPF.setText(pf != null ? pf : "N/A");
                        workerDepartment.setText(department != null ? department : "N/A");
                        workerDOJ.setText(doj != null ? doj : "N/A");
                        workerDOL.setText((dol != null && !dol.isEmpty()) ? dol : "Not specified");

                        // Load image
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .into(workerImage);
                        } else {
                            workerImage.setImageResource(R.drawable.baseline_person_24);
                        }

                    } else {
                        Toast.makeText(this, "Worker data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading worker detail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}
