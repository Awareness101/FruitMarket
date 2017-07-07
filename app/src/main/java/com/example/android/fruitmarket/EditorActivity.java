/*
 * Copyright (c) 2017 by Francis Gálvez.
 */
package com.example.android.fruitmarket;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    //public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    /**
     * Photo request code
     */
    public static final int PHOTO_REQUEST_CODE = 20;
    public static final int EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE = 21;
    /** Identifier for the fruit data loader */
    private static final int EXISTING_FRUIT_LOADER = 0;

    @Bind(R.id.scrollview)
    ScrollView scrollView;
    /** EditText field to enter the fruit's name */
    @Bind(R.id.edit_fruit_name)
    EditText mNameEditText;
    /** EditText field to enter the fruit's price */
    @Bind(R.id.edit_fruit_price)
    EditText mPriceEditText;
    /** EditText field to enter the fruit's quantity */
    @Bind(R.id.edit_fruit_quantity)
    EditText mQuantityEditText;
    /** EditText field to enter the fruit's supplier */
    @Bind(R.id.edit_fruit_supplier)
    EditText mSupplierEditText;
    /** ImageView for the fruit's picture */
    @Bind(R.id.image_fruit_picture)
    ImageView mPictureImageView;

    /**
     * Content URI for the existing fruit (null if it's a new fruit)
     */
    private Uri mCurrentFruitUri;

    private byte[] imageByte;

    /** Boolean flag that keeps track of whether the fruit has been edited (true) or not (false) */
    private boolean mFruitHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mFruitHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mFruitHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_image);
        imageByte = Utils.getBytes(bm); // setting default system image icon in case user didnt upload a image

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new fruit or editing an existing one.
        Intent intent = getIntent();
        mCurrentFruitUri = intent.getData();

        // If the intent DOES NOT contain a fruit content URI, then we know that we are
        // creating a new fruit.
        if (mCurrentFruitUri == null) {
            // This is a new fruit, so change the app bar to say "Add a Fruit"
            setTitle(getString(R.string.editor_activity_title_new_fruit));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a fruit that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing fruit, so change app bar to say "Edit Fruit"
            setTitle(getString(R.string.editor_activity_title_edit_fruit));

            // Initialize a loader to read the fruit data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_FRUIT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        ButterKnife.bind(this);

        scrollView.fullScroll(ScrollView.FOCUS_UP);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mPictureImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mFruitHasChanged = true;
                return false;
            }
        });

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);

        mPictureImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFruitImage(v);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            onSelectFromGalleryResult(data);
        }
    }

    /**
     * Get user input from editor and save fruit into database.
     */
    private void saveFruit() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();

        // Check if this is supposed to be a new fruit
        // and check if all the fields in the editor are blank
        if (mCurrentFruitUri == null &&
                TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString) ||
                TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(supplierString) ||
                imageByte == null) {
            // Since no fields were modified, we can return early without creating a new fruit.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, "Please, insert all required information.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and fruit attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(FruitEntry.COLUMN_FRUIT_NAME, nameString);
        values.put(FruitEntry.COLUMN_FRUIT_PRICE, priceString);
        values.put(FruitEntry.COLUMN_FRUIT_SUPPLIER, supplierString);
        values.put(FruitEntry.COLUMN_FRUIT_PICTURE, imageByte);

        // If the quantity is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(FruitEntry.COLUMN_FRUIT_QUANTITY, quantity);

        // Determine if this is a new or existing fruit by checking if mCurrentFruitUri is null or not
        if (mCurrentFruitUri == null) {
            // This is a NEW fruit, so insert a new fruit into the provider,
            // returning the content URI for the new fruit.
            Uri newUri = getContentResolver().insert(FruitEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_fruit_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_fruit_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING fruit, so update the fruit with content URI:
            // mCurrentFruitUri and pass in the new ContentValues. Pass in null for the selection
            // and selection args because mCurrentFruitUri will already identify the correct row
            // in the database that we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentFruitUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_fruit_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_fruit_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new fruit, hide the "Delete" menu item.
        if (mCurrentFruitUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save fruit to database
                saveFruit();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the fruit hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mFruitHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the fruit hasn't changed, continue with handling back button press
        if (!mFruitHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all fruit attributes, define a projection that contains
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
                mCurrentFruitUri,         // Query the content URI for the current fruit
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
            Bitmap image = Utils.getImage(cursor.getBlob(imageColumnIndex));

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(String.valueOf(price));
            mSupplierEditText.setText(supplier);
            mQuantityEditText.setText(String.valueOf(quantity));

            mPictureImageView.setImageBitmap(image);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
        mQuantityEditText.setText("");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
            chooseFruitImage();
        } else {
            Toast.makeText(this, R.string.permission,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
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

    /**
     * Perform the deletion of the fruit in the database.
     */
    private void deleteFruit() {
        // Only perform the delete if this is an existing fruit.
        if (mCurrentFruitUri != null) {
            // Call the ContentResolver to delete the fruit at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentFruitUri
            // content URI already identifies the fruit that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentFruitUri, null, null);

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

    /**
     * Update the image of the fruit
     */
    public void updateFruitImage(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                chooseFruitImage();
            } else {
                String[] permisionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permisionRequest, EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE);
            }
        } else {
            // In case we dont need any permissions to access the data
            chooseFruitImage();
        }
    }

    /**
     * Choose the image from the user's device
     */
    private void chooseFruitImage() {
        Intent selectImage= new Intent(Intent.ACTION_PICK);

        File photo = Environment.getExternalStoragePublicDirectory (Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = photo.getPath();

        Uri data = Uri.parse(pictureDirectoryPath);

        selectImage.setDataAndType(data, "image/*");

        startActivityForResult(selectImage, PHOTO_REQUEST_CODE);
    }

    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mPictureImageView.setVisibility(View.VISIBLE);
        mPictureImageView.setImageBitmap(bm);

        this.imageByte = Utils.getBytes(bm);
    }
}
