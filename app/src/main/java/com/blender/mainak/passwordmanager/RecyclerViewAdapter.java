package com.blender.mainak.passwordmanager;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private List<Record> records;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDomain, textViewUsername, textViewPassword;
        ViewHolder(View view) {
            super(view);
            this.textViewDomain = view.findViewById(R.id.textView_domain);
            this.textViewUsername = view.findViewById(R.id.textView_username);
            this.textViewPassword = view.findViewById(R.id.textView_password);
        }
    }

    RecyclerViewAdapter(List<Record> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_row, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record record = records.get(position);
        holder.textViewDomain.setText(record.domain);
        holder.textViewUsername.setText(record.username);
        holder.textViewPassword.setText(record.password);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void removeRecord(int position, DatabaseHandler databaseHandler) {
        databaseHandler.removeRecord(records.remove(position));
        notifyItemRemoved(position);
        //Log.i("RecyclerViewAdapter", "position removed:" + position);
    }

    public void addRecord(Record record) {
        records.add(record);
    }
}
