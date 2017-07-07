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
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FruitActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private Uri currentFruitUri;

    @Bind(R.id.fruit_image)
    ImageView photoImageView;

    @Bind(R.id.fruit_name)
    TextView nameTextView;

    @Bind(R.id.fruit_price)
    TextView priceTextView;

    @Bind(R.id.fruit_supplier)
    TextView supplierTextView;

    @Bind(R.id.fruit_quantity)
    TextView quantityTextView;

    @Bind(R.id.quantity_ordered)
    TextView quantityOrderedTextView;

    @Bind(R.id.total_order)
    TextView totalTextView;

    @Bind(R.id.buy_icon)
    ImageButton orderOne;

    @Bind(R.id.shipment_icon)
    ImageButton askMore;

    private int stock, quantityOrdered = 0;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fruit);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        currentFruitUri = intent.getData();

        getLoaderManager().initLoader(0, null, this);

        orderOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateView();
            }
        });

        askMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shipmentRequest();
            }
        });
    }

    private void updateView() {
        String [] aux = quantityTextView.getText().toString().split(" ");
        stock = Integer.parseInt(aux[0]);

        String [] temp = priceTextView.getText().toString().split(" ");
        double price = Double.parseDouble(temp[0]);

        if(stock > 0){
            stock--;
            quantityTextView.setText("" + stock + " kg");
            quantityOrdered++;
            quantityOrderedTextView.setText("QUANTITY ORDERED: " + quantityOrdered + " kg");
            double total = price * quantityOrdered;
            totalTextView.setText("TOTAL: " + String.format("%.2f", total) + " $");
        } else {
            Toast.makeText(FruitActivity.this, R.string.no_stock, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fruit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_fruit:
                editFruit();
                finish();
                return true;
            case R.id.submit:
                updateStock();
                finish();
                return true;
            case R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the activity shows all fruit attributes, define a projection that contains
        // all columns from the fruit table
        String[] projection = {
                FruitEntry._ID,
                FruitEntry.COLUMN_FRUIT_NAME,
                FruitEntry.COLUMN_FRUIT_PRICE,
                FruitEntry.COLUMN_FRUIT_QUANTITY,
                FruitEntry.COLUMN_FRUIT_SUPPLIER,
                FruitEntry.COLUMN_FRUIT_PICTURE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                currentFruitUri,         // Query the content URI for the current fruit
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of fruit attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_PICTURE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            Double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String currentPhotoUri = cursor.getString(imageColumnIndex);

            // Update the views on the screen with the values from the database
            nameTextView.setText(name);
            priceTextView.setText(String.valueOf(price) + " $/kg");
            supplierTextView.setText(supplier);
            quantityTextView.setText(String.valueOf(quantity) + " kg");

            Picasso.with(this).load(currentPhotoUri)
                    .placeholder(R.drawable.ic_new_image)
                    .fit()
                    .into(photoImageView);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        nameTextView.setText("");
        priceTextView.setText("");
        supplierTextView.setText("");
        quantityOrderedTextView.setText("");
    }

    private void updateStock() {
        ContentValues values = new ContentValues();
        values.put(FruitEntry.COLUMN_FRUIT_QUANTITY, stock);

        if (currentFruitUri != null) {
            getContentResolver().update(currentFruitUri, values, null, null);
        }
    }

    private void shipmentRequest() {
        String fruit = nameTextView.getText().toString();
        String sup = supplierTextView.getText().toString();

        String supplierEmail = sup + "@gmail.com";
        String[] emailTo = {supplierEmail};

        String solicitud = "Hello, " + sup + " :\n" +
                "I need some of your product and I want to request a shipment. I left you the specifications below\n" +
                "Fruit: " + fruit + "\n" +
                "Quantity: (type the quantity you need here)" +
                "\nYou can send it to this address (type here your address)" +
                "\nThank you very much!\nBest regards!";

        Intent shipment = new Intent(Intent.ACTION_SENDTO);
        shipment.setData(Uri.parse("mailto:"));
        shipment.putExtra(Intent.EXTRA_EMAIL, emailTo);
        shipment.putExtra(Intent.EXTRA_SUBJECT, "Shipment Request ");
        shipment.putExtra(Intent.EXTRA_TEXT, solicitud);
        startActivity(shipment);
    }

    private void editFruit() {
        // Display the EditorActivity
        Intent intent = new Intent(FruitActivity.this, EditorActivity.class);

        Uri currentFruitUri = ContentUris.withAppendedId(FruitEntry.CONTENT_URI,
                ContentUris.parseId(this.currentFruitUri));

        intent.setData(currentFruitUri);
        startActivity(intent);
        finish();
    }
}
