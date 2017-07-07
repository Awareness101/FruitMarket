/*
 * Copyright (c) 2017 by Francis Gálvez.
 */
package com.example.android.fruitmarket.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;

class FruitDbHelper extends SQLiteOpenHelper {
    //public static final String LOG_TAG = FruitDbHelper.class.getSimpleName();

    /** Name of the database file */
    private static final String DATABASE_NAME = "fruitmarket.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link FruitDbHelper}.
     *
     * @param context of the app
     */
    FruitDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the fruits table
        String SQL_CREATE_FRUITS_TABLE =  "CREATE TABLE " + FruitEntry.TABLE_NAME + " ("
                + FruitEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FruitEntry.COLUMN_FRUIT_NAME + " TEXT NOT NULL, "
                + FruitEntry.COLUMN_FRUIT_PRICE + " REAL NOT NULL, "
                + FruitEntry.COLUMN_FRUIT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + FruitEntry.COLUMN_FRUIT_SUPPLIER + " TEXT DEFAULT 'UNKNOWN', "
                + FruitEntry.COLUMN_FRUIT_QUANTITY_ORDERED + " INTEGER DEFAULT 0, "
                + FruitEntry.COLUMN_FRUIT_TOTAL + " REAL DEFAULT 0.0, "
                + FruitEntry.COLUMN_FRUIT_PICTURE + " TEXT NOT NULL DEFAULT 'NO IMAGE AVAILABLE');";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_FRUITS_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
