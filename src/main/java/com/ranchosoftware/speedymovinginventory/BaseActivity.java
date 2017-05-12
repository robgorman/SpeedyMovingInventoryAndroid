package com.ranchosoftware.speedymovinginventory;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ranchosoftware.speedymovinginventory.app.RanchoApp;

/**
 * Created by rob on 7/13/16.
 */

public class BaseActivity extends AppCompatActivity {
  protected BaseActivity thisActivity;
  private FirebaseAnalytics firebaseAnalytics;
  @Override
  protected void onCreate(Bundle bundle){
    super.onCreate(bundle);
    thisActivity = this;
    firebaseAnalytics = FirebaseAnalytics.getInstance(this);
  }


  public RanchoApp app(){
    return ((RanchoApp)getApplication());
  }
  public View getRootView(){
    return findViewById(android.R.id.content);
  }

  /**
   * Shows the progress UI and hides the login form.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  protected void showProgress(final boolean show, final View progressView, final View otherView) {
    // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
    // for very easy animations. If available, use these APIs to fade-in
    // the progress spinner.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
      int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

      otherView.setVisibility(show ? View.GONE : View.VISIBLE);
      otherView.animate().setDuration(shortAnimTime).alpha(
              show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          otherView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
      });

      progressView.setVisibility(show ? View.VISIBLE : View.GONE);
      progressView.animate().setDuration(shortAnimTime).alpha(
              show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
      });
    } else {
      // The ViewPropertyAnimator APIs are not available, so simply show
      // and hide the relevant UI components.
      progressView.setVisibility(show ? View.VISIBLE : View.GONE);
      otherView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
  }

}
