package com.chirpy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.chirpy.data.TweetDatabase;
import com.chirpy.data.TwitterManager;
import com.chirpy.util.Logger;
import com.chirpy.view.AuthActivity;
import com.chirpy.view.MainScreen;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Splash Screen
 *
 * @author dhavalmotghare@gmail.com
 */
public class Chirp extends Activity {

    /**
     * Log tag
     */
    private static final String TAG = TweetDatabase.class.getSimpleName();

    /**
     * Show the splash for sometime using this timer
     */
    private Timer timer = new Timer();
    /**
     * Splash duration
     */
    public static final int SPLASH_DURATION = 2000;
    /**
     * Check if already authenticated
     */
    private boolean alreadyAuthenticated = false;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        init();

        Logger.v(TAG, "Lets Chirp !!!");
        timer.schedule(new Task(this), SPLASH_DURATION);
        alreadyAuthenticated = isUserAuthenticated();
    }

    /**
     * Initialize twitter manager with shared preferences
     */
    private void init() {
        TwitterManager twitterManager = TwitterManager.getInstance();
        twitterManager.setPreferences(this);
    }

    /**
     * Check if already authenticated
     *
     * @return boolean
     */
    private boolean isUserAuthenticated() {
        TwitterManager twitterManager = TwitterManager.getInstance();
        return twitterManager.checkIfAuthenticated();
    }

    /**
     * Timer task to lauch the appropriate activity after the splash is shown
     *
     * @author dhavalmotghare@gmail.com
     */
    class Task extends TimerTask {

        Chirp parent;

        Task(Chirp parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            if (alreadyAuthenticated) {
                Intent intent = new Intent(parent, MainScreen.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(parent, AuthActivity.class);
                startActivity(intent);
            }
            finish();
        }

    }
}