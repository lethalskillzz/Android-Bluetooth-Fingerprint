package com.fgtit.fingerprintattendance.doa;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.fgtit.fingerprintattendance.helper.DatabaseHelper;
import com.fgtit.fingerprintattendance.model.CaptureItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by letha on 9/4/2016.
 */
public class CaptureDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = { DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_CAPTURE_ID, DatabaseHelper.COLUMN_LONGITUDE,
            DatabaseHelper.COLUMN_LATITUDE, DatabaseHelper.COLUMN_TIMESTAMP, DatabaseHelper.COLUMN_IS_SYNC};

    public CaptureDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }


    public void close() {
        dbHelper.close();
    }


    /**
     * Creating  Capture
     */
    public boolean createCapture(CaptureItem captureItem) {

        ContentValues values = new ContentValues();
        //values.put(DatabaseHelper.COLUMN_ID, captureItem.getId());
        values.put(DatabaseHelper.COLUMN_CAPTURE_ID, captureItem.getCaptureId());
        values.put(DatabaseHelper.COLUMN_LONGITUDE, captureItem.getLongitude());
        values.put(DatabaseHelper.COLUMN_LATITUDE, captureItem.getLatitude());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, captureItem.getTimeStamp());
        values.put(DatabaseHelper.COLUMN_IS_SYNC, captureItem.getIsSync());
        // insert row
        database.insert(DatabaseHelper.TABLE_CAPTURE, DatabaseHelper.COLUMN_CAPTURE_ID, values);

        return true;
    }


    /**
     * Fetching All Capture
     */
    public List<CaptureItem> fetchAllCapture() {

        String whereClause = null;
        String[] whereArgs = null;
        List<CaptureItem> captureItems = new ArrayList<CaptureItem>();

        Cursor cursor = database.query(DatabaseHelper.TABLE_CAPTURE,
                allColumns, whereClause, whereArgs, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CaptureItem item = cursorToCapture(cursor);
            captureItems.add(item);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return captureItems;
    }


    private CaptureItem cursorToCapture(Cursor cursor) {
        CaptureItem item = new CaptureItem();
        item.setId(cursor.getInt(0));
        item.setCaptureId(cursor.getInt(1));
        item.setLongitude(cursor.getDouble(2));
        item.setLatitude(cursor.getDouble(3));
        item.setTimeStamp(cursor.getString(4));
        item.setIsSync(cursor.getInt(5)==1);
        return item;
    }


    public void deleteCaptureItem(CaptureItem captureItem) {
        long id = captureItem.getId();
        database.delete(DatabaseHelper.TABLE_CAPTURE, DatabaseHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void clear() {
        database.delete(DatabaseHelper.TABLE_CAPTURE, null, null);
    }



}
