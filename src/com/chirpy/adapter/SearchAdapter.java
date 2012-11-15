package com.chirpy.adapter;

import java.util.List;

import twitter4j.Tweet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chirpy.R;
import com.chirpy.util.DrawableManager;

/**
 * List adapter to show the search results
 * 
 * @author dhavalmotghare@gmail.com
 */
public class SearchAdapter extends BaseAdapter {
	
	/** Context reference */
	private Context context;
	/** list holding the search results */
	private List<Tweet> searchResult;
	/** DrawableManager to download profile images */
	private DrawableManager drawableManager = new DrawableManager();

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param searchResult
	 */
	public SearchAdapter(final Context context, List<Tweet> searchResult) {
		super();
		this.searchResult = searchResult;
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			final LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = vi.inflate(R.layout.row, null);
		}

		Tweet tweet = (Tweet) getItem(position);
		final ImageView profileImage = (ImageView) row.findViewById(R.id.tweet_img);
		drawableManager.fetchDrawableOnThread(tweet.getProfileImageUrl(),profileImage);

		final TextView message = (TextView) row.findViewById(R.id.tweet_text);
		message.setText(tweet.getText());

		return row;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	public int getCount() {
		return searchResult.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	public Object getItem(final int index) {
		return searchResult.get(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	public long getItemId(final int index) {
		return index;
	}
}
