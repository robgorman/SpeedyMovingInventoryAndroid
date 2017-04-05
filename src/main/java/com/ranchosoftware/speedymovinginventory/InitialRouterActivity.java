package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.model.User;

public class InitialRouterActivity extends BaseActivity {

  private static final String TAG = InitialRouterActivity.class.getSimpleName();

  private FirebaseAuth.AuthStateListener authListener;
  private FirebaseAuth auth;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_initial_router);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);


    auth = FirebaseAuth.getInstance();
    //auth.signOut();
    authListener = new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

        auth.removeAuthStateListener(authListener);
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null){
          launchLogin();

        } else {
          lookupDatabaseUser(user);
        }
      }
    };

  }

  private void launchLogin(){
    Intent intent = new Intent(thisActivity, LoginActivity.class);
    startActivity(intent);
    finish();
  }

  ValueEventListener userListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

      //userReference.removeEventListener(userListener);
      try {
        final User user = dataSnapshot.getValue(User.class);
        if (user == null){
          launchLogin();
        } else {
          app().setCurrentUser(user);


          Handler mainHandler = new Handler(thisActivity.getMainLooper());
          mainHandler.post(new Runnable() {

            @Override
            public void run() {
              Intent intent = new Intent(thisActivity, ChooseCompanyActivity.class);
              startActivity(intent);
              finish();
            }
          });
        }
      } catch (Exception e) {
        // if we get an exception trying to pull the user out of the dataSnapshot
        launchLogin();
      }

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

  };

  private DatabaseReference userReference;

  private void lookupDatabaseUser(FirebaseUser firebaseUser){
    String uid = firebaseUser.getUid();
    userReference = FirebaseDatabase.getInstance().getReference("/users/"+uid);

    userReference.addListenerForSingleValueEvent(userListener);


  }

  @Override
  protected void onResume() {
    super.onResume();
    auth.addAuthStateListener(authListener);
  }

  @Override
  protected void onPause() {
    super.onPause();
    //auth.removeAuthStateListener(authListener);
  }

  @Override
  public void onStart() {
    super.onStart();
    //auth.addAuthStateListener(authListener);

  }

  @Override
  public void onStop() {
    super.onStop();

  }
}
