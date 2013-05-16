package com.chirpy.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.IBinder;
import com.chirpy.R;
import com.chirpy.data.TweetDatabase;
import com.chirpy.data.TwitterManager;
import com.chirpy.util.Logger;
import com.chirpy.util.Util;
import com.chirpy.view.MainScreen;
import org.json.JSONException;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

import java.util.List;

/**
 * Service to manage the download and update of tweets. The service downloads the tweets and stores it in a shared
 * content provider. It stores the last downloaded tweet ID and only updates tweets after that ID.
 *
 * @author dhavalmotghare@gmail.com
 */
public class ChirpyService extends Service {

    /**
     * LOG TAG
     */
    private static final String TAG = ChirpyService.class.getSimpleName();

    public static final String TWEET_UPDATE_INTENT = "tweet_update";
    public static final String TWEET_UPDATE_INTENT_URI = "content://com.chirpy.service/";

    /**
     * Service states
     */
    public static final int STATUS_UPDATING = 0;
    public static final int STATUS_LASTUPDATE_FAILED = 1;
    public static final int STATUS_IDLE = 2;
    public static final int STATUS_UNKNOWN = -1;

    private int newTweets;
    private Uri mContentUri;
    private ContentResolver mContentResolver;
    private TwitterManager twitterManager = null;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onCreate()
     */
    public void onCreate() {
        super.onCreate();
        TwitterManager.getInstance().setServiceStatus(STATUS_IDLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (TwitterManager.getInstance().getServiceStatus() == STATUS_UPDATING
                || !TwitterManager.getInstance().checkIfAuthenticated()) {
            return START_STICKY;
        }

        new Thread() {
            public void run() {
                try {
                    if (Util.networkAvailable(getApplicationContext())) {
                        TwitterManager.getInstance().setServiceStatus(STATUS_UPDATING);
                        loadTimeline(TweetDatabase.TIMELINE_TYPE_FRIENDS);
                        loadTimeline(TweetDatabase.TIMELINE_TYPE_MENTIONS);
                        loadTimeline(TweetDatabase.TIMELINE_TYPE_MYTWEETS);
                    } else {
                        TwitterManager.getInstance().setServiceStatus(STATUS_IDLE);
                        Logger.d(TAG, " Unable to connect, No wifi available ");
                    }
                } catch (Exception e) {
                    TwitterManager.getInstance().setServiceStatus(STATUS_LASTUPDATE_FAILED);
                    e.printStackTrace();
                    Logger.d(TAG, " Something went wrong while syncing " + e.toString());
                }
            }

            ;
        }.start();
        return START_STICKY;
    }

    /**
     * Helper method to get the twitter manager instance
     *
     * @return TwitterManager
     */
    private TwitterManager getTwitterManager() {
        if (twitterManager == null) {
            twitterManager = TwitterManager.getInstance();
        }
        return twitterManager;
    }

    /**
     * Load time line for the supplied time-line type
     *
     * @param timelineType
     * @return boolean
     */
    public boolean loadTimeline(int timelineType) {
        boolean operationStatus = false;
        newTweets = 0;
        int limit = 200; // for now setting the limit to 200 tweets

        long lastId = getTwitterManager().getTweetID(timelineType);
        if (lastId >= 0)
            lastId = 1;

        long saveLastID = lastId;
        Paging paging = new Paging(1, limit, lastId);

        mContentResolver = getApplicationContext().getContentResolver();
        if (getTwitterManager().checkIfAuthenticated()) {
            List<Status> tweets = null;
            switch (timelineType) {
                case TweetDatabase.TIMELINE_TYPE_FRIENDS:
                    mContentUri = TweetDatabase.Tweets.CONTENT_URI;
                    tweets = getTwitterManager().getHomeTimeLine(paging);
                    break;
                case TweetDatabase.TIMELINE_TYPE_MENTIONS:
                    mContentUri = TweetDatabase.Mentions.CONTENT_URI;
                    tweets = getTwitterManager().getMentions(paging);
                    break;
                case TweetDatabase.TIMELINE_TYPE_MYTWEETS:
                    mContentUri = TweetDatabase.MyTweets.CONTENT_URI;
                    tweets = getTwitterManager().getUserTimeLine(paging);
                    break;
                default:
                    Logger.e(TAG, "Type not supported " + timelineType);
                    break;
            }
            if (tweets != null) {
                operationStatus = true;
                try {
                    for (Status status : tweets) {
                        long id = status.getId();
                        if (id > lastId) {
                            lastId = id;
                        }
                        insertTweet(status, timelineType);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (newTweets > 0) {
                mContentResolver.notifyChange(mContentUri, null);
                if (timelineType != TweetDatabase.TIMELINE_TYPE_MYTWEETS) {
                    clearNotification(timelineType);
                    sendNotification(timelineType, newTweets);
                }
            }
            if (lastId > saveLastID) {
                getTwitterManager().saveTweetID(lastId, timelineType);
            }
            sendBroadcast();

        }
        TwitterManager.getInstance().setServiceStatus(STATUS_IDLE);
        return operationStatus;
    }

    /**
     * Clear All Notifications
     */
    private void clearNotification(int type) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(type);
    }

    /**
     * Display a notification if there are new tweets
     *
     * @param type
     * @param noofTweets
     */
    @SuppressWarnings("deprecation")
    private void sendNotification(int type, int noofTweets) {

        if (type == TweetDatabase.TIMELINE_TYPE_MYTWEETS)
            return;

        final int icon = R.drawable.chirpy_icon_small;
        final String ns = Context.NOTIFICATION_SERVICE;
        final CharSequence tickerText = getString(R.string.app_name);

        final long when = System.currentTimeMillis();

        final Context context = getApplicationContext();
        CharSequence contentTitle = "";
        CharSequence contentText = "";

        switch (type) {
            case TweetDatabase.TIMELINE_TYPE_FRIENDS:
                contentTitle = getString(R.string.tweets);
                contentText = noofTweets + getString(R.string.new_text) + getString(R.string.tweets);
                break;
            case TweetDatabase.TIMELINE_TYPE_MENTIONS:
                contentTitle = getString(R.string.mentions);
                contentText = noofTweets + getString(R.string.new_text) + getString(R.string.mentions);
                break;
        }
        if (contentTitle == null || contentTitle.equals("") || contentText == null || contentText.equals(""))
            return;

        final Intent notificationIntent = new Intent(getApplicationContext(), MainScreen.class);
        notificationIntent.putExtra("Refresh", true);
        final PendingIntent contentIntent = PendingIntent
                .getActivity(getApplicationContext(), 0, notificationIntent, 0);

        final Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        mNotificationManager.notify(type, notification);
    }

    /**
     * Send a broadcast that we are done updating, it could either be a successful update or a failed update
     */
    private void sendBroadcast() {
        Uri uri = Uri.parse(TWEET_UPDATE_INTENT_URI);
        Intent intent = new Intent(TWEET_UPDATE_INTENT, uri);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Logger.d(TAG, "Broadcasting tweet intent! ");
        getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Insert a tweet
     *
     * @param status
     * @param type
     * @return
     * @throws JSONException
     * @throws SQLiteConstraintException
     */
    public Uri insertTweet(Status status, int type) throws JSONException, SQLiteConstraintException {
        ContentValues values = new ContentValues();

        Long lTweetId = status.getId();
        Uri aTweetUri = ContentUris.withAppendedId(mContentUri, lTweetId);
        String message = status.getText();

        try {
            switch (type) {
                case TweetDatabase.TIMELINE_TYPE_FRIENDS:
                case TweetDatabase.TIMELINE_TYPE_MENTIONS:
                case TweetDatabase.TIMELINE_TYPE_MYTWEETS:
                    User user = status.getUser();
                    values.put(TweetDatabase.Tweets._ID, lTweetId.toString());
                    values.put(TweetDatabase.AUTHOR_ID, user.getScreenName());
                    values.put(TweetDatabase.IMG_URL, user.getProfileImageURL().toString());
                    values.put(TweetDatabase.MESSAGE, message);
                    values.put(TweetDatabase.SOURCE, status.getSource());
                    values.put(TweetDatabase.TWEET_TYPE, type);
                    values.put(TweetDatabase.IN_REPLY_TO_STATUS_ID, status.getInReplyToStatusId());
                    values.put(TweetDatabase.IN_REPLY_TO_AUTHOR_ID, status.getInReplyToUserId());
                    values.put(TweetDatabase.FAVORITED, status.isFavorited());
                    break;
            }

            Long created = status.getCreatedAt().getTime();
            values.put(TweetDatabase.SENT_DATE, created);
        } catch (Exception e) {
            Logger.e(TAG, "inserttweet: " + e.toString());
        }

        if ((mContentResolver.update(aTweetUri, values, null, null)) == 0) {
            // There was no such row so add new one
            mContentResolver.insert(mContentUri, values);
            newTweets++;
        }
        return aTweetUri;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TwitterManager.getInstance().setServiceStatus(STATUS_IDLE);
    }

}
