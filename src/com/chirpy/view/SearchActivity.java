package com.chirpy.view;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import com.chirpy.R;
import com.chirpy.adapter.SearchAdapter;
import com.chirpy.data.TwitterManager;
import twitter4j.Tweet;

import java.util.List;

/**
 * Search Screen for searching public tweets.
 *
 * @author dhavalmotghare@gmail.com
 */
public class SearchActivity extends Activity {

    /**
     * References to needed objects
     */
    private EditText searchBox;
    private ListView searchResult;
    private ImageButton searchButton;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchlayout);
        searchBox = (EditText) findViewById(R.id.search_box);
        searchResult = (ListView) findViewById(R.id.search_result);
        searchButton = (ImageButton) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String searchQuery = searchBox.getText().toString();
                if (searchQuery == null || searchQuery.equals("")) {
                    Toast.makeText(getApplicationContext(), "Please enter a search query.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Searching Please wait..", Toast.LENGTH_LONG).show();
                    new SearchTask().execute(searchQuery);
                    hideKeyBoard();
                }
            }
        });
    }

    /**
     * Hide the keyboard when the user clicks the search button
     */
    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
    }

    /**
     * Search async task
     *
     * @author dhavalmotghare@gmail.com
     */
    class SearchTask extends AsyncTask<String, Void, List<Tweet>> {

        protected List<Tweet> doInBackground(String... query) {
            try {
                return TwitterManager.getInstance().search(query[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(List<Tweet> tweets) {

            if (tweets != null && tweets.size() > 0) {
                searchResult.setAdapter(new SearchAdapter(getApplicationContext(), tweets));
                Toast.makeText(getApplicationContext(), "Tweets found", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "No results found", Toast.LENGTH_LONG).show();
            }
        }
    }

}