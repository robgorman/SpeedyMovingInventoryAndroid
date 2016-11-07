package com.ranchosoftware.speedymovinginventory;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.apache.commons.validator.routines.EmailValidator;


public class ForgotPasswordActivity extends BaseActivity {

  private AutoCompleteTextView emailView;

  private TextView textSentMessage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_forgot_password);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    CharSequence s = toolbar.getTitle();
    if (s != null){
      String name = s.toString();
    }

    //s = toolbar.getTitle();
    toolbar.setTitle("Forgot Password");
    setSupportActionBar(toolbar);



    textSentMessage = (TextView) findViewById(R.id.tvMessageSent);
    textSentMessage.setVisibility(View.INVISIBLE);
    emailView = (AutoCompleteTextView) findViewById(R.id.cellular_number);
    emailView.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

    Button textMyPassword = (Button) findViewById(R.id.text_password);
    textMyPassword.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        attemptToSend();
      }
    });
  }


  private void attemptToSend(){
    textSentMessage.setVisibility(View.INVISIBLE);
    emailView.setError(null);
    final String email = emailView.getText().toString();

    boolean cancel = false;
    View focusView = null;

    // Check for a valid cell num.
    if (TextUtils.isEmpty(email)) {
      emailView.setError(getString(R.string.error_field_required));
      focusView = emailView;
      cancel = true;
    } else if (!EmailValidator.getInstance().isValid(email)) {
      emailView.setError(getString(R.string.error_invalid_email));
      focusView = emailView;
      cancel = true;
    }

    if (cancel) {
      // There was an error; don't attempt login and focus the first
      // form field with an error.
      focusView.requestFocus();
    } else {
      sendResetInstructions(email);
    }
  }

  private void sendResetInstructions(final String email){
    Task<Void> task = FirebaseAuth.getInstance().sendPasswordResetEmail(email);
    task.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        String message = "Reset request failed: " + e.getLocalizedMessage();
        textSentMessage.setText(message);

      }
    }).addOnSuccessListener(new OnSuccessListener<Void>() {
      @Override
      public void onSuccess(Void aVoid) {

        String message = "Reset succeeded! Check " + email + " for instructions.";
        textSentMessage.setText(message);
      }
    });


    textSentMessage.setVisibility(View.VISIBLE);


  }


}
