package com.ranchosoftware.speedymovinginventory;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.ranchosoftware.speedymovinginventory.app.RanchoApp;
import com.ranchosoftware.speedymovinginventory.firebase.Authorization;
import com.ranchosoftware.speedymovinginventory.firebase.FirebaseServer;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.utility.Utility;


/**
 * A login screen that offers login via email/passwordView.
 */
public class LoginActivity extends BaseActivity {

  private static final String TAG = LoginActivity.class.getSimpleName();
  // UI references.
  private AutoCompleteTextView emailView;
  private EditText passwordView;
  private TextView loginErrorView;
  private View loginFormView;
  private View progressView;
  private CheckBox rememberMe;

  static Intent getLaunchIntent(Context context){
    return new Intent(context, LoginActivity.class);
  }

  /**
   * ATTENTION: This was auto-generated to implement the App Indexing API.
   * See https://g.co/AppIndexing/AndroidStudio for more information.
   */
  //private GoogleApiClient client;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    Bundle params = getIntent().getExtras();

    if (!BuildConfig.FLAVOR.equalsIgnoreCase("prod")){
      // change the background color
      View backgroundView = findViewById(R.id.backgroundLayout);
      //backgroundView.setBackgroundColor(Color.RED);
    }

    boolean allowAutoLogin = true;
    if (params != null) {
      allowAutoLogin = params.getBoolean("allowAutoLogin", true);
    }

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Login");
    setSupportActionBar(toolbar);

    app().getAuthorization().addListener(new Authorization.FirebaseSignIn() {
      @Override
      public void userSignedIn(FirebaseUser user) {
        app().getAuthorization().removeListener(this);
        lookupDatabaseUser(user);
      }

      @Override
      public void userSignedOut() {
        // nothing to do
      }
    });

    // Set up the login form.
    emailView = (AutoCompleteTextView) findViewById(R.id.tvEmail);
    //emailView.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

    loginFormView = findViewById(R.id.login_form);
    loginErrorView = (TextView) findViewById(R.id.loginErrorView);
    passwordView = (EditText) findViewById(R.id.password);
    passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        if (id == R.id.email_sign_in_button || id == EditorInfo.IME_NULL) {
          attemptLogin();
          return true;
        }
        return false;
      }
    });

    //emailView.setText("rob@ranchosoftware.com");
    //passwordView.setText("aaaaaaaa");

    progressView = findViewById(R.id.login_progress);
    progressView.setVisibility(View.GONE);

    Button emailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
    emailSignInButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        attemptLogin();
      }
    });

    Button forgotPasswordButton = (Button) findViewById(R.id.forgot_password_button);
    forgotPasswordButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        launchForgotPasswordActivity();
      }
    });

    rememberMe = (CheckBox) findViewById(R.id.checkBoxRememberMe);
    rememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO this is an issue
      }
    });

    if (app().getSavedCredentials() != null && allowAutoLogin){
      showProgress(true);
      doLogin(app().getSavedCredentials().email, app().getSavedCredentials().password);
    } else if (app().getSavedCredentials() != null && !allowAutoLogin){
      String s = app().getSavedCredentials().email;
      emailView.setText(s);
      passwordView.setText(app().getSavedCredentials().password);

    }


    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
  }

  private void launchForgotPasswordActivity() {
    Intent intent = ForgotPasswordActivity.getLaunchIntent(thisActivity);
    startActivity(intent);
  }


  private void lookupDatabaseUser(FirebaseUser firebaseUser) {
    FirebaseServer server = app().getFirebaseServer();
    server.lookupDatabaseUser(firebaseUser, new FirebaseServer.LookupDatabaseUserSuccess() {
      @Override
      public void success(User user) {
        app().setCurrentUser(user);
        if (rememberMe.isChecked()) {
          String email = emailView.getText().toString();
          String password = passwordView.getText().toString();
          app().saveCredentials(new RanchoApp.LoginCredential(email, password));
        }
        Intent intent = ChooseCompanyActivity.getLaunchIntent(thisActivity);
        startActivity(intent);
        finish();
      }
    }, new FirebaseServer.Failure(){
      @Override
      public void error(String message) {
        Utility.error(thisActivity.getRootView(), thisActivity, message);
      }
    });

  }



  /**
   * Attempts to sign in or register the account specified by the login form.
   * If there are form errors (invalid email, missing fields, etc.), the
   * errors are presented and no actual login attempt is made.
   */
  private void attemptLogin() {
    loginErrorView.setVisibility(View.INVISIBLE);

    if (!rememberMe.isChecked()){
      app().resetCredentials();
    }
    // Reset errors.
    emailView.setError(null);
    passwordView.setError(null);

    // Store values at the time of the login attempt.
    String email = emailView.getText().toString();
    String password = passwordView.getText().toString();

    boolean cancel = false;
    View focusView = null;

    // Check for a valid password, if the user entered one.
    if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
      passwordView.setError(getString(R.string.error_invalid_password));
      focusView = passwordView;
      cancel = true;
    } else if (TextUtils.isEmpty(email)) {
      emailView.setError(getString(R.string.error_field_required));
      focusView = emailView;
      cancel = true;
    } else if (!Utility.isValidEmailAddress(email)) {
      emailView.setError(getString(R.string.error_invalid_email));
      focusView = emailView;
      cancel = true;
    }

    if (cancel) {
      // There was an error; don't attempt login and focus the first
      // form field with an error.
      focusView.requestFocus();
    } else {
      // Show a progress spinner, and kick off a background task to
      // perform the user login attempt.
      showProgress(true);
      doLogin(email, password);

    }
  }

  /**
   * Shows the progress UI and hides the login form.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showProgress(final boolean show) {
    // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
    // for very easy animations. If available, use these APIs to fade-in
    // the progress spinner.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
      int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

      loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
      loginFormView.animate().setDuration(shortAnimTime).alpha(
              show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
      loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
  }


  private void doLogin(String email, String password) {

    Authorization authorization = app().getAuthorization();
    // make sure no-one is signed in
    FirebaseServer server = app().getFirebaseServer();

    server.signIn(email, password, new FirebaseServer.SignInSuccess() {
      @Override
      public void success(FirebaseUser firebaseUser) {
         lookupDatabaseUser(firebaseUser);
      }
    }, new FirebaseServer.Failure() {
      @Override
      public void error(String message) {
        loginErrorView.setText("Authentication Failed: " + message);
        loginErrorView.setVisibility(View.VISIBLE);
        showProgress(false);
      }
    });
  }

  private boolean isPasswordValid(String password) {
    return password.length() > 6;
  }
}

