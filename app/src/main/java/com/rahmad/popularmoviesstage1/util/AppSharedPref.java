package com.rahmad.popularmoviesstage1.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hartviq on 10/10/16.
 * inbox.rahmad@gmail.com
 * Copyright 2016
 */

public class AppSharedPref {
  private static final String FILENAME = "APP_SHARED_DATA";
  private final String isPopularState = "IS_POPULAR_STATE";
  private static AppSharedPref instance = null;

  private final SharedPreferences sharedPreferences;
  private final SharedPreferences.Editor editor;

  public static AppSharedPref getInstance(Context context) {
    if (instance == null) {
      instance = new AppSharedPref(context);
    }
    return instance;
  }

  public AppSharedPref(Context context) {
    sharedPreferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
    editor = sharedPreferences.edit();
  }

  public Boolean getIsPopularState() {
    return sharedPreferences.getBoolean(isPopularState, true);
  }

  public void setIsPopularState(Boolean isPopularState) {
    editor.putBoolean(this.isPopularState, isPopularState);
    editor.apply();
  }

  public void clear() {
    editor.clear();
    editor.commit();
  }

  public boolean commit() {
    return editor.commit();
  }
}
