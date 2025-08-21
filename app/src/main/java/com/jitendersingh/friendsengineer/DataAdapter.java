package com.jitendersingh.friendsengineer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DataAdapter extends ArrayAdapter<DataListActivity.DataItem> {

    private final Context context;
    private final List<DataListActivity.DataItem> dataList;

    public DataAdapter(Context context, List<DataListActivity.DataItem> dataItems) {
        super(context, R.layout.item_data, dataItems);
        this.context = context;
        this.dataList = new ArrayList<>(dataItems); // initialize dataList as a copy
    }

    // Update adapter data and refresh listview
    public void updateData(List<DataListActivity.DataItem> newData) {
        dataList.clear();
        dataList.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Nullable
    @Override
    public DataListActivity.DataItem getItem(int position) {
        return dataList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_data, parent, false);
        }

        DataListActivity.DataItem item = dataList.get(position);

        TextView tvId = convertView.findViewById(R.id.tv_id);
        TextView tvName = convertView.findViewById(R.id.tv_name);
        TextView tvFatherName = convertView.findViewById(R.id.tv_father_name);

        tvId.setText(item.getDocumentId());
        tvName.setText(item.getName());
        tvFatherName.setText(item.getFatherName());

        return convertView;
    }
}
