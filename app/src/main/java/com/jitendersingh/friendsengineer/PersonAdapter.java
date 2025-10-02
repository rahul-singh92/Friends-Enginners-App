package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private List<WagePersonListActivity.PersonData> persons;
    private OnPersonClickListener listener;

    public interface OnPersonClickListener {
        void onPersonClick(WagePersonListActivity.PersonData person);
    }

    public PersonAdapter(List<WagePersonListActivity.PersonData> persons, OnPersonClickListener listener) {
        this.persons = persons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        WagePersonListActivity.PersonData person = persons.get(position);

        holder.nameText.setText(person.getName() != null ? person.getName() : "Unknown");
        holder.fatherNameText.setText(person.getFatherName() != null ?
                "Father: " + person.getFatherName() : "");
        holder.pageInfo.setText("Page " + person.getPdfPage());

        // Set initial letter
        String name = person.getName();
        if (name != null && !name.isEmpty()) {
            holder.initial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            holder.initial.setText("?");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPersonClick(person);
            }
        });
    }

    @Override
    public int getItemCount() {
        return persons.size();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, fatherNameText, pageInfo, initial;

        PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.personName);
            fatherNameText = itemView.findViewById(R.id.personFatherName);
            pageInfo = itemView.findViewById(R.id.personPageInfo);
            initial = itemView.findViewById(R.id.personInitial);
        }
    }
}
