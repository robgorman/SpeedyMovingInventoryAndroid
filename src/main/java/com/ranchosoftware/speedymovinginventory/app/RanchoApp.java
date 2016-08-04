package com.ranchosoftware.speedymovinginventory.app;

import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;
import android.view.View;

import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.User;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by rob on 7/13/16.
 */

public class RanchoApp extends MultiDexApplication {

  private static final String PreferencesName = "SpeedyMovingInventoryPrefs";

  private static final String storageUrl ="gs://speedymovinginventory.appspot.com";

  public static class LoginCredential{
    public String email;
    public String password;
    public LoginCredential(String email, String password){
      this.email = email;
      this.password = password;
    }
  }

  private final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yy");
  private final DateTimeFormatter preciseDateTimeFormatter = DateTimeFormat.forPattern("yyyy_MM_dd:HH:mm:ss:S");
  private final DateTimeFormatter imageDateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yy HH:mm:ss");

  private User currentUser;
  private Job currentJob;
  private String currentJobKey;

  private Item currentItem;
  private Item.Category currentCategory;

  @Override
  public void onCreate() {
    super.onCreate();

    init();
  }


  public void resetCredentials(){
    SharedPreferences prefs = getSharedPreferences(PreferencesName, 0);
    SharedPreferences.Editor edit = prefs.edit();
    edit.putString("email", "");
    edit.putString("password", "");
    edit.commit();
  }
  public void saveCredentials(LoginCredential cred){
    SharedPreferences prefs = getSharedPreferences(PreferencesName, 0);
    SharedPreferences.Editor edit = prefs.edit();
    edit.putString("email", cred.email);
    edit.putString("password", cred.password);
    edit.commit();

  }

  // may reutrn null
  public LoginCredential getSavedCredentials(){
    SharedPreferences prefs = getSharedPreferences(PreferencesName, 0);
    String email = prefs.getString("email", "");
    String password = prefs.getString("password", "");

    if (email.equals("")){
      return null;
    }

    return new LoginCredential(email, password);
  }



  private void init(){
    MyVolley.init(this);
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

//  public Job getCurrentJob() {
//    return currentJob;
//  }
//
//  public void setCurrentJob(Job currentJob) {
//    this.currentJob = currentJob;
//  }

  public DateTimeFormatter getDateTimeFormatter() {
    return dateTimeFormatter;
  }

  public DateTimeFormatter getPreciseDateTimeFormatter() {
    return preciseDateTimeFormatter;
  }

  public DateTimeFormatter getImageDateTimeFormatter() {
    return imageDateTimeFormatter;
  }

  public Item getCurrentItem() {
    return currentItem;
  }

  public void setCurrentItem(Item currentItem) {
    this.currentItem = currentItem;
  }

  public Item.Category getCurrentCategory() {
    return currentCategory;
  }

  public void setCurrentCategory(Item.Category currentCategory) {
    this.currentCategory = currentCategory;
  }

//  public String getCurrentJobKey() {
//    return currentJobKey;
//  }
//
//  public void setCurrentJobKey(String currentJobKey) {
//    this.currentJobKey = currentJobKey;
//  }

  public java.lang.String getStorageUrl() {
    return storageUrl;
  }
}
