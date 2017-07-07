/*
 * Copyright (c) 2017 by Francis Gálvez.
 */
package com.example.android.fruitmarket.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class FruitContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private FruitContract() {}

    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    static final String CONTENT_AUTHORITY = "com.example.android.fruitmarket";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's)
     * For instance, content://com.example.android.fruits/fruits/ is a valid path for
     * looking at fruit data. content://com.example.android.fruits/staff/ will fail,
     * as the ContentProvider hasn't been given any information on what to do with "staff".
     */
    static final String PATH_FRUITS = "fruits";

    /**
     * Inner class that defines constant values for the fruits database table.
     * Each entry in the table represents a single fruit.
     */
    public static final class FruitEntry implements BaseColumns {

        /** The content URI to access the fruit data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FRUITS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of fruits.
         */
        static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FRUITS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single fruit.
         */
        static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FRUITS;

        /** Name of database table for fruits */
        final static String TABLE_NAME = "fruits";

        /**
         * Unique ID number for the fruit (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the fruit.
         *
         * Type: TEXT
         */
        public final static String COLUMN_FRUIT_NAME = "name";

        /**
         * Price of the fruit.
         *
         * Type: REAL
         */
        public final static String COLUMN_FRUIT_PRICE = "price";

        /**
         * Quantity of fruits avaliable.
         *
         * Type: INTEGER
         */
        public final static String COLUMN_FRUIT_QUANTITY = "quantity";

        /**
         * Supplier of the fruit.
         *
         * Type: TEXT
         */
        public final static String COLUMN_FRUIT_SUPPLIER = "supplier";

        /**
         * Picture of the fruit.
         *
         * Type: TEXT (URL of the photo)
         */
        public final static String COLUMN_FRUIT_PICTURE = "picture";
    }
}
