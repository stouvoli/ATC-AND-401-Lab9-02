package com.numeris_ci.contentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

public class CustomContentProvider extends ContentProvider{
    //Fields for the content provider
    static final String PROVIDER_NAME = "com.numeris_ci.provider";
    static final String URL = "content://" + PROVIDER_NAME + "/nicknames";
    static final Uri CONTENT_URI = Uri.parse(URL);

    //Fields for the database
    static final String ID = "id";
    static final String NAME = "name";
    static final String NICK_NAME = "nickname";

    //Integer values used in content URI
    static final int NICKNAME = 1;
    static final int NICKNAME_ID = 2;

    DBHelper dbHelper;

    //Projection map for a query
    private static HashMap<String, String> NickNameMap;

    //Maps content URI "patterns" to the interger values that were set above
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "nicknames", NICKNAME);
        uriMatcher.addURI(PROVIDER_NAME, "nicknames", NICKNAME);
        uriMatcher.addURI(PROVIDER_NAME, "nicknames/#", NICKNAME_ID);
    }

    //Database declarations
    private SQLiteDatabase database;
    static final String DATABASE_NAME = "NicknamesDirectory";
    static final String TABLE_NAME = "Nicknames";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME +
            " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " name TEXT NOT NULL, " +
            " nickname TEXT NOT NULL);";

    //Class that creates and manages the provider's database
    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(DBHelper.class.getCanonicalName(),
                    "Upgrading database from version " + oldVersion +
                    " to " + newVersion + ". Old data will be destroyed.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        dbHelper = new DBHelper(context);
        //Permissions to be wriable
        database = dbHelper.getWritableDatabase();

        if (database == null) {
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        //The table name to query
        queryBuilder.setTables(TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            //Maps all database columns names
            case NICKNAME:
                queryBuilder.setProjectionMap(NickNameMap);
                break;
            case NICKNAME_ID:
                queryBuilder.appendWhere(ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknow URI " + uri);
        }
        if (sortOrder == null || sortOrder == "") {
            //No sorting-> sort on names by default
            sortOrder = NAME;
        }
        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
        //Register to watch a content URI for changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long row = database.insert(TABLE_NAME, "", values);
        //if record is added successfully
        if (row > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, row);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        } else {
            throw new SQLException("Fail to add a new record into " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case NICKNAME:
                count = database.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            case NICKNAME_ID:
                count = database.update(TABLE_NAME, values, ID + " = " + uri.getLastPathSegment() +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsuported URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case NICKNAME:
                //delete all record of the table
                count = database.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case NICKNAME_ID:
                String id = uri.getLastPathSegment();
                count = database.delete(TABLE_NAME, ID + " = " + id + (!TextUtils.isEmpty(selection) ? " AND (" +
                        selection + ")" : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsuported URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case NICKNAME:
                //Get all nicknames
                return "vnd.android.cursor.dir/vnd.example.nicknames";
            case NICKNAME_ID:
                //Get a particular name
                return "vnd.android.cursor.item/vnd.example.nicknames";
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri);
        }
    }
}
