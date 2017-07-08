/*
 * Copyright (c) 2017 by Francis Gálvez.
 */
package com.example.android.fruitmarket;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FruitActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //public static final String LOG_TAG = FruitActivity.class.getSimpleName();

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
    @Bind(R.id.increase_button)
    Button increaseButton;
    @Bind(R.id.decrease_button)
    Button decreaseButton;
    @Bind(R.id.shipment_icon)
    ImageButton askMore;
    private Uri currentFruitUri;
    private int stock;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fruit);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        currentFruitUri = intent.getData();

        getLoaderManager().initLoader(0, null, this);

        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseQuantity();
            }
        });

        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseQuantity();
            }
        });

        askMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shipmentRequest();
            }
        });
    }

    private void increaseQuantity() {
        String [] aux = quantityTextView.getText().toString().split(" ");
        stock = Integer.parseInt(aux[0]);
        stock++;
        quantityTextView.setText("" + stock + " kg");
    }

    private void decreaseQuantity() {
        String[] aux = quantityTextView.getText().toString().split(" ");
        stock = Integer.parseInt(aux[0]);

        if(stock > 0){
            stock--;
            quantityTextView.setText("" + stock + " kg");
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
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(FruitActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Prompt the user to confirm that they want to delete this fruit.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the fruit.
                deleteFruit();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the fruit.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteFruit() {
        // Only perform the delete if this is an existing fruit.
        if (currentFruitUri != null) {
            // Call the ContentResolver to delete the fruit at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentFruitUri
            // content URI already identifies the fruit that we want.
            int rowsDeleted = getContentResolver().delete(currentFruitUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_fruit_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_fruit_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
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
                FruitEntry.COLUMN_FRUIT_QUANTITY_ORDERED,
                FruitEntry.COLUMN_FRUIT_TOTAL,
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
            int qOrderedColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_QUANTITY_ORDERED);
            int totalColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_TOTAL);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            Double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int quantityOrdered = cursor.getInt(qOrderedColumnIndex);
            Double total = cursor.getDouble(totalColumnIndex);
            Bitmap image = Utils.getImage(cursor.getBlob(imageColumnIndex));

            // Update the views on the screen with the values from the database
            nameTextView.setText(name);
            priceTextView.setText(String.format("%s $/kg", String.valueOf(price)));
            supplierTextView.setText(supplier);
            quantityTextView.setText(String.format("%s kg", String.valueOf(quantity)));
            quantityOrderedTextView.setText("Quantity ordered: " + quantityOrdered + " kg");
            totalTextView.setText("Total: " + total + " $/kg");
            photoImageView.setImageBitmap(Bitmap.createScaledBitmap(image, 250, 250, false));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        nameTextView.setText("");
        priceTextView.setText("");
        supplierTextView.setText("");
        quantityTextView.setText("");
        quantityOrderedTextView.setText("");
        totalTextView.setText("");
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
