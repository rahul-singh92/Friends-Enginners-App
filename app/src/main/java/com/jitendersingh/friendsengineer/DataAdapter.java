package com.jitendersingh.friendsengineer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class DataAdapter extends ArrayAdapter<DataListActivity.DataItem> {

    private final Context context;
    private final List<DataListActivity.DataItem> dataItems;

    public DataAdapter(Context context, List<DataListActivity.DataItem> dataItems) {
        super(context, R.layout.item_data, dataItems);
        this.context = context;
        this.dataItems = dataItems;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_data, parent, false);
        }

        DataListActivity.DataItem item = dataItems.get(position);

        TextView tvId = convertView.findViewById(R.id.tv_id);
        TextView tvName = convertView.findViewById(R.id.tv_name);
        TextView tvFatherName = convertView.findViewById(R.id.tv_father_name);

        tvId.setText(String.valueOf(item.getId()));
        tvName.setText(item.getName());
        tvFatherName.setText(item.getFatherName());

        return convertView;
    }
}