package com.speedymovinginventory.speedyinventory.app;

import android.app.ActivityManager;
import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.speedymovinginventory.speedyinventory.toolbox.BitmapLruCache;

/**
 * Helper class that is used to provide references to initialized RequestQueue(s) and ImageLoader(s)
 *
 * @author Ognyan Bankov
 *
 */
public class MyVolley {
  private static RequestQueue requestQueue;
  private static ImageLoader imageLoader;


  private MyVolley() {
    // no instances
  }


  public static void init(Context context) {

    //HttpStack stack = new HttpClientStack(new MyHttpClient(context));

    requestQueue = Volley.newRequestQueue(context);


    int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
            .getMemoryClass();
    // Use 1/8th of the available memory for this memory cache.
    int cacheSize = 1024 * 1024 * memClass / 8;
    imageLoader = new ImageLoader(requestQueue, new BitmapLruCache(cacheSize));
  }


  public static RequestQueue getRequestQueue() {
    if (requestQueue != null) {
      return requestQueue;
    } else {
      throw new IllegalStateException("RequestQueue not initialized");
    }
  }


  /**
   * Returns instance of ImageLoader initialized with {@see UnrealImageCache} which effectively means
   * that no memory caching is used. This is useful for images that you know that will be show
   * only once.
   *
   * @return
   */
  public static ImageLoader getImageLoader() {
    if (imageLoader != null) {
      return imageLoader;
    } else {
      throw new IllegalStateException("ImageLoader not initialized");
    }
  }
}
