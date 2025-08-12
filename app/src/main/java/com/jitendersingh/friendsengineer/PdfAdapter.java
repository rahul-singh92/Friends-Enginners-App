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
    public PdfAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfAdapter.ViewHolder holder, int position) {
        PdfModel pdf = pdfList.get(position);

        holder.title.setText(pdf.getDocumentId());
        holder.subtitle.setText("From: " + pdf.getStartDate() + " To: " + pdf.getEndDate());

        holder.itemView.setOnClickListener(v -> {
            if (pdf.getPdfUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(pdf.getPdfUrl()));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "PDF URL not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, subtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
