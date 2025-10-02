package com.jitendersingh.friendsengineer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.ViewHolder> {

    private List<PdfModel> pdfList;
    private Context context;

    public PdfAdapter(List<PdfModel> pdfList, Context context) {
        this.pdfList = pdfList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_pdf, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PdfModel pdf = pdfList.get(position);

        // Format dates
        String startDate = (pdf.getStartDate() != null && !pdf.getStartDate().isEmpty())
                ? pdf.getStartDate() : "N/A";
        String endDate = (pdf.getEndDate() != null && !pdf.getEndDate().isEmpty())
                ? pdf.getEndDate() : "N/A";

        // Set date range as main text
        holder.dateRangeText.setText(startDate + " → " + endDate);

        // Set individual dates in badges
        holder.startDateText.setText("Start: " + startDate);
        holder.endDateText.setText("End: " + endDate);

        // Click listener to open PDF
        holder.itemView.setOnClickListener(v -> {
            if (pdf.getPdfUrl() != null && !pdf.getPdfUrl().isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(pdf.getPdfUrl()));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Unable to open PDF", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "PDF URL not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateRangeText;
        TextView startDateText;
        TextView endDateText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateRangeText = itemView.findViewById(R.id.scheduleDateRange);
            startDateText = itemView.findViewById(R.id.scheduleStartDate);
            endDateText = itemView.findViewById(R.id.scheduleEndDate);
        }
    }
}
