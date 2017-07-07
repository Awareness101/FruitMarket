/*
 * Copyright (c) 2017 by Francis Gálvez.
 */
package com.example.android.fruitmarket;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;

public class CatalogActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for the fruit data loader */
    private static final int FRUIT_LOADER = 0;

    /** Adapter for the ListView */
    FruitCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the fruit data
        ListView fruitListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        fruitListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of fruit data in the Cursor.
        // There is no fruit data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new FruitCursorAdapter(this, null);
        fruitListView.setAdapter(mCursorAdapter);

        // Setup the item click listener
        fruitListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, FruitActivity.class);

                // Form the content URI that represents the specific fruit that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link FruitEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.fruits/fruits/2"
                // if the fruit with ID 2 was clicked on.
                Uri currentfruitUri = ContentUris.withAppendedId(FruitEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentfruitUri);

                // Launch the {@link EditorActivity} to display the data for the current fruit.
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(FRUIT_LOADER, null, this);
    }

    /**
     * Helper method to insert hardcoded fruit data into the database. For debugging purposes only.
     */
    private void insertFruit() {
        // Create a ContentValues object where column names are the keys,
        // and Tomato's fruit attributes are the values.
        ContentValues values = new ContentValues();
        values.put(FruitEntry.COLUMN_FRUIT_NAME, "Tomato");
        values.put(FruitEntry.COLUMN_FRUIT_PRICE, 5.0);
        values.put(FruitEntry.COLUMN_FRUIT_QUANTITY, 100);
        values.put(FruitEntry.COLUMN_FRUIT_SUPPLIER, "Juan Carlos S.L.");
        values.put(FruitEntry.COLUMN_FRUIT_PICTURE, "picture");

        // Insert a new row for Tomato into the provider using the ContentResolver.
        // Use the {@link FruitEntry#CONTENT_URI} to indicate that we want to insert
        // into the fruits database table.
        // Receive the new content URI that will allow us to access Tomato's data in the future.
        Uri newUri = getContentResolver().insert(FruitEntry.CONTENT_URI, values);
        Toast.makeText(this, "Dummy data inserted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper method to delete all fruits in the database.
     */
    private void deleteAllFruits() {
        int rowsDeleted = getContentResolver().delete(FruitEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from fruits database");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertFruit();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllFruits();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                FruitEntry._ID,
                FruitEntry.COLUMN_FRUIT_NAME,
                FruitEntry.COLUMN_FRUIT_QUANTITY,
                FruitEntry.COLUMN_FRUIT_PICTURE,
                FruitEntry.COLUMN_FRUIT_SUPPLIER,
                FruitEntry.COLUMN_FRUIT_PRICE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                FruitEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link FruitCursorAdapter} with this new cursor containing updated fruit data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}
