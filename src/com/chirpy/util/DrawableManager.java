package com.chirpy.util;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;


/**
 * Drawable manager for downloading and maintaining the profile images
 *
 * @author dhavalmotghare@gmail.com
 */
public class DrawableManager {
    private final Map<String, Drawable> drawableMap;

    /**
     * No argument constructor
     */
    public DrawableManager() {
        drawableMap = new HashMap<String, Drawable>();
    }

    /**
     * Download the profile image.
     *
     * @param urlString
     * @return Drawable
     */
    private Drawable fetchDrawable(String urlString) {

        try {
            InputStream is = fetch(urlString);
            Drawable drawable = Drawable.createFromStream(new FlushedInputStream(is), "src");
            if (null != drawable) {
                drawableMap.put(urlString, drawable);
            }
            return drawable;
        } catch (MalformedURLException e) {
            Logger.e(this.getClass().getSimpleName(), "---- fetchDrawable() ---> " + urlString + " fetchDrawable failed" + e.toString());
            return null;
        } catch (IOException e) {
            Logger.e(this.getClass().getSimpleName(), "---- fetchDrawable() ---> " + urlString + " fetchDrawable failed" + e.toString());
            return null;
        }
    }

    /**
     * Fetch the profile image if not already present
     *
     * @param urlString
     * @param imageView
     */
    public void fetchDrawableOnThread(final String urlString,
                                      final ImageView imageView) {
        if (drawableMap.containsKey(urlString)) {
            imageView.setImageDrawable(drawableMap.get(urlString));
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    final Drawable drawable = fetchDrawable(urlString);
                    if (!drawableMap.containsKey(urlString)) {
                        drawableMap.put(urlString, drawable);
                    }
                    if (null != drawable) {
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageDrawable(drawable);
                            }
                        });
                    }
                }
            };
            thread.start();
        }
    }

    /**
     * Get the input stream for the URL
     *
     * @param urlString
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private InputStream fetch(String urlString) throws MalformedURLException,
            IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(urlString);
        HttpResponse response = httpClient.execute(request);
        return response.getEntity().getContent();
    }

}