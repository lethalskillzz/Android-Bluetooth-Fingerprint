package com.fgtit.fingerprintattendance.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Ibrahim on 8/17/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    //TABLE
    public static final String TABLE_ENROLL = "enroll";
    public static final String TABLE_CAPTURE = "capture";

    //COMMON
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_IS_SYNC = "isSync";

    //ENROLL
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_FIRST_NAME = "firstName";
    public static final String COLUMN_LAST_NAME = "lastName";
    public static final String COLUMN_LEFT_THUMB = "leftThumb";
    public static final String COLUMN_RIGHT_THUMB = "rightThumb";

    //CAPTURE
    public static final String COLUMN_CAPTURE_ID = "captureId";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String DATABASE_NAME = "fingerprint_attendance.db";
    private static final int DATABASE_VERSION = 1;

    //Database creation sql statement
    private static final String DATABASE_CREATE_ENROLL = "CREATE TABLE " + TABLE_ENROLL + "( "
            + COLUMN_ID + " INTEGER PRIMARY KEY, " + COLUMN_IMAGE + " TEXT NOT NULL, " + COLUMN_FIRST_NAME +
            " TEXT NOT NULL , " + COLUMN_LAST_NAME + " TEXT NOT NULL, "+ COLUMN_LEFT_THUMB + " TEXT NOT NULL, " +
            COLUMN_RIGHT_THUMB + " TEXT NOT NULL, " + COLUMN_IS_SYNC + " INTEGER NOT NULL );";


    // Database creation sql statement
    private static final String DATABASE_CREATE_CAPTURE = "CREATE TABLE " + TABLE_CAPTURE + "( "
            + COLUMN_ID + " INTEGER PRIMARY KEY, " + COLUMN_CAPTURE_ID + " INTEGER NOT NULL, " + COLUMN_LONGITUDE +
            " DOUBLE NOT NULL , " + COLUMN_LATITUDE + " DOUBLE NOT NULL, "+ COLUMN_TIMESTAMP +
            " TEXT NOT NULL, " + COLUMN_IS_SYNC + " INTEGER NOT NULL );";



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_ENROLL);
        database.execSQL(DATABASE_CREATE_CAPTURE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENROLL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAPTURE);
        onCreate(db);
    }
}
