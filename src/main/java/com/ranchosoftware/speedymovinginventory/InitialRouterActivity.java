package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.firebase.auth.FirebaseUser;
import com.ranchosoftware.speedymovinginventory.firebase.Authorization;
import com.ranchosoftware.speedymovinginventory.firebase.FirebaseServer;
import com.ranchosoftware.speedymovinginventory.model.User;

public class InitialRouterActivity extends BaseActivity {

  private static final String TAG = InitialRouterActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_initial_router);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    final Authorization authorization = app().getAuthorization();

    authorization.addListener(new Authorization.FirebaseSignIn() {
      @Override
      public void userSignedIn(FirebaseUser user) {
        authorization.removeListener(this);
        lookupDatabaseUser(user);
      }

      @Override
      public void userSignedOut() {
        authorization.removeListener(this);
        launchLogin();
      }
    });
  }

  private void launchLogin(){
    Intent intent = LoginActivity.getLaunchIntent(thisActivity);
    startActivity(intent);
    finish();
  }


  private void lookupDatabaseUser(FirebaseUser firebaseUser){
    app().getFirebaseServer().lookupDatabaseUser(firebaseUser,
            new FirebaseServer.LookupDatabaseUserSuccess() {
              @Override
              public void success(User user) {
                app().setCurrentUser(user);
                Intent intent = ChooseCompanyActivity.getLaunchIntent(thisActivity);
                startActivity(intent);
                finish();
              }
            }, new FirebaseServer.Failure() {
              @Override
              public void error(String message) {

              }
            }
    );
  }

  @Override
  protected void onResume() {
    super.onResume();

  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  public void onStart() {
    super.onStart();

  }

  @Override
  public void onStop() {
    super.onStop();

  }
}
