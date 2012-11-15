package com.chirpy.data;


import java.util.Arrays;
import java.util.HashMap;

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
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.chirpy.util.Logger;

/**
 * Tweets content provider. This class stores the tweets, mentions and user tweets in separate tables
 * 
 * @author dhavalmotghare@gmail.com
 *
 */
public class TweetDatabase extends ContentProvider
{
	/** Log tag */
	private static final String TAG = TweetDatabase.class.getSimpleName();
    private static final String DATABASE_NAME = "chirpyTweets";
    
    /** Database constants */
    private static final int DATABASE_VERSION = 1;
    private static final String TWEETS_TABLE_NAME = "tweets";
    private static final String MENTIONS_TABLE_NAME = "mentions";
    private static final String MYTWEET_TABLE_NAME = "mytweets";
    public static final String AUTHORITY = "com.chirpy.tweetsProvider";

    /** URI  */
    private static final int TWEETS = 1;
    private static final int TWEETS_COUNT = 2;
    private static final int MENTIONS = 3;
    private static final int MENTIONS_COUNT = 4;
    private static final int MYTWEETS = 5;
    private static final int MYTWEETS_COUNT = 6;
    private static final int TWEET_ID = 7;
    private static final int MENTION_TWEET_ID = 8;
    private static final int MYTWEET_ID = 9;
    
    /** Database helper reference */
    private DatabaseHelper databaseHelper;
    private static HashMap<String, String> tweetsProjections;
    private static final UriMatcher sUriMatcher;
 
    /** Table columns */
	public static final String AUTHOR_ID = "author_id";
	public static final String MESSAGE = "message";
	public static final String SOURCE = "source";
	public static final String TWEET_TYPE = "tweet_type";
	public static final String IN_REPLY_TO_STATUS_ID = "in_reply_to_status_id";
	public static final String IN_REPLY_TO_AUTHOR_ID = "in_reply_to_author_id";
	public static final String FAVORITED = "favorited";
	public static final String CREATED_DATE = "created";
	public static final String SENT_DATE = "sent";
	public static final String IMG_URL = "imgUrl";
	public static final String IMAGE_BLOB = "image_blob";

    public static final int TIMELINE_TYPE_NONE = 0;
	public static final int TIMELINE_TYPE_FRIENDS = 1;
	public static final int TIMELINE_TYPE_MENTIONS = 2;
	public static final int TIMELINE_TYPE_MYTWEETS = 3;
	
    /**
	 * Tweets table
	 * 
	 */
	public static final class Tweets implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tweets");
		public static final String DEFAULT_SORT_ORDER = "sent DESC";
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.chirpy.tweet";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.chirpy.tweet";

	}

	/**
	 * Mentions table
	 * 
	 */
	public static final class Mentions implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/mentions");
		public static final String DEFAULT_SORT_ORDER = "sent DESC";
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.chirpy.mention";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.chirpy.mention";

	}
	
	/**
	 * My Tweets table
	 * 
	 */
	public static final class MyTweets implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/mytweets");
		public static final String DEFAULT_SORT_ORDER = "sent DESC";
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.chirpy.mytweet";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.chirpy.mytweet";

	}


    /**
     * Database helper for Chirpy.
     * 
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
    	
        private static final String TAG = TweetDatabase.class.getSimpleName();
        private SQLiteDatabase mDatabase;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
        	return super.getWritableDatabase();
        }

        @Override
        public synchronized SQLiteDatabase getReadableDatabase() {
            return super.getReadableDatabase();
        }

        @Override
        public synchronized void close() {
            super.close();
            if (mDatabase != null && mDatabase.isOpen()) {
                mDatabase.close();
                mDatabase = null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         */
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "Creating tables");
            db.execSQL("CREATE TABLE " + TWEETS_TABLE_NAME + " (" + Tweets._ID
	                    + " INTEGER PRIMARY KEY," + AUTHOR_ID + " TEXT,"  + IMG_URL + " TEXT," 
	                    + IMAGE_BLOB + " BLOB," + MESSAGE
	                    + " TEXT," + SOURCE + " TEXT," + TWEET_TYPE + " INTEGER,"
	                    + IN_REPLY_TO_STATUS_ID + " INTEGER," + IN_REPLY_TO_AUTHOR_ID
	                    + " TEXT," + FAVORITED + " INTEGER," + SENT_DATE + " INTEGER,"
	                    + CREATED_DATE + " INTEGER" + ");");
            db.execSQL("CREATE TABLE " + MENTIONS_TABLE_NAME + " (" + Tweets._ID
	                    + " INTEGER PRIMARY KEY," + AUTHOR_ID + " TEXT," + IMG_URL + " TEXT," 
	                    + IMAGE_BLOB + " BLOB," + MESSAGE
	                    + " TEXT," + SOURCE + " TEXT," + TWEET_TYPE + " INTEGER,"
	                    + IN_REPLY_TO_STATUS_ID + " INTEGER," + IN_REPLY_TO_AUTHOR_ID
	                    + " TEXT," + FAVORITED + " INTEGER," + SENT_DATE + " INTEGER,"
	                    + CREATED_DATE + " INTEGER" + ");");
            db.execSQL("CREATE TABLE " + MYTWEET_TABLE_NAME + " (" + Tweets._ID
	                    + " INTEGER PRIMARY KEY," + AUTHOR_ID + " TEXT," + IMG_URL + " TEXT," 
	                    + IMAGE_BLOB + " BLOB," + MESSAGE
	                    + " TEXT," + SOURCE + " TEXT," + TWEET_TYPE + " INTEGER,"
	                    + IN_REPLY_TO_STATUS_ID + " INTEGER," + IN_REPLY_TO_AUTHOR_ID
	                    + " TEXT," + FAVORITED + " INTEGER," + SENT_DATE + " INTEGER,"
	                    + CREATED_DATE + " INTEGER" + ");");
	        }

	        @Override
	        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	        	Logger.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
						+ ", which will destroy all old data");
				db.execSQL("DROP TABLE IF EXISTS " + TWEETS_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + MENTIONS_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + MYTWEET_TABLE_NAME);

				onCreate(db);
	        }
	       
	        
    }

    /**
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return (databaseHelper == null) ? false : true;
    }

	   

    /**
     * Delete a record from the database.
     * 
     * @see android.content.ContentProvider#delete(android.net.Uri,
     *      java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TWEETS:
                count = db.delete(TWEETS_TABLE_NAME, selection, selectionArgs);
                break;

            case TWEET_ID:
                String tweetId = uri.getPathSegments().get(1);
                count = db.delete(TWEETS_TABLE_NAME, Tweets._ID + "=" + tweetId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;

            case MENTION_TWEET_ID:
                tweetId = uri.getPathSegments().get(1);
                count = db.delete(MENTIONS_TABLE_NAME, Mentions._ID + "=" + tweetId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
                
            case MYTWEET_ID:
                tweetId = uri.getPathSegments().get(1);
                count = db.delete(MYTWEET_TABLE_NAME, MyTweets._ID + "=" + tweetId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            case MENTIONS:
                count = db.delete(MENTIONS_TABLE_NAME, selection, selectionArgs);
                break;

            case MYTWEETS:
                count = db.delete(MYTWEET_TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /**
     * Insert a new record into the database.
     * 
     * @see android.content.ContentProvider#insert(android.net.Uri,
     *      android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        ContentValues values;
        long rowId;
        Long now = System.currentTimeMillis();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String table;
        String nullColumnHack;
        Uri contentUri;

        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        switch (sUriMatcher.match(uri)) {
            case TWEETS:
                table = TWEETS_TABLE_NAME;
                nullColumnHack = MESSAGE;
                contentUri = Tweets.CONTENT_URI;
                /**
                 * Add default values for missed required fields
                 */
                if (values.containsKey(CREATED_DATE) == false)
                    values.put(CREATED_DATE, now);
                if (values.containsKey(SENT_DATE) == false)
                    values.put(SENT_DATE, now);
                if (values.containsKey(AUTHOR_ID) == false)
                    values.put(AUTHOR_ID, "");
                if (values.containsKey(IMG_URL) == false)
                    values.put(IMG_URL, "");
                if (values.containsKey(MESSAGE) == false)
                    values.put(MESSAGE, "");
                if (values.containsKey(SOURCE) == false)
                    values.put(SOURCE, "");
                if (values.containsKey(TWEET_TYPE) == false)
                    values.put(TWEET_TYPE, TIMELINE_TYPE_FRIENDS);
                if (values.containsKey(IN_REPLY_TO_AUTHOR_ID) == false)
                    values.put(IN_REPLY_TO_AUTHOR_ID, "");
                if (values.containsKey(FAVORITED) == false)
                    values.put(FAVORITED, 0);
                break;

            case MENTIONS:
                table = MENTIONS_TABLE_NAME;
                nullColumnHack = MESSAGE;
                contentUri = Mentions.CONTENT_URI;
                /**
                 * Add default values for missed required fields
                 */
                if (values.containsKey(CREATED_DATE) == false)
                    values.put(CREATED_DATE, now);
                if (values.containsKey(SENT_DATE) == false)
                    values.put(SENT_DATE, now);
                if (values.containsKey(AUTHOR_ID) == false)
                    values.put(AUTHOR_ID, "");
                if (values.containsKey(IMG_URL) == false)
                    values.put(IMG_URL, "");
                if (values.containsKey(MESSAGE) == false)
                    values.put(MESSAGE, "");
                if (values.containsKey(SOURCE) == false)
                    values.put(SOURCE, "");
                if (values.containsKey(TWEET_TYPE) == false)
                    values.put(TWEET_TYPE, TIMELINE_TYPE_FRIENDS);
                if (values.containsKey(IN_REPLY_TO_AUTHOR_ID) == false)
                    values.put(IN_REPLY_TO_AUTHOR_ID, "");
                if (values.containsKey(FAVORITED) == false)
                    values.put(FAVORITED, 0);
                break;
            case MYTWEETS:
                table = MYTWEET_TABLE_NAME;
                nullColumnHack = MESSAGE;
                contentUri = Tweets.CONTENT_URI;
                /**
                 * Add default values for missed required fields
                 */
                if (values.containsKey(CREATED_DATE) == false)
                    values.put(CREATED_DATE, now);
                if (values.containsKey(SENT_DATE) == false)
                    values.put(SENT_DATE, now);
                if (values.containsKey(AUTHOR_ID) == false)
                    values.put(AUTHOR_ID, "");
                if (values.containsKey(IMG_URL) == false)
                    values.put(IMG_URL, "");
                if (values.containsKey(MESSAGE) == false)
                    values.put(MESSAGE, "");
                if (values.containsKey(SOURCE) == false)
                    values.put(SOURCE, "");
                if (values.containsKey(TWEET_TYPE) == false)
                    values.put(TWEET_TYPE, TIMELINE_TYPE_FRIENDS);
                if (values.containsKey(IN_REPLY_TO_AUTHOR_ID) == false)
                    values.put(IN_REPLY_TO_AUTHOR_ID, "");
                if (values.containsKey(FAVORITED) == false)
                    values.put(FAVORITED, 0);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        rowId = db.insert(table, nullColumnHack, values);
        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(contentUri, rowId);
            return newUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * Get a cursor to the database
     * 
     * @see android.content.ContentProvider#query(android.net.Uri,
     *      java.lang.String[], java.lang.String, java.lang.String[],
     *      java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String sql = "";

        int matchedCode = sUriMatcher.match(uri);
        switch (matchedCode) {
            case TWEETS:
                qb.setTables(TWEETS_TABLE_NAME);
                qb.setProjectionMap(tweetsProjections);
                break;

            case TWEETS_COUNT:
                sql = "SELECT count(*) FROM " + TWEETS_TABLE_NAME;
                if (selection != null && selection.length() > 0) {
                    sql += " WHERE " + selection;
                }
                break;

            case TWEET_ID:
                qb.setTables(TWEETS_TABLE_NAME);
                qb.setProjectionMap(tweetsProjections);
                qb.appendWhere(Tweets._ID + "=" + uri.getPathSegments().get(1));
                break;

            case MENTIONS:
                qb.setTables(MENTIONS_TABLE_NAME);
                qb.setProjectionMap(tweetsProjections);
                break;

            case MENTIONS_COUNT:
                sql = "SELECT count(*) FROM " + MENTIONS_TABLE_NAME;
                if (selection != null && selection.length() > 0) {
                    sql += " WHERE " + selection;
                }
                break;

            case MENTION_TWEET_ID:
                qb.setTables(MENTIONS_TABLE_NAME);
                qb.setProjectionMap(tweetsProjections);
                qb.appendWhere(Mentions._ID + "=" + uri.getPathSegments().get(1));
                break;
                
            case MYTWEETS:
                qb.setTables(MYTWEET_TABLE_NAME);
                qb.setProjectionMap(tweetsProjections);
                break;

            case MYTWEETS_COUNT:
                sql = "SELECT count(*) FROM " + MYTWEET_TABLE_NAME;
                if (selection != null && selection.length() > 0) {
                    sql += " WHERE " + selection;
                }
                break;

            case MYTWEET_ID:
                qb.setTables(MYTWEET_TABLE_NAME);
                qb.setProjectionMap(tweetsProjections);
                qb.appendWhere(MyTweets._ID + "=" + uri.getPathSegments().get(1));
                break;


            default:
                throw new IllegalArgumentException("Unknown URI \"" + uri + "\"; matchedCode="
                        + matchedCode);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            switch (matchedCode) {
                case TWEETS:
                case TWEET_ID:
                case MENTIONS:
                case MENTION_TWEET_ID:
                case MYTWEETS:
                case MYTWEET_ID:
                    orderBy = Tweets.DEFAULT_SORT_ORDER;
                    break;

                case TWEETS_COUNT:
                case MENTIONS_COUNT:
                case MYTWEETS_COUNT:
                    orderBy = "";
                    break;

                default:
                    throw new IllegalArgumentException("Unknown URI \"" + uri + "\"; matchedCode="
                            + matchedCode);
            }
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = null;
        boolean logQuery = Log.isLoggable(TAG, Log.VERBOSE);
        try {
            if (sql.length() > 0) {
                c = db.rawQuery(sql, selectionArgs);
            } else {
                c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
            }
        } catch (Exception e) {
            logQuery = true;
            Logger.e(TAG, "Database query failed");
            e.printStackTrace();
        }

        if (logQuery) {
            if (sql.length() > 0) {
                Logger.v(TAG, "query, SQL=\"" + sql + "\"");
                if (selectionArgs != null && selectionArgs.length > 0) {
                    Log.v(TAG, "; selectionArgs=" + Arrays.toString(selectionArgs));
                }
            } else {
            	Logger.v(TAG, "query, uri=" + uri + "; projection=" + Arrays.toString(projection));
            	Logger.v(TAG, "; selection=" + selection);
            	Logger.v(TAG, "; selectionArgs=" + Arrays.toString(selectionArgs) + "; sortOrder=" + sortOrder);
            	Logger.v(TAG, "; qb.getTables=" + qb.getTables() + "; orderBy=" + orderBy);
            }
        }
        
        if (c != null) {
            // Tell the cursor what Uri to watch, so it knows when its source data changes
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }


    /**
     * Update a record in the database
     * 
     * @see android.content.ContentProvider#update(android.net.Uri,
     *      android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TWEETS:
                count = db.update(TWEETS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case TWEET_ID:
                String noteId = uri.getPathSegments().get(1);
                count = db.update(TWEETS_TABLE_NAME, values, Tweets._ID + "=" + noteId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
                
            case MENTIONS:
                count = db.update(MENTIONS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case MENTION_TWEET_ID:
                noteId = uri.getPathSegments().get(1);
                count = db.update(MENTIONS_TABLE_NAME, values, Mentions._ID + "=" + noteId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;

            case MYTWEETS:
                count = db.update(MYTWEET_TABLE_NAME, values, selection, selectionArgs);
                break;

            case MYTWEET_ID:
                noteId = uri.getPathSegments().get(1);
                count = db.update(MYTWEET_TABLE_NAME, values, MyTweets._ID + "=" + noteId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI \"" + uri + "\"");
        }

        return count;
    }

    // Static Definitions for UriMatcher and Projection Maps
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(AUTHORITY, TWEETS_TABLE_NAME, TWEETS);
        sUriMatcher.addURI(AUTHORITY, TWEETS_TABLE_NAME + "/#", TWEET_ID);
        sUriMatcher.addURI(AUTHORITY, TWEETS_TABLE_NAME + "/count", TWEETS_COUNT);
        
        sUriMatcher.addURI(AUTHORITY, MENTIONS_TABLE_NAME, MENTIONS);
        sUriMatcher.addURI(AUTHORITY, MENTIONS_TABLE_NAME + "/#", MENTION_TWEET_ID);
        sUriMatcher.addURI(AUTHORITY, MENTIONS_TABLE_NAME + "/count", MENTIONS_COUNT);
        
        sUriMatcher.addURI(AUTHORITY, MYTWEET_TABLE_NAME, MYTWEETS);
        sUriMatcher.addURI(AUTHORITY, MYTWEET_TABLE_NAME + "/#", MYTWEET_ID);
        sUriMatcher.addURI(AUTHORITY, MYTWEET_TABLE_NAME + "/count", MYTWEETS_COUNT);

        tweetsProjections = new HashMap<String, String>();
        tweetsProjections.put(Tweets._ID, Tweets._ID);
        tweetsProjections.put(AUTHOR_ID, AUTHOR_ID);
        tweetsProjections.put(IMG_URL, IMG_URL);
        tweetsProjections.put(IMAGE_BLOB, IMAGE_BLOB);
        tweetsProjections.put(MESSAGE, MESSAGE);
        tweetsProjections.put(SOURCE, SOURCE);
        tweetsProjections.put(TWEET_TYPE, TWEET_TYPE);
        tweetsProjections.put(IN_REPLY_TO_STATUS_ID, IN_REPLY_TO_STATUS_ID);
        tweetsProjections.put(IN_REPLY_TO_AUTHOR_ID, IN_REPLY_TO_AUTHOR_ID);
        tweetsProjections.put(FAVORITED, FAVORITED);
        tweetsProjections.put(SENT_DATE, SENT_DATE);
        tweetsProjections.put(CREATED_DATE, CREATED_DATE);

    }

    /**
     * Get MIME type of the content, used for the supplied Uri
     * 
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TWEETS:
            case TWEETS_COUNT:
                return Tweets.CONTENT_TYPE;

            case TWEET_ID:
                return Tweets.CONTENT_ITEM_TYPE;

            case MENTIONS:
            case MENTIONS_COUNT:
                return Mentions.CONTENT_TYPE;

            case MENTION_TWEET_ID:
                return Mentions.CONTENT_ITEM_TYPE;
                
            case MYTWEETS:
            case MYTWEETS_COUNT:
                return MyTweets.CONTENT_TYPE;

            case MYTWEET_ID:
                return MyTweets.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
	
}