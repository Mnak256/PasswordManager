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
    private static final String COL_DOMAIN = "domain";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    DatabaseHandler(Context context, int version) {
        super(context, DB_NAME, null, version);
        //context is needed to create a database which is private to this application.
        //DB_NAME is the database name.
        //null is passed to the CursorFactory parameter because we don't want custom cursors to be returned form this database. The standard SQLineCursors will be enough.
        //1 is passed as the default version.
    }

    @Override
    public void onCreate(SQLiteDatabase db) { //invoked when getWritableDatabase() is called and iff the database doesn't already exist.
        String CREATE_TABLE = "CREATE TABLE " + TBL_NAME + " (" + COL_DOMAIN + " TEXT, " + COL_USERNAME + " TEXT, " + COL_PASSWORD + " TEXT, PRIMARY KEY (" + COL_DOMAIN + ", " + COL_USERNAME + ", " + COL_PASSWORD + "))"; //sql query to create the table.
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

    void addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_DOMAIN, record.domain);
        contentValues.put(COL_USERNAME, record.username);
        contentValues.put(COL_PASSWORD, record.password);

        db.insert(TBL_NAME, null, contentValues);
        db.close();
    }

    void removeRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TBL_NAME, COL_DOMAIN + " = ? AND " + COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?", record.toStringArr());
        db.close();
    }

    List<Record> getAllRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        String SELECT_ALL = "SELECT * FROM " + TBL_NAME;
        Cursor cursor = db.rawQuery(SELECT_ALL, null);
        List<Record> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Record record = new Record();
                record.domain = cursor.getString(0);
                record.username = cursor.getString(1);
                record.password = cursor.getString(2);
                list.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    List<Record> search(String searchQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        String SEARCH = "Select * FROM " + TBL_NAME + " WHERE " + COL_DOMAIN + " LIKE '%" + searchQuery + "%'";
        Cursor cursor = db.rawQuery(SEARCH, null);
        List<Record> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Record record = new Record();
                record.domain = cursor.getString(0);
                record.username = cursor.getString(1);
                record.password = cursor.getString(2);
                list.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
