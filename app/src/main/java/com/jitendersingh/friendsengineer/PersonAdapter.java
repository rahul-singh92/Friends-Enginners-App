package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.ViewHolder> {

    public interface OnPersonClickListener {
        void onClick(WagePersonListActivity.PersonData person);
    }

    private List<WagePersonListActivity.PersonData> persons;
    private OnPersonClickListener listener;

    public PersonAdapter(List<WagePersonListActivity.PersonData> persons, OnPersonClickListener listener) {
        this.persons = persons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PersonAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonAdapter.ViewHolder holder, int position) {
        WagePersonListActivity.PersonData person = persons.get(position);
        holder.text1.setText(person.getName());
        holder.text2.setText("Father: " + person.getFatherName());
        holder.itemView.setTag(person);
    }

    @Override
    public int getItemCount() {
        return persons.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        public ViewHolder(@NonNull View itemView, OnPersonClickListener listener) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);

            itemView.setOnClickListener(v -> {
                WagePersonListActivity.PersonData person = (WagePersonListActivity.PersonData) v.getTag();
                if (listener != null) {
                    listener.onClick(person);
                }
            });
        }
    }
}
