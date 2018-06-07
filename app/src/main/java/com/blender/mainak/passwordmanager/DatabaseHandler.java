package com.blender.mainak.passwordmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DB_NAME = "PasswordManagerDatabase";
    private static final String TBL_NAME = "PasswordManager";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    public DatabaseHandler(Context context) {
        super(context, DB_NAME, null, 1);
        //context is needed to create a database which is private to this application.
        //DB_NAME is the database name.
        //null is passed to the CursorFactory parameter because we don't want custom cursors to be returned form this database. The standard SQLineCursors will be enough.
        //1 is passed as the default version.
    }

    @Override
    public void onCreate(SQLiteDatabase db) { //invoked when getWritableDatabase() is called and iff the database doesn't already exist.
        String CREATE_TABLE = "CREATE TABLE " + TBL_NAME + " (" + COL_USERNAME + " TEXT, " + COL_PASSWORD + " TEXT, PRIMARY KEY (" + COL_USERNAME + ", " + COL_PASSWORD + "))"; //sql query to create the table.
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old, int new_) {
        //used to change the database schema.
        // If schema is changed then the version number should be incremented while creating the database and only then this method is invoked.
        // In this method we just recreate the table as data loss is not an issue with this application.
        String DROP_TABLE = "DROP TABLE IF EXISTS " + TBL_NAME; //sql query to delete the table
        db.execSQL(DROP_TABLE);
        onCreate(db); //create the table again.
    }

    //helper functions for CURD operations:

    public void addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, record.username);
        contentValues.put(COL_PASSWORD, record.password);

        db.insert(TBL_NAME, null, contentValues);
        db.close();
    }

    public List<Record> getAllStudents() {
        SQLiteDatabase db = this.getWritableDatabase();
        String SELECT_ALL = "SELECT * FROM " + TBL_NAME;
        Cursor cursor = db.rawQuery(SELECT_ALL, null);
        List<Record> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Record record = new Record();
                record.username = cursor.getString(0);
                record.password = cursor.getString(1);
                list.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
