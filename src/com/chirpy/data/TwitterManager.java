package com.chirpy.data;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import twitter4j.Tweet;

/**
 * Twitter manager for managing all twitter specific operations
 * 
 * @author dhavalmotghare@gmail.com
 * 
 */
public class TwitterManager {

	/** Twitter constants */
	public final static String CONSUMER_KEY = "Your key here";
	public final static String CONSUMER_SECRET = "Your secret here";
	public final static String CALLBACK_URL = "chirpy://OAuthTwitter";

	/** Twitter URLS */
	public final static String TWITTER_TOKEN_REQUEST_URL = "http://twitter.com/oauth/request_token";
	public final static String TWITTER_TOKEN_ACCESS_URL = "http://twitter.com/oauth/access_token";
	public final static String TWITTER_AUTH_URL = "http://twitter.com/oauth/authorize";

	/** Shared preferences */
	private static final String PREFS_FILE = "chirpy_settings";

	/** Shared preferences keys|settings */
	public static final String KEY_VERIFIED = "verified";
	public static final String KEY_USER_ID = "userID";
	public static final String KEY_TWITTER_USERNAME = "username";
	public static final String KEY_TWITTER_ACCESS_TOKEN = "accesstoken";
	public static final String KEY_TWITTER_ACCESS_TOKEN_SECRET = "accesstokenSecret";
	public static final String KEY_TWITTER_REQUEST_TOKEN = "requesttoken";
	public static final String KEY_TWITTER_REQUEST_TOKEN_SECRET = "requesttokenSecret";
	public static final String KEY_LIMIT = "limit";
	public static final String KEY_LAST_TIME = "time";
	public static final String KEY_FREQUENCY = "frequency";
	public static final String KEY_TWEETS_LAST_ID = "lastid";
	public static final String KEY_SERVICE_STATUS = "status";

	/** Twitter references */
	private Twitter twitter;
	/** Shared Preferences settings */
	private SharedPreferences settings;

	/**
	 * Private constructor for single reference
	 * 
	 */
	private TwitterManager() {

	}

	/**
	 * Holder pattern for single instance
	 * 
	 * @author dhavalmotghare@gmail.com
	 * 
	 */
	private static class LazyHolder {
		private static TwitterManager instance = new TwitterManager();
	}

	/**
	 * Static get instance method for getting the instance
	 * 
	 * @return TwitterManager
	 */
	public static TwitterManager getInstance() {
		return LazyHolder.instance;
	}

	@SuppressLint("WorldReadableFiles")
	public void setPreferences(Context context) {
		this.settings = context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_WORLD_READABLE);
	}

	/**
	 * Check if the user is authenticated
	 * 
	 * @return boolean
	 */
	public boolean checkIfAuthenticated() {
		boolean verified = false;
		if (settings != null) {
			verified = settings.getBoolean(KEY_VERIFIED, false);
		}
		return verified;
	}

	/**
	 * Set that the status if authenticated or not
	 * 
	 * @param value
	 */
	public void setAuthenticated(boolean value) {
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(KEY_VERIFIED, value);
			editor.commit();
		}
	}

	/**
	 * Store the access token
	 * 
	 */
	public void storeAccessToken(AccessToken at) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(KEY_USER_ID, at.getUserId());
		editor.putString(KEY_TWITTER_ACCESS_TOKEN, at.getToken());
		editor.putString(KEY_TWITTER_ACCESS_TOKEN_SECRET, at.getTokenSecret());
		editor.putString(KEY_TWITTER_USERNAME, at.getScreenName());
		editor.commit();
	}

	/**
	 * load the account details
	 * 
	 */
	public void loadAccountData() {
		try {
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
			AccessToken accessToken = loadAccessToken();
			twitter.setOAuthAccessToken(accessToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load and return the access token
	 * 
	 * @return AccessToken
	 */
	private AccessToken loadAccessToken() {
		String token = settings.getString(KEY_TWITTER_ACCESS_TOKEN, "");
		String tokenSecret = settings.getString(KEY_TWITTER_ACCESS_TOKEN_SECRET, "");
		return new AccessToken(token, tokenSecret);
	}

	/**
	 * Get request token
	 * 
	 * @return RequestToken
	 */
	public RequestToken getRequestToken() {
		String token = settings.getString(KEY_TWITTER_REQUEST_TOKEN, "");
		String tokenSecret = settings.getString(KEY_TWITTER_REQUEST_TOKEN_SECRET, "");
		return new RequestToken(token, tokenSecret);
	}

	/**
	 * Save the request token
	 * 
	 * @param requestToken
	 */
	public void saveRequestToken(RequestToken requestToken) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(KEY_TWITTER_REQUEST_TOKEN, requestToken.getToken());
		editor.putString(KEY_TWITTER_REQUEST_TOKEN_SECRET, requestToken.getTokenSecret());
		editor.commit();
	}

	/**
	 * Get twitter instance
	 * 
	 * @return Twitter
	 */
	public Twitter getTwitter() {
		return twitter;
	}

	/**
	 * Verify credentials for the save details
	 * 
	 * @return boolean
	 * @throws TwitterException
	 */
	public boolean verifyCredentials() throws TwitterException {
		try {
			if (twitter == null) {
				twitter = new TwitterFactory().getInstance();
			}
			loadAccountData();
			return (twitter.verifyCredentials() != null);
		} catch (TwitterException e) {
			if (e.getStatusCode() == 401) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(KEY_VERIFIED, false);
				editor.commit();
			} else {
				throw e;
			}
		}
		return false;
	}

	/**
	 * Get the home time line for the verified user
	 * 
	 * @return List<Status>
	 */
	public List<Status> getHomeTimeLine() {
		try {
			if (twitter == null)
				loadAccountData();
			return twitter.getHomeTimeline();
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the home time line for the verified user
	 * 
	 * @param paging
	 * @return List<Status>
	 */
	public List<Status> getHomeTimeLine(Paging paging) {
		try {
			if (twitter == null)
				loadAccountData();
			return twitter.getHomeTimeline(paging);
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the user time line for the verified user
	 * 
	 * @return List<Status>
	 */
	public List<Status> getUserTimeLine() {
		try {
			if (twitter == null)
				loadAccountData();
			return twitter.getUserTimeline();
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the user time line for the verified user
	 * 
	 * @param paging
	 * @return List<Status>
	 */
	public List<Status> getUserTimeLine(Paging paging) {
		try {
			if (twitter == null)
				loadAccountData();
			return twitter.getUserTimeline(paging);
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get mentions for the verified user
	 * 
	 * @return List<Status>
	 */
	public List<Status> getMentions() {
		try {
			if (twitter == null)
				loadAccountData();
			return twitter.getMentions();
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get mentions for the verified user
	 * 
	 * @param paging
	 * @return List<Status>
	 */
	public List<Status> getMentions(Paging paging) {
		try {
			if (twitter == null)
				loadAccountData();
			return twitter.getMentions(paging);
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Save the last tweet ID
	 * 
	 * @param statusID
	 * @param timelineType
	 */
	public void saveTweetID(long statusID, int timelineType) {
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putLong(KEY_TWEETS_LAST_ID + timelineType, statusID);
			editor.commit();
		}
	}

	/**
	 * Get the last saved ID
	 * 
	 * @param timelineType
	 * @return long
	 */
	public long getTweetID(int timelineType) {
		long lastID = 0;
		if (settings != null) {
			lastID = settings.getLong(KEY_TWEETS_LAST_ID + timelineType, 0);
		}
		return lastID;
	}

	/**
	 * Get user name for the verified user
	 * 
	 * @return String
	 */
	public String getUserName() {
		try {
			String userName = settings.getString(KEY_TWITTER_USERNAME, "Not Available");
			return userName;
		} catch (Exception e) {
			e.printStackTrace();
			return "Not Available";
		}
	}

	/**
	 * Set the updating service status
	 * 
	 * @param value
	 */
	public void setServiceStatus(int value) {
		if (settings != null) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(KEY_SERVICE_STATUS, value);
			editor.commit();
		}
	}

	/**
	 * Get the service status
	 * 
	 * @return int
	 */
	public int getServiceStatus() {
		int value = -1;
		if (settings != null) {
			value = settings.getInt(KEY_SERVICE_STATUS, -1);
		}
		return value;
	}

	/**
	 * Update status for the verified user
	 * 
	 * @param status
	 * @return boolean
	 */
	public boolean updateStatus(String status) {
		try {
			if (twitter == null)
				loadAccountData();
			return (twitter.updateStatus(status) != null);
		} catch (TwitterException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Search the tweets for the passed search query
	 * 
	 * @param searchQuery
	 * @return List<Tweet>
	 */
	public List<Tweet> search(String searchQuery) {
		try {
			if (twitter == null)
				loadAccountData();
			Query query = new Query();
			query.setQuery(searchQuery);
			QueryResult queryResult = twitter.search(query);
			return queryResult.getTweets();
		} catch (TwitterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Delete shared preferences 
	 * 
	 * @return boolean
	 */
	public boolean deleteSettings() {
		try {
			if (settings != null) {
				SharedPreferences.Editor editor = settings.edit();
				editor.clear();
				editor.commit();
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
