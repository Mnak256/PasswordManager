package com.blender.mainak.passwordmanager;

import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    DatabaseHandler databaseHandler;
    RecyclerViewAdapter recyclerViewAdapter;
    java.util.List<Record> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                showInputDialog();
            }
        });

        databaseHandler = new DatabaseHandler(this, 3);

        refreshListView();
    }

    @Override
    protected void onDestroy() {
        databaseHandler.close();
        super.onDestroy();
    }

    void refreshListView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        records = databaseHandler.getAllRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        recyclerViewAdapter = new RecyclerViewAdapter(records);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add New Note");
        View view = getLayoutInflater().inflate(R.layout.input_alert_dialog, null);
        builder.setView(view);

        final EditText editTextTitle = view.findViewById(R.id.editText_title);
        final EditText editTextBody = view.findViewById(R.id.editText_body);

                /*
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    inputMethodManager.toggleSoftInputFromWindow(editTextTitle.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                }
                editTextTitle.requestFocus();
                */

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = editTextTitle.getText().toString();
                String body = editTextBody.getText().toString();
                if (title.length() == 0 || body.length() == 0) {
                    Toast.makeText(MainActivity.this, "Inputs Cannot be Empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                Record record = new Record(title, body);
                recyclerViewAdapter.addRecord(record);
                databaseHandler.addRecord(record);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
