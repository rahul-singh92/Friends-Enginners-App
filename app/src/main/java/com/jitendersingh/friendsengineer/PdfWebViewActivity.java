package com.jitendersingh.friendsengineer;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfWebViewActivity extends AppCompatActivity {
    private static final String TAG = "ViewPdfPageActivity";

    private String pdfUrl;
    private int pdfPage;
    private String personName;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pdfUrl = getIntent().getStringExtra("pdfUrl");
        pdfPage = getIntent().getIntExtra("pdfPage", 1);
        personName = getIntent().getStringExtra("personName");

        if (pdfUrl == null) {
            Toast.makeText(this, "PDF URL missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "PDF URL: " + pdfUrl + ", Page: " + pdfPage);

        PDFBoxResourceLoader.init(getApplicationContext());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Preparing wage slip...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(this::downloadAndOpenPdfPage).start();
    }

    private void downloadAndOpenPdfPage() {
        try {
            Log.d(TAG, "Downloading PDF from: " + pdfUrl);
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP Response: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP error: " + responseCode);
            }

            InputStream inputStream = connection.getInputStream();
            PDDocument document = PDDocument.load(inputStream);
            int totalPages = document.getNumberOfPages();
            Log.d(TAG, "PDF loaded. Total pages: " + totalPages);

            if (pdfPage > totalPages || pdfPage < 1) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Page not found in PDF", Toast.LENGTH_SHORT).show();
                    finish();
                });
                document.close();
                return;
            }

            PDDocument singlePageDoc = new PDDocument();
            singlePageDoc.addPage(document.getPage(pdfPage - 1));

            // Save to app-specific storage
            File pdfDir = new File(getExternalFilesDir(null), "PDFs");
            if (!pdfDir.exists()) {
                pdfDir.mkdirs();
            }

            File outputFile = new File(pdfDir, "wage_slip_page_" + pdfPage + ".pdf");
            Log.d(TAG, "Saving to: " + outputFile.getAbsolutePath());

            singlePageDoc.save(outputFile);
            singlePageDoc.close();
            document.close();
            inputStream.close();

            if (outputFile.exists() && outputFile.length() > 0) {
                Log.d(TAG, "PDF saved successfully, size: " + outputFile.length());
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    openPdfWithChooser(outputFile);
                });
            } else {
                throw new Exception("PDF file not created");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Error loading PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void openPdfWithChooser(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
            Log.d(TAG, "FileProvider URI: " + uri);
            Log.d(TAG, "File exists: " + pdfFile.exists() + ", Size: " + pdfFile.length());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Create chooser to show all available PDF viewers
            String title = personName != null ? personName + " - Wage Slip" : "Open Wage Slip";
            Intent chooser = Intent.createChooser(intent, title);

            // Just try to start the chooser - let Android handle if apps exist
            startActivity(chooser);
            Log.d(TAG, "PDF chooser opened successfully");

            // Don't finish immediately - wait a moment for user to see chooser
            new android.os.Handler().postDelayed(this::finish, 1000);

        } catch (ActivityNotFoundException e) {
            // This exception is thrown if NO apps can handle the intent
            Log.e(TAG, "No PDF viewer apps found", e);
            showInstallPdfViewerDialog();
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showInstallPdfViewerDialog();
        }
    }

    private void showInstallPdfViewerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("PDF Viewer Required")
                .setMessage("To view wage slips, please install a free PDF viewer app from Play Store.\n\nRecommended: Adobe Acrobat Reader or Google PDF Viewer")
                .setPositiveButton("Install PDF Viewer", (dialog, which) -> openPlayStoreForPdfViewer())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void openPlayStoreForPdfViewer() {
        try {
            // Open Play Store with PDF viewer search results
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://search?q=pdf viewer&c=apps"));
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            // Play Store not available, use web
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/search?q=pdf viewer&c=apps"));
            startActivity(intent);
            finish();
        }
    }
}
