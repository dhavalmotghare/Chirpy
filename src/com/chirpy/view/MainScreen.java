package com.chirpy.view;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.chirpy.R;
import com.chirpy.data.TweetDatabase;
import com.chirpy.data.TwitterManager;
import com.chirpy.service.AlarmReceiver;
import com.chirpy.service.ChirpyService;
import com.chirpy.util.DrawableManager;
import com.chirpy.util.Logger;

public class MainScreen extends Activity implements OnItemClickListener,
		ActionBar.TabListener {

	/** LOG TAG */
	private static final String TAG = ChirpyService.class.getSimpleName();

	/** Table columns to use for the tweets data */
	private static final String[] PROJECTION = new String[] {
			TweetDatabase.Tweets._ID, TweetDatabase.AUTHOR_ID,
			TweetDatabase.MESSAGE, TweetDatabase.IMG_URL,
			TweetDatabase.SENT_DATE };
	
	/** from and to fields */
	private static final String[] FROM = { TweetDatabase.IMG_URL,TweetDatabase.MESSAGE }; 
	private static final int[] TO = { R.id.tweet_img, R.id.tweet_text };

	/** tabs, action bar and menu item */
	private ViewPager tabs;
	private ActionBar actionBar;
	private MenuItem menuRefresh;
	private ProgressBar progressBar;

	/** Time line lists */
	private ListView timeLine;
	private ListView myUpdates;
	private ListView mentions;

	/** Cursors for the three time line lists */
	protected Cursor mCursor;
	protected Cursor mentionCursor;
	protected Cursor myTweetsCursor;

	/** flag to check if we are updating already */
	private boolean updating = false;
	private BroadcastReceiver dbUpdateReceiver;

	private DrawableManager drawableManager = new DrawableManager();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		createTimelineList();

		LayoutInflater inflater = this.getLayoutInflater();
		progressBar = (ProgressBar) inflater.inflate(R.layout.progress_action,null);

		setContentView(R.layout.main_screen_layout);
		this.tabs = (ViewPager) this.findViewById(R.id.ViewsPager);
		actionBar = getActionBar();

		TwitterManager.getInstance().setServiceStatus(ChirpyService.STATUS_IDLE);

		ArrayList<View> appViews = new ArrayList<View>();
		appViews.add(timeLine);
		appViews.add(mentions);
		appViews.add(myUpdates);

		TweetPagerAdapter pagerAdapter = new TweetPagerAdapter(this, appViews);
		tabs.setAdapter(pagerAdapter);

		tabs.setOnPageChangeListener(new OnPageChangeListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected
			 * (int)
			 */
			public void onPageSelected(int position) {
				actionBar.selectTab(actionBar.getTabAt(position));
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled
			 * (int, float, int)
			 */
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.support.v4.view.ViewPager.OnPageChangeListener#
			 * onPageScrollStateChanged(int)
			 */
			public void onPageScrollStateChanged(int arg0) {

			}
		});

		refreshData(); // refresh the data when freshly launched
		updateLists(); // update the time line list
		cancelRepeatingAlarm(); // cancel any repeating alarms
		scheduleRepeatingAlarm();
	}

	private void createTimelineList() {
		timeLine = new ListView(MainScreen.this);
		myUpdates = new ListView(MainScreen.this);
		mentions = new ListView(MainScreen.this);
	}

	/**
	 * Register the receiver for receiving the database update broadcast
	 * 
	 */
	private void registerUpdateReceiver() {
		// update the list when the database has been updated
		dbUpdateReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {

				// Check if the update failed.
				if (intent.getAction().equals(ChirpyService.TWEET_UPDATE_INTENT)) {
					if (!updating) {
						updateLists();
					}
				}
			}
		};

		IntentFilter filter = getUpdateIntentFilter(ChirpyService.TWEET_UPDATE_INTENT);
		registerReceiver(dbUpdateReceiver, filter);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	protected void onStop() {
		unregisterReceiver(dbUpdateReceiver);
		super.onStop();
	}

	/**
	 * Update all lists
	 * 
	 */
	private void updateLists() {

		updating = true; // set updating to true when we start updating
		String sortOrder = TweetDatabase.Tweets.DEFAULT_SORT_ORDER;
		Uri contentUri = TweetDatabase.Tweets.CONTENT_URI;
		String selection = TweetDatabase.TWEET_TYPE + " IN (?, ?) ";
		mCursor = getContentResolver().query(
				contentUri,PROJECTION,selection,
				new String[] {
						String.valueOf(TweetDatabase.TIMELINE_TYPE_FRIENDS),
						String.valueOf(TweetDatabase.TIMELINE_TYPE_MENTIONS) },
				sortOrder);

		contentUri = TweetDatabase.Mentions.CONTENT_URI;
		mentionCursor = getContentResolver().query(contentUri, PROJECTION, null, null, sortOrder);

		contentUri = TweetDatabase.MyTweets.CONTENT_URI;
		myTweetsCursor = getContentResolver().query(contentUri, PROJECTION, null, null, sortOrder);

		Logger.i(" cursor count ->", mCursor.getCount() + "");

		timeLine.setAdapter(getCusorAdapter(mCursor));
		mentions.setAdapter(getCusorAdapter(mentionCursor));
		myUpdates.setAdapter(getCusorAdapter(myTweetsCursor));
		updating = false; // set updating to false when we are done updating

	}

	/**
	 * Get a simple cursor adapter for the passed cursor
	 * 
	 * @param cursor
	 * @return SimpleCursorAdapter
	 */
	private SimpleCursorAdapter getCusorAdapter(Cursor cursor) {
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.row, cursor, FROM, TO);
		adapter.setViewBinder(new TimeLineViewBinder());
		return adapter;
	}

	/**
	 * View binder for downloading the profile image
	 * 
	 */
	class TimeLineViewBinder implements SimpleCursorAdapter.ViewBinder {

		/**
		 * Binds the Cursor column defined by the specified index to the
		 * specified view
		 */
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (cursor.getCount() > 0) {
				if (view.getId() == R.id.tweet_img) {
					String url = cursor.getString(cursor.getColumnIndex(TweetDatabase.IMG_URL));
					drawableManager.fetchDrawableOnThread(url,
							((ImageView) view));
					return true; // true because the data was bound to the view
				}
			}
			return false;
		}
	};

	/**
	 * Set the call back intent
	 * 
	 * @param action
	 * @return Intent
	 */
	public static IntentFilter getUpdateIntentFilter(String action) {
		IntentFilter filter = new IntentFilter(action);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		filter.addDataScheme("content");
		filter.addDataAuthority("com.chirpy.service", null);
		return filter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	protected void onStart() {
		super.onStart();
		actionBar = this.getActionBar();
		setupActionBar();
		setupTabs();
		registerUpdateReceiver();
	}

	/**
	 * This method does initialization of ActionBar
	 */
	private void setupActionBar() {
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setBackgroundDrawable(new ColorDrawable(0xFF6B6B6B));
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.show();
	}

	/**
	 * Set up tabs for the application
	 * 
	 */
	private void setupTabs() {
		/**
		 * order of tabs MODE_TIMELINE = 0; MODE_MENTIONS = 1;MODE_MY_TWEETS =
		 * 2;
		 */
		if (actionBar != null && actionBar.getTabCount() == 0) {
			actionBar.addTab(actionBar.newTab()
					.setText(this.getResources().getString(R.string.timeline))
					.setTabListener(this));
			actionBar.addTab(actionBar.newTab()
					.setText(this.getResources().getString(R.string.mentions))
					.setTabListener(this));
			actionBar.addTab(actionBar.newTab()
					.setText(this.getResources().getString(R.string.mytweets))
					.setTabListener(this));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			if (menuRefresh == null)
				menuRefresh = item;
			refreshData();
			return true;
		case R.id.menu_delete:
			confirmDelete();
			return true;
		case R.id.menu_search:
			launchSearch();
			return true;
		case R.id.menu_tweet:
			showTweetDialog();
			return true;

		}
		return super.onOptionsItemSelected(item);

	}

	/**
	 * Confirm account delete
	 */
	private void confirmDelete() {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.delete_account)
				.setMessage(R.string.delete_account_msg)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							/*
							 * (non-Javadoc)
							 * 
							 * @see
							 * android.content.DialogInterface.OnClickListener
							 * #onClick(android.content.DialogInterface, int)
							 */
							public void onClick(DialogInterface dialog,int which) {
								deleteAccount();
							}

						}).setNegativeButton(R.string.no, null).show();
	}

	/**
	 * Delete the account
	 * 
	 * @return
	 */
	private boolean deleteAccount() {
		boolean accountDeleteStatus = true;
		String message = "Account deleted successfully";
		String error = "There was some problem in deleting the account - ";
		ContentResolver contentResolver = getApplicationContext().getContentResolver();
		try {
			contentResolver.delete(TweetDatabase.Tweets.CONTENT_URI, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			error += e.toString();
			accountDeleteStatus = false;
		}
		try {
			contentResolver.delete(TweetDatabase.Mentions.CONTENT_URI, null,null);
		} catch (Exception e) {
			e.printStackTrace();
			error += e.toString();
			accountDeleteStatus = false;
		}
		try {
			contentResolver.delete(TweetDatabase.MyTweets.CONTENT_URI, null,null);
		} catch (Exception e) {
			e.printStackTrace();
			error += e.toString();
			accountDeleteStatus = false;
		}
		try {
			TwitterManager.getInstance().deleteSettings();
		} catch (Exception e) {
			e.printStackTrace();
			error += e.toString();
			accountDeleteStatus = false;
		}
		if (accountDeleteStatus) {
			showMessage(message);
			Intent intent = new Intent(this, AuthActivity.class);
			startActivity(intent);
			finish();
		} else {
			showMessage(error);
		}
		clearNotifications();
		cancelRepeatingAlarm();
		return accountDeleteStatus;
	}

	/**
	 * Clear All Notification when this screen is shown
	 */
	private void clearNotifications() {
		NotificationManager notificationManager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

	}

	/**
	 * Launch the search activity
	 * 
	 */
	private void launchSearch() {
		Intent intent = new Intent(this, SearchActivity.class);
		startActivity(intent);
	}

	/**
	 * Show the update tweet dialog
	 * 
	 */
	public void showTweetDialog() {

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();

		/**
		 * Inflate and set the layout for the dialog Pass null as the parent
		 * view because its going in the dialog layout
		 */
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.tweet_dailog, null);
		alertDialog.setTitle(R.string.update_status);
		alertDialog.setView(layout);

		final EditText editText = (EditText) layout.findViewById(R.id.status_text);

		alertDialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

					}
				});

		alertDialog.setPositiveButton(
				getResources().getString(R.string.update),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String status = editText.getText().toString();
						new UpdateStatus().execute(status);
					}
				});

		alertDialog.show();

	}

	/**
	 * Show a toast for the message
	 * 
	 * @param message
	 */
	private void showMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG)
				.show();
	}

	/**
	 * Async task to update the status
	 * 
	 * @author dhavalmotghare@gmail.com
	 * 
	 */
	class UpdateStatus extends AsyncTask<String, Void, Boolean> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		protected Boolean doInBackground(String... status) {
			try {
				return TwitterManager.getInstance().updateStatus(status[0]);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(Boolean result) {
			if (result) {
				Toast.makeText(getApplicationContext(),"Status updated successfully.", Toast.LENGTH_LONG).show();
				refreshData();
			} else {
				Toast.makeText(getApplicationContext(),"There was some problem in updating the status.",Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Either we are launching the application or the user has asked us to
	 * refresh the data
	 * 
	 */
	private void refreshData() {
		int status = TwitterManager.getInstance().getServiceStatus();
		if (status != ChirpyService.STATUS_UPDATING) {
			Intent serviceIntent = new Intent(this, ChirpyService.class);
			getApplicationContext().startService(serviceIntent);
			showMessage("Updating Please wait");
		}
	}

	/**
	 * Starts the repeating Alarm.
	 */
	private boolean scheduleRepeatingAlarm() {
		final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		final PendingIntent pendingIntent = getRepeatingIntent();
		final int frequency = 1000 * 60 * 5;
		final long firstTime = SystemClock.elapsedRealtime() + frequency;
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,frequency, pendingIntent);
		Logger.d(TAG, "Started repeating alarm in a " + frequency + "ms rhythm.");
		return true;
	}

	/**
	 * Cancels the repeating Alarm that sends the fetch Intent.
	 */
	private boolean cancelRepeatingAlarm() {
		final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		final PendingIntent pendingIntent = getRepeatingIntent();
		am.cancel(pendingIntent);
		Logger.d(TAG, "Cancelled repeating alarm.");
		return true;
	}

	/**
	 * Returns Intent to be send with Repeating Alarm. This alarm will be
	 * received by {@link AlarmReceiver}
	 * 
	 * @return the Intent
	 */
	private PendingIntent getRepeatingIntent() {
		final Intent intent = new Intent(getApplicationContext(),AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,intent, 0);
		return pendingIntent;
	}

	/**
	 * Set the selected tab mode
	 * 
	 * @param newMode
	 */
	public void setMode(int newMode) {
		tabs.setCurrentItem(newMode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
	 * .AdapterView, android.view.View, int, long)
	 */
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.ActionBar.TabListener#onTabReselected(android.app.ActionBar
	 * .Tab, android.app.FragmentTransaction)
	 */
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.ActionBar.TabListener#onTabUnselected(android.app.ActionBar
	 * .Tab, android.app.FragmentTransaction)
	 */
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.app.ActionBar.TabListener#onTabSelected(android.app.ActionBar
	 * .Tab, android.app.FragmentTransaction)
	 */
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		setMode(tab.getPosition());
	}

}
