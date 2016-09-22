package com.fgtit.fingerprintattendance.doa;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.fgtit.fingerprintattendance.helper.DatabaseHelper;
import com.fgtit.fingerprintattendance.model.EnrollItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by letha on 9/4/2016.
 */
public class EnrollDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_IMAGE, DatabaseHelper.COLUMN_FIRST_NAME,
            DatabaseHelper.COLUMN_LAST_NAME, DatabaseHelper.COLUMN_LEFT_THUMB, DatabaseHelper.COLUMN_RIGHT_THUMB, DatabaseHelper.COLUMN_IS_SYNC};

    public EnrollDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }


    public void close() {
        dbHelper.close();
    }


    /**
     * Creating Enroll
     */
    public boolean createEnroll(EnrollItem enrollItem) {

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IMAGE, enrollItem.getImage());
        values.put(DatabaseHelper.COLUMN_FIRST_NAME, enrollItem.getFirstName());
        values.put(DatabaseHelper.COLUMN_LAST_NAME, enrollItem.getLastNme());
        values.put(DatabaseHelper.COLUMN_LEFT_THUMB, enrollItem.getLeftThumb());
        values.put(DatabaseHelper.COLUMN_RIGHT_THUMB, enrollItem.getRightThumb());
        values.put(DatabaseHelper.COLUMN_IS_SYNC, enrollItem.getIsSync()?"1":"0");
        // insert row
        database.insert(DatabaseHelper.TABLE_ENROLL, DatabaseHelper.COLUMN_FIRST_NAME, values);

        return true;
    }


    /**
     * Fetching All Enroll
     */
    public List<EnrollItem> fetchAllEnroll() {

        String whereClause = null;
        String[] whereArgs = null;
        List<EnrollItem> enrollItems = new ArrayList<EnrollItem>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_ENROLL,
                allColumns, whereClause, whereArgs, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            EnrollItem item = cursorToEnroll(cursor);
            enrollItems.add(item);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return enrollItems;
    }


    private EnrollItem cursorToEnroll(Cursor cursor) {

        EnrollItem item = new EnrollItem();
        item.setId(cursor.getInt(0));
        item.setImage(cursor.getString(1));
        item.setFirstName(cursor.getString(2));
        item.setLastName(cursor.getString(3));
        item.setLeftThumb(cursor.getString(4));
        item.setRightThumb(cursor.getString(5));
        item.setIsSync(cursor.getInt(6)==1);
        return item;
    }


    public void deleteEnrollItem(EnrollItem enrollItem) {
        long id = enrollItem.getId();
        database.delete(DatabaseHelper.TABLE_ENROLL, DatabaseHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void clear() {
        database.delete(DatabaseHelper.TABLE_ENROLL, null, null);
    }



}
