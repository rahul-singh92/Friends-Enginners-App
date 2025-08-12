package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AdminActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin); // Ensure this matches your XML filename

        // Set up the custom toolbar
        Toolbar toolbar = findViewById(R.id.admin_toolbar);
        setSupportActionBar(toolbar);

        // Find the button and set a click listener
        Button uploadButton = findViewById(R.id.admin_button);
        uploadButton.setOnClickListener(v -> {
            UploadExcelBottomSheet bottomSheet = new UploadExcelBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "UploadExcelBottomSheet");
        });

        Button viewDetailsButton = findViewById(R.id.admin_button2);
        viewDetailsButton.setOnClickListener(v -> {
            // Launch the TableListActivity to show available data tables
            Intent intent = new Intent(AdminActivity.this, TableListActivity.class);
            startActivity(intent);
        });

        Button uploadPdfButton = findViewById(R.id.admin_button_pdf);
        uploadPdfButton.setOnClickListener(v -> {
            UploadPdfBottomSheet pdfBottomSheet = new UploadPdfBottomSheet();
            pdfBottomSheet.show(getSupportFragmentManager(), "UploadPdfBottomSheet");
        });

        Button viewPdfButton = findViewById(R.id.view_pdf_button);
        viewPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, WageCollectionsActivity.class);
            startActivity(intent);
        });

        Button advanceRequestButton = findViewById(R.id.advance_request_button);
        advanceRequestButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdvanceRequestActivity.class);
            startActivity(intent);
        });

        //Total Advance Button
        Button totalAdvanceButton = findViewById(R.id.total_advance_button);
        totalAdvanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, TotalAdvanceActivity.class);
            startActivity(intent);
        });

        //Worker Schedule
        Button uploadScheduleButton = findViewById(R.id.worker_schedule_upload_button);
        uploadScheduleButton.setOnClickListener(v -> {
            UploadScheduleBottomSheet bottomSheet = new UploadScheduleBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "WorkerScheduleBottomSheet");
        });

        // View Worker Schedule
        findViewById(R.id.view_schedule_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, BranchListActivity.class);
            startActivity(intent);
        });

        // Upload Credentials button
        Button uploadCredentialButton = findViewById(R.id.upload_credentials_button);
        uploadCredentialButton.setOnClickListener(v -> {
            UploadCredentialsBottomSheet bottomSheet = new UploadCredentialsBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "CredentialBottomScheet");
        });

        Button viewCredentialButton = findViewById(R.id.view_credentials_button);
        viewCredentialButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, CredentialsTablesActivity.class);
            startActivity(intent);
        });

        Button enterWorkerDetails = findViewById(R.id.enter_worker_details_button);
        enterWorkerDetails.setOnClickListener(v -> {
            WorkerDetailBottomSheet bottomSheet = new WorkerDetailBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "WorkerDetailBottomScheet");
        });

        // View Worker Details
        findViewById(R.id.view_worker_details_button).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewWorkerDetailsActivity.class));
        });

    }
}