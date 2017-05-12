package com.ranchosoftware.speedymovinginventory.firebase;

import android.support.annotation.NonNull;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rob on 5/7/17.
 */

public class Authorization {

  private FirebaseAuth auth;
  private FirebaseAuth.AuthStateListener authListener;
  private FirebaseUser user;
  private boolean gotAuthStateChangedOnce = false;

  private List<FirebaseSignIn> signInListeners = new ArrayList<FirebaseSignIn>();

  public static interface FirebaseSignIn {
    void userSignedIn(FirebaseUser user);
    void userSignedOut();
  }

  public Authorization(){
    auth = FirebaseAuth.getInstance();
    authListener = new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        gotAuthStateChangedOnce = true;
        user = firebaseAuth.getCurrentUser();
        notifyListeners();
      }
    };
    auth.addAuthStateListener(authListener);
  }

  private void notifyLoggedIn(){
    for (FirebaseSignIn listener : signInListeners){
      listener.userSignedIn(user);
    }
  }

  private void notifyLoggedOut(){
    for (FirebaseSignIn listener : signInListeners){
      listener.userSignedOut();
    }
  }

  private void notifyListeners(){
    if (isLoggedIn()) {
      notifyLoggedIn();
    } else {
      notifyLoggedOut();
    }
  }

  public void addListener(FirebaseSignIn listener){
    if (!signInListeners.contains(listener)){
      signInListeners.add(listener);
      // notify latecomers of the initial state if we know it
      if (gotAuthStateChangedOnce){
        if (isLoggedIn()){
          listener.userSignedIn(user);
        } else {
          listener.userSignedOut();
        }
      }
    }
  }

  public void removeListener(FirebaseSignIn listener){
    if (signInListeners.contains(listener)){
      signInListeners.remove(listener);
    }
  }

  public boolean isLoggedIn(){
    return user != null;
  }

  public FirebaseUser getUser(){
    return user;
  }


}
