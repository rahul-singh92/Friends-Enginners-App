package com.jitendersingh.friendsengineer;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WageSlipDetailActivity extends BaseActivity {

    private LinearLayout backButton;
    private LinearLayout emptyStateLayout;

    private TextView headingText;
    private TextView txtName, txtFather, txtId;
    private TextView btnOpenPdf;
    private TextView btnDownloadPdf;
    private TextView btnSharePdf;

    private FirebaseFirestore firestore;
    private DocumentSnapshot salaryDocument;
    private File generatedPdfFile;

    private String workerId;
    private String branch;
    private String month;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wage_slip_detail);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        headingText = findViewById(R.id.headingText);

        txtName = findViewById(R.id.txtName);
        txtFather = findViewById(R.id.txtFather);
        txtId = findViewById(R.id.txtId);
        btnOpenPdf = findViewById(R.id.btnOpenPdf);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);

        backButton.setOnClickListener(v -> finish());

        branch = getIntent().getStringExtra("branch");
        month = getIntent().getStringExtra("month");
        workerId = getIntent().getStringExtra("workerId");

        if (branch == null || month == null || workerId == null) {
            showEmpty();
            return;
        }

        headingText.setText(branch + "-" + month);

        fetchWorkerWage();
    }

    private void fetchWorkerWage() {
        firestore.collection("salary_data")
                .document(branch)
                .collection("salary_months")
                .document(month)
                .collection("employees")
                .document(workerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showEmpty();
                        return;
                    }

                    showData(documentSnapshot);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void showData(DocumentSnapshot doc) {
        salaryDocument = doc;

        emptyStateLayout.setVisibility(View.GONE);

        Object idObj = doc.get("id");
        String id = idObj != null ? String.valueOf(idObj) : workerId;

        String name = doc.getString("name");
        String fatherName = doc.getString("fatherName");

        txtId.setText(id);
        txtName.setText(name != null ? name : "Unknown");
        txtFather.setText(fatherName != null ? fatherName : "Unknown");

        btnOpenPdf.setOnClickListener(v -> {
            if (ensurePdfGenerated()) {
                openPdf();
            }
        });

        btnDownloadPdf.setOnClickListener(v -> {
            if (ensurePdfGenerated()) {
                downloadPdf();
            }
        });

        btnSharePdf.setOnClickListener(v -> {
            if (ensurePdfGenerated()) {
                sharePdf();
            }
        });
    }

    /**
     * Generates the PDF into cache (if not already generated for this view).
     * Returns true if a valid PDF file is ready to use.
     */
    private boolean ensurePdfGenerated() {
        if (generatedPdfFile != null && generatedPdfFile.exists()) {
            return true;
        }
        return generateSalarySlip();
    }

    private boolean generateSalarySlip() {

        if (salaryDocument == null) {
            Toast.makeText(this,
                    "Salary data not loaded",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        try {

            SalarySlipPdfGenerator.SalaryData data =
                    new SalarySlipPdfGenerator.SalaryData();
            data.mon2 = month;

            String[] parts = month.split("_");

            if (parts.length == 2) {
                data.mon1 = parts[0];
                data.mon2 = parts[0];
                data.year = parts[1];
            }

            data.name = salaryDocument.getString("name");
            data.fatherName = salaryDocument.getString("fatherName");
            data.designation = salaryDocument.getString("designation");
            data.department = salaryDocument.getString("department");
            data.doj = salaryDocument.getString("doj");

            data.punchingNo = salaryDocument.getString("punchingNo");
            data.pfNo = salaryDocument.getString("pfNo");
            data.esiNo = salaryDocument.getString("esiNo");
            data.uanNo = salaryDocument.getString("uanNo");
            data.tpd = salaryDocument.getString("tpd");
            data.nodw = salaryDocument.getString("nodw");
            data.wO = salaryDocument.getString("wo");
            data.holiday = salaryDocument.getString("holiday");
            data.otH = salaryDocument.getString("otHours");
            data.basic = salaryDocument.getString("basic");
            data.hra = salaryDocument.getString("hra");
            data.convenience = salaryDocument.getString("conveyance");
            data.cl = salaryDocument.getString("cl");
            data.pl = salaryDocument.getString("pl");
            data.bonus = salaryDocument.getString("bonus");
            data.gross = salaryDocument.getString("gross");
            data.basicE = salaryDocument.getString("basicEarned");
            data.hraE = salaryDocument.getString("hraEarned");
            data.convenieceE = salaryDocument.getString("conveyanceEarned");
            data.otE = salaryDocument.getString("otEarned");
            data.clE = salaryDocument.getString("clEarned");
            data.plE = salaryDocument.getString("plEarned");
            data.bonusE = salaryDocument.getString("bonusEarned");
            data.totalEarning = salaryDocument.getString("totalEarning");
            data.pfD = salaryDocument.getString("pfDeduction");
            data.esiD = salaryDocument.getString("esiDeduction");
            data.oteD = salaryDocument.getString("otDeduction");
            data.advanceD = salaryDocument.getString("advanceDeduction");
            data.tea = salaryDocument.getString("tea");
            data.canteen = salaryDocument.getString("canteen");
            data.totalDeduction = salaryDocument.getString("totalDeduction");
            data.netSalary = salaryDocument.getString("netSalary");
            data.netSalaryWords = salaryDocument.getString("netSalaryWords");

            data.bankName = salaryDocument.getString("bankName");
            data.accountNo = salaryDocument.getString("accountNo");

            generatedPdfFile = new File(
                    getCacheDir(),
                    workerId + "_" + month + ".pdf"
            );

            SalarySlipPdfGenerator generator =
                    new SalarySlipPdfGenerator(this);

            generator.generate(generatedPdfFile, data);

            return true;

        } catch (Exception e) {
            Toast.makeText(this,
                    e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private Uri getPdfUri() {
        return FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                generatedPdfFile
        );
    }

    private void openPdf() {
        Uri pdfUri = getPdfUri();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, "Open Salary Slip"));
        } else {
            Toast.makeText(this, "No PDF Viewer installed", Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdf() {
        Uri pdfUri = getPdfUri();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Salary Slip"));
    }

    /**
     * Saves a copy of the generated PDF into the public Downloads folder.
     * Uses MediaStore (API 29+) so NO storage permission is required.
     */
    private void downloadPdf() {
        String fileName = workerId + "_" + month + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                Uri itemUri = getContentResolver().insert(collection, values);

                if (itemUri == null) {
                    Toast.makeText(this, "Unable to create download entry", Toast.LENGTH_LONG).show();
                    return;
                }

                try (OutputStream out = getContentResolver().openOutputStream(itemUri);
                     FileInputStream in = new FileInputStream(generatedPdfFile)) {

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(itemUri, values, null, null);

                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();

            } else {
                // Fallback for API < 29 (pre scoped-storage devices).
                // Only needed if your minSdk targets these versions.
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File outFile = new File(downloadsDir, fileName);

                try (FileInputStream in = new FileInputStream(generatedPdfFile);
                     FileOutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }

                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showEmpty() {
        emptyStateLayout.setVisibility(View.VISIBLE);
    }
}