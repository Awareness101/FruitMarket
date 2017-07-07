package com.example.android.fruitmarket;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;

class FruitCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = FruitCursorAdapter.class.getSimpleName();

    /**
     * Constructs a new {@link FruitCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    FruitCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the fruit data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current fruit can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        ImageView pictureImageView = (ImageView) view.findViewById(R.id.product_image);
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView supplierTextView = (TextView) view.findViewById(R.id.supplier);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        TextView quantityOrderedTextView = (TextView) view.findViewById(R.id.quantity_ordered);
        TextView totalTextView = (TextView) view.findViewById(R.id.total_order);
        ImageView orderImageView = (ImageView) view.findViewById(R.id.buy_icon);

        // Find the columns of fruit attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(FruitEntry._ID);
        int imageColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_PICTURE);
        int nameColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_NAME);
        int supplierColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_SUPPLIER);
        int priceColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_QUANTITY);
        int qOrderedColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_QUANTITY_ORDERED);
        int totalColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_TOTAL);

        // Read the fruit attributes from the Cursor for the current fruit
        Bitmap image = Utils.getImage(cursor.getBlob(imageColumnIndex));
        int inventoryId = cursor.getInt(idColumnIndex);
        String fruitName = cursor.getString(nameColumnIndex);
        String fruitSupplier = cursor.getString(supplierColumnIndex);
        final Double fruitPrice = cursor.getDouble(priceColumnIndex);
        final int fruitQuantity = cursor.getInt(quantityColumnIndex);
        final int quantityOrdered = cursor.getInt(qOrderedColumnIndex);
        Double totalPvp = cursor.getDouble(totalColumnIndex);

        final Uri currentItemUri = ContentUris.withAppendedId(FruitEntry.CONTENT_URI, inventoryId);

        // Update the TextViews with the attributes for the current fruit
        nameTextView.setText(fruitName);
        supplierTextView.setText(fruitSupplier);
        priceTextView.setText(String.format("%s $/kg", fruitPrice));
        quantityTextView.setText("Quantity: " + fruitQuantity + " kg");
        quantityOrderedTextView.setText("Quantity ordered: " + quantityOrdered + " kg");
        totalTextView.setText("Total: " + totalPvp + " $");
        pictureImageView.setImageBitmap(image);

        orderImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver resolver = v.getContext().getContentResolver();
                int stock = fruitQuantity;

                Log.e(LOG_TAG, "Stock is " + stock);

                if (stock == 0) {
                    Toast.makeText(v.getContext(), R.string.no_stock,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                double price = fruitPrice;
                int orderedAux = quantityOrdered + 1;
                double total = price * orderedAux;

                ContentValues values = new ContentValues();
                values.put(FruitEntry.COLUMN_FRUIT_QUANTITY, --stock);
                values.put(FruitEntry.COLUMN_FRUIT_TOTAL, total);
                values.put(FruitEntry.COLUMN_FRUIT_QUANTITY_ORDERED, orderedAux);
                resolver.update(currentItemUri, values, null, null);
                context.getContentResolver().notifyChange(currentItemUri, null);
            }
        });
    }
}
