package com.chirpy.view;

import java.util.ArrayList;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Page adapter for displaying the three time lines
 * 
 * @author dhavalmotghare@gmail.com
 *
 */
public class TweetPagerAdapter extends PagerAdapter {

	/** Store the views in a list*/
	ArrayList<View> allviews;
	/** Store the current position of the list*/
	private int currentposition;

	/**
	 * No argument constructor
	 */
	public TweetPagerAdapter() {
		if (allviews == null || allviews.size() == 0) {
			throw new NullPointerException("no view set for adapter");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#getItemPosition(java.lang.Object)
	 */
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	/**
	 * TweetPagerAdapter constructor
	 *  
	 * @param ctx
	 * @param views
	 */
	public TweetPagerAdapter(Context ctx, ArrayList<View> views) {
		if (views == null || views.size() == 0) {
			throw new NullPointerException("no view set for adapter");
		}
		allviews = views;
	}

	/**
	 * Get the current item
	 * 
	 * @return Object
	 */
	public Object getCurrentItem() {
		return allviews.get(currentposition);
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#instantiateItem(android.view.View, int)
	 */
	public Object instantiateItem(View collection, int position) {
		currentposition = position;
		((ViewPager) collection).addView(allviews.get(currentposition));
		return allviews.get(currentposition);
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#destroyItem(android.view.View, int, java.lang.Object)
	 */
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#isViewFromObject(android.view.View, java.lang.Object)
	 */
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#finishUpdate(android.view.View)
	 */
	public void finishUpdate(View arg0) {

	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#getCount()
	 */
	public int getCount() {
		return allviews.size();
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#restoreState(android.os.Parcelable, java.lang.ClassLoader)
	 */
	public void restoreState(Parcelable arg0, ClassLoader arg1) {

	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#saveState()
	 */
	public Parcelable saveState() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.view.PagerAdapter#startUpdate(android.view.View)
	 */
	public void startUpdate(View arg0) {

	}

}
