/*
 * Copyright (c) 2017 by Francis Gálvez.
 */
package com.example.android.fruitmarket.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;

/**
 * {@link ContentProvider} for Fruits app.
 */
public class FruitProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = FruitProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the fruits table */
    private static final int FRUITS = 500;

    /** URI matcher code for the content URI for a single fruit in the fruits table */
    private static final int FRUIT_ID = 501;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.fruits/fruits" will map to the
        // integer code {@link #FRUITS}. This URI is used to provide access to MULTIPLE rows
        // of the fruits table.
        sUriMatcher.addURI(FruitContract.CONTENT_AUTHORITY, FruitContract.PATH_FRUITS, FRUITS);

        // The content URI of the form "content://com.example.android.fruits/fruits/#" will map to the
        // integer code {@link #FRUIT_ID}. This URI is used to provide access to ONE single row
        // of the fruits table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.fruits/fruits/3" matches, but
        // "content://com.example.android.fruits/fruits" (without a number at the end) doesn't match.
        sUriMatcher.addURI(FruitContract.CONTENT_AUTHORITY, FruitContract.PATH_FRUITS + "/#", FRUIT_ID);
    }

    /** Database helper object */
    private FruitDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new FruitDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case FRUITS:
                // For the FRUITS code, query the fruits table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the fruits table.
                cursor = database.query(FruitEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case FRUIT_ID:
                // For the FRUIT_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.fruits/fruits/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = FruitEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the fruits table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(FruitEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FRUITS:
                return insertFruit(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a fruit into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertFruit(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(FruitEntry.COLUMN_FRUIT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Fruit requires a name");
        }

        // Check that the gender is valid
        Double price = values.getAsDouble(FruitEntry.COLUMN_FRUIT_PRICE);
        if (price == null || price < 0.0) {
            throw new IllegalArgumentException("Fruit requires valid price");
        }

        // If the weight is provided, check that it's greater than or equal to 0 kg
        Integer quantity = values.getAsInteger(FruitEntry.COLUMN_FRUIT_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Fruit requires valid quantity");
        }

        // No need to check the breed, any value is valid (including null).

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new fruit with the given values
        long id = database.insert(FruitEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the fruit content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FRUITS:
                return updateFruit(uri, contentValues, selection, selectionArgs);
            case FRUIT_ID:
                // For the FRUIT_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = FruitEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateFruit(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update fruits in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more fruits).
     * Return the number of rows that were successfully updated.
     */
    private int updateFruit(Uri uri, ContentValues values, String selection,
                            String[] selectionArgs) {
        // If the {@link FruitEntry#COLUMN_FRUIT_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(FruitEntry.COLUMN_FRUIT_NAME)) {
            String name = values.getAsString(FruitEntry.COLUMN_FRUIT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Fruit requires a name");
            }
        }

        // If the {@link FruitEntry#COLUMN_FRUIT_PRICE} key is present,
        // check that the price value is valid.
        if (values.containsKey(FruitEntry.COLUMN_FRUIT_PRICE)) {
            Double price = values.getAsDouble(FruitEntry.COLUMN_FRUIT_PRICE);
            if (price == null || price < 0.0) {
                throw new IllegalArgumentException("Fruit requires valid price");
            }
        }

        // If the {@link FruitEntry#COLUMN_FRUIT_QUANTITY} key is present,
        // check that the quantity value is valid.
        if (values.containsKey(FruitEntry.COLUMN_FRUIT_QUANTITY)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer quantity = values.getAsInteger(FruitEntry.COLUMN_FRUIT_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Fruit requires valid quantity");
            }
        }

        // No need to check the supplier and/or the picture, any value is valid (including null).

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(FruitEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FRUITS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(FruitEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FRUIT_ID:
                // Delete a single row given by the ID in the URI
                selection = FruitEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(FruitEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case FRUITS:
                return FruitEntry.CONTENT_LIST_TYPE;
            case FRUIT_ID:
                return FruitEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
