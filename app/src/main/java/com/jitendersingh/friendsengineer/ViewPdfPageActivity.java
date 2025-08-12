package com.jitendersingh.friendsengineer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ViewPdfPageActivity extends AppCompatActivity {

    private String pdfUrl;
    private int pdfPage;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pdf_page);

        pdfUrl = getIntent().getStringExtra("pdfUrl");
        pdfPage = getIntent().getIntExtra("pdfPage", 1);

        if (pdfUrl == null) {
            Toast.makeText(this, "PDF URL missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PDFBoxResourceLoader.init(getApplicationContext());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(this::downloadAndOpenPdfPage).start();
    }

    private void downloadAndOpenPdfPage() {
        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            PDDocument document = PDDocument.load(inputStream);

            if (pdfPage > document.getNumberOfPages()) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Page not found in PDF", Toast.LENGTH_SHORT).show();
                    finish();
                });
                document.close();
                return;
            }

            PDDocument singlePageDoc = new PDDocument();
            singlePageDoc.addPage(document.getPage(pdfPage - 1)); // zero-based

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            File outputFile = new File(downloadsDir, "wage_slip_page_" + pdfPage + ".pdf");

            singlePageDoc.save(outputFile);
            singlePageDoc.close();
            document.close();
            inputStream.close();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                openPdfFile(outputFile);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Error loading PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void openPdfFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
    }
}
