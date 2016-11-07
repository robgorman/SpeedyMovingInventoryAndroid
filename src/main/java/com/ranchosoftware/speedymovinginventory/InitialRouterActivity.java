package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.fitness.data.Value;
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

  private boolean fired = false;
  private boolean foundUser = false;

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
        if (fired){
          return;
        }
        fired = true;
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

  ValueEventListener listener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

      try {
        final User user = dataSnapshot.getValue(User.class);
        String companyKey = user.getCompanyKey();
        app().setCurrentUser(user);

        foundUser = true;
        Handler mainHandler = new Handler(thisActivity.getMainLooper());
        mainHandler.post(new Runnable() {

          @Override
          public void run() {
            Intent intent = new Intent(thisActivity, JobsActivity.class);
            Bundle params = new Bundle();
            params.putString("companyKey", user.getCompanyKey());
            intent.putExtras(params);
            startActivity(intent);
            finish();
          }
        });
      } catch (Exception e) {
        // if we get an exception trying to pull the user out of the dataSnapshot
        launchLogin();
      }

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

  };

  private void lookupDatabaseUser(FirebaseUser firebaseUser){
    String uid = firebaseUser.getUid();
    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/users/"+uid);

    // set a timeout because if the user has been deleted there will be no event occurring
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        if (!foundUser) {
          ref.removeEventListener(listener);
          FirebaseAuth.getInstance().signOut();
          launchLogin();
        }
      }
    }, 10000);

    ref.addValueEventListener(listener);
    //ref.addChildEventListener(childEventListener);

  }

  @Override
  public void onStart() {
    super.onStart();

    auth.addAuthStateListener(authListener);
  }

  @Override
  public void onStop() {
    super.onStop();
    if (authListener != null) {
      auth.removeAuthStateListener(authListener);
    }
  }
}
