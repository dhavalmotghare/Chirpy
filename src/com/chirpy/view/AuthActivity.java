package com.chirpy.view;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.signature.HmacSha1MessageSigner;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.chirpy.R;
import com.chirpy.data.TwitterManager;
import com.chirpy.util.Logger;

/**
 * Activity to authenticate the user.
 * 
 * @author dhavalmotghare@gmail.com
 * 
 */
public class AuthActivity extends Activity {

	/** LOG TAG */
	private static final String TAG = AuthActivity.class.getSimpleName();

	/** Hold reference to needed objects */
	private static CommonsHttpOAuthConsumer commonHttpOAuthConsumer;
	private static OAuthProvider authProvider;
	private Context context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authlayout);
		Button setUpAccount = (Button) findViewById(R.id.setupbutton);
		this.context = this;

		commonHttpOAuthConsumer = new CommonsHttpOAuthConsumer(
				TwitterManager.CONSUMER_KEY, TwitterManager.CONSUMER_SECRET);
		commonHttpOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
		authProvider = new CommonsHttpOAuthProvider(
				TwitterManager.TWITTER_TOKEN_REQUEST_URL,
				TwitterManager.TWITTER_TOKEN_ACCESS_URL,
				TwitterManager.TWITTER_AUTH_URL);
		authProvider.setOAuth10a(true);

		setUpAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new RetrieveRequestToken().execute(context);// retrieve the request token
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	protected void onStart() {
		super.onStart();
		Logger.d(TAG, " On Start - " + TAG);
	}

	/**
	 * Retrieve Request Token task.
	 */
	private class RetrieveRequestToken extends
			AsyncTask<Context, String, String> {

		private Context context;// store the context.

		@Override
		protected String doInBackground(Context... params) {
			this.context = params[0];
			try {
				return authProvider.retrieveRequestToken(
						commonHttpOAuthConsumer, TwitterManager.CALLBACK_URL);
			} catch (Exception e) {
				Logger.e(TAG,"Error while trying to launch Twitter Authentication - " + e.toString());
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {

			Logger.d(TAG, "Twitter OAuth URL: " + result);
			if (!TextUtils.isEmpty(result)) {
				/** saving the token */
				RequestToken requestToken = new RequestToken(
						commonHttpOAuthConsumer.getToken(),
						commonHttpOAuthConsumer.getTokenSecret());
				TwitterManager.getInstance().saveRequestToken(requestToken);
				this.context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse(result)));// launching the browser
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();
		Uri uri = this.getIntent().getData();
		Logger.d(TAG, " On Resume - " + TAG);
		Logger.d(TAG, " URI is - " + uri);

		if (uri != null) {
			retreiveAndStoreToken(uri);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		Logger.d(TAG, " New Intent for - " + TAG);
		Logger.d(TAG, " URI is - " + uri);

		if (uri != null) {
			retreiveAndStoreToken(uri);
		}
	}

	/**
	 * Retrieve the token from the URI
	 * 
	 * @param uri
	 */
	private void retreiveAndStoreToken(Uri uri) {

		if (uri != null
				&& uri.toString().startsWith(TwitterManager.CALLBACK_URL)) {
			String verifier = uri
					.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			try {
				RequestToken requestToken = TwitterManager.getInstance().getRequestToken();
				commonHttpOAuthConsumer.setTokenWithSecret(
						requestToken.getToken(), requestToken.getTokenSecret());
				/**
				 * retrieve the access token from the consumer and the OAuth
				 * verifier returner by the Twitter Callback URL
				 */
				new RetrieveAccessToken(verifier).execute(context);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getBaseContext(), e.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Retrieve Access Token task.
	 */
	private class RetrieveAccessToken extends
			AsyncTask<Context, String, Boolean> {

		/**
		 * The Twitter OAuth verifier.
		 */
		private String verifier;

		/**
		 * Default constructor.
		 * 
		 * @param oauth_verifier
		 *            Twitter OAuth verifier
		 */
		public RetrieveAccessToken(String verifier) {
			this.verifier = verifier;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		protected Boolean doInBackground(Context... params) {
			try {
				authProvider.retrieveAccessToken(commonHttpOAuthConsumer,verifier);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getBaseContext(), e.getMessage(),
						Toast.LENGTH_LONG).show();
				return false;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(Boolean result) {
			if (result) {
				AccessToken accessToken = new AccessToken(
						commonHttpOAuthConsumer.getToken(),
						commonHttpOAuthConsumer.getTokenSecret());

				Logger.d(TAG, " Access Token - " + accessToken);
				Logger.d(TAG, " Token - " + commonHttpOAuthConsumer.getToken());
				Logger.d(TAG, " Token Secret- " + commonHttpOAuthConsumer.getTokenSecret());

				TwitterManager.getInstance().storeAccessToken(accessToken);
				TwitterManager.getInstance().setAuthenticated(true);
				TwitterManager.getInstance().loadAccountData();
				Intent intent = new Intent(getApplicationContext(),
						MainScreen.class);
				startActivity(intent);
				finish();

			} else {
				Toast.makeText(getBaseContext(), "Failed to authorize !!",
						Toast.LENGTH_LONG).show();
			}
		}

	}

}