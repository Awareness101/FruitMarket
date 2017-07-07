package com.example.android.fruitmarket;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.fruitmarket.data.FruitContract.FruitEntry;
import com.squareup.picasso.Picasso;

class FruitCursorAdapter extends CursorAdapter {

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
    public void bindView(View view, Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        ImageView pictureImageView = (ImageView) view.findViewById(R.id.product_image);
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView supplierTextView = (TextView) view.findViewById(R.id.supplier);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);

        // Find the columns of fruit attributes that we're interested in
        int pictureColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_PICTURE);
        int nameColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_NAME);
        int supplierColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_SUPPLIER);
        int priceColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(FruitEntry.COLUMN_FRUIT_QUANTITY);

        // Read the fruit attributes from the Cursor for the current fruit
        Uri fruitPicture = Uri.parse(cursor.getString(pictureColumnIndex));
        String fruitName = cursor.getString(nameColumnIndex);
        String fruitSupplier = cursor.getString(supplierColumnIndex);
        String fruitPrice = cursor.getString(priceColumnIndex);
        String fruitQuantity = cursor.getString(quantityColumnIndex);

        // If the fruit supplier is empty string or null, then use some default text
        // that says "Unknown supplier", so the TextView isn't blank.
        if (TextUtils.isEmpty(fruitSupplier)) {
            fruitSupplier = context.getString(R.string.supplier_unknown);
        }

        // Update the TextViews with the attributes for the current fruit
        nameTextView.setText(fruitName);
        supplierTextView.setText(fruitSupplier);
        priceTextView.setText(fruitPrice + " $/kg");
        quantityTextView.setText("Quantity: " + fruitQuantity);
        Picasso.with(context).load(fruitPicture)
                .placeholder(R.drawable.ic_new_image)

                .fit()
                .into(pictureImageView);
    }
}
