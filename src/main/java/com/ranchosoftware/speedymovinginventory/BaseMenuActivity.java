package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by rob on 7/19/16.
 */

public class BaseMenuActivity extends BaseActivity {
  public class BaseActivity extends AppCompatActivity {
    FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() == null){
          // we just logged out. finish.
          finish();
        }
      }
    };

    FirebaseAuth auth;

    @Override
    protected void onStart(){
      super.onStart();
      auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop(){
      super.onStop();
      auth.removeAuthStateListener(authStateListener);
    }
    @Override
    protected void onCreate(Bundle bundle) {
      super.onCreate(bundle);

    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.base_menu, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.logout:
        logout();
        return true;
      case R.id.about:

        Toast.makeText(this, "Version :" + version(),Toast.LENGTH_LONG).show();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private String version(){
    String versionCode = Integer.toString(BuildConfig.VERSION_CODE);
    String versionName = BuildConfig.VERSION_NAME;
    return versionName + " " + versionCode;
  }
  private void logout(){
    FirebaseAuth auth = FirebaseAuth.getInstance();
    auth.signOut();

    Intent intent = new Intent(this, LoginActivity.class);
    // send no autologin param
    Bundle b = new Bundle();
    b.putBoolean("allowAutoLogin", false);
    intent.putExtras(b);
    startActivity(intent);

  }
}
