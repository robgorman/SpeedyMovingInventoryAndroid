package com.ranchosoftware.speedymovinginventory.app;

import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.R;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObject;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObjectEventListener;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.MovingItemDataDescription;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.server.Server;

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;

/**
 * Created by rob on 7/13/16.
 */

public class RanchoApp extends MultiDexApplication {

  private static final String TAG = RanchoApp.class.getSimpleName();

  private static final String PreferencesName = "SpeedyMovingInventoryPrefs";

  private String storageUrl;

  private Server mailServer;

  private Map<String, ArrayList<MovingItemDataDescription>> movingItemDescriptions = new HashMap<>();


  public static class LoginCredential{
    public String email;
    public String password;
    public LoginCredential(String email, String password){
      this.email = email;
      this.password = password;
    }
  }

  private final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("MM/dd/yy");
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yy HH:mm a");

  private final DateTimeFormatter preciseDateTimeFormatter = DateTimeFormat.forPattern("yyyy_MM_dd:HH:mm:ss:S");
  private final DateTimeFormatter imageDateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yy HH:mm:ss");

  private User currentUser;
  private Company currentCompany;

  private Item currentItem;
  private Item.Category currentCategory;

 // private Configuration mailgunConfiguration;

  @Override
  public void onCreate() {
    super.onCreate();

    init();
    mailServer = new Server("https://speedymovinginventory.appspot.com");
    //mailServer = new Server("http://localhost:8080");
//
//    mailgunConfiguration = new Configuration()
//            .domain("ranchosoftware.com")
//            .apiKey("key-c90fa773c9d000ce3cd38a903368ee7b")
//            .from("Speedy Moving Inventory", "speedy@ranchosoftware.com");
    storageUrl = "gs://" + getString(R.string.google_storage_bucket);
    loadMovingItemDescriptions();
  }


  private ChildEventListener itemListForRoomListener = new ChildEventListener(){
    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

      MovingItemDataDescription itemDataDescription = dataSnapshot.getValue(MovingItemDataDescription.class);

      ArrayList<MovingItemDataDescription> itemsSoFar = movingItemDescriptions.get(itemDataDescription.getRoom());
      if (itemsSoFar == null){
        itemsSoFar = new ArrayList<MovingItemDataDescription>();
      }
      itemsSoFar.add(itemDataDescription);
      movingItemDescriptions.put(itemDataDescription.getRoom(), itemsSoFar);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  };



  private void loadMovingItemDescriptions(){
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference roomLists = database.getReference("speedyMovingItemDataDescriptions/");
    roomLists.addChildEventListener(new ChildEventListener(){
      @Override
      public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        final DatabaseReference itemListForRoom =
                database.getReference("speedyMovingItemDataDescriptions/" + dataSnapshot.getKey());
        itemListForRoom.addChildEventListener(itemListForRoomListener);

      }

      @Override
      public void onChildChanged(DataSnapshot dataSnapshot, String s) {

      }

      @Override
      public void onChildRemoved(DataSnapshot dataSnapshot) {

      }

      @Override
      public void onChildMoved(DataSnapshot dataSnapshot, String s) {

      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
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
    String companyKey = currentUser.getCompanyKey();

//    FirebaseDatabase.getInstance().getReference("companies/" + companyKey).addValueEventListener(new ValueEventListener() {
//      @Override
//      public void onDataChange(DataSnapshot dataSnapshot) {
//        Company company = dataSnapshot.getValue(Company.class);
//        currentCompany = company;
//      }
//
//      @Override
//      public void onCancelled(DatabaseError databaseError) {
//
//      }
//    });

    DatabaseObject<Company> companyObject = new DatabaseObject<>(Company.class, companyKey);
    companyObject.addValueEventListener(new DatabaseObjectEventListener<Company>() {
      @Override
      public void onChange(String key, Company company) {
        currentCompany = company;
      }
    });
  }

  public DateTimeFormatter getDateFormatter() {
    return dateFormatter;
  }

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

  public java.lang.String getStorageUrl() {
    return storageUrl;
  }

  public int getCompanyPoundsPerCubicFoot(){
    if (currentCompany != null){
      return Integer.valueOf(currentCompany.getPoundsPerCubicFoot());
    }

    return 7;
  }

  public Company getCurrentCompany(){return currentCompany;}

  // may return null
  public String getCompanyLogoUrl(){
    if (currentCompany != null){
      return currentCompany.getLogoUrl();
    }
    return null;
  }

  public boolean userIsAtLeastForeman(){
    User.Role role = currentUser.getRoleAsEnum();
    return role == User.Role.AgentForeman ||
            role == User.Role.CompanyAdmin ||
            role == User.Role.ServiceAdmin ||
            role == User.Role.Foreman;

  }
  public Server getMailServer(){return mailServer;}
//
//  public Configuration getMailgunConfiguration() {
//    return mailgunConfiguration;
//  }

  public List<MovingItemDataDescription> getListFor(MovingItemDataDescription.Room room){
    return movingItemDescriptions.get(room.name());
  }
}
