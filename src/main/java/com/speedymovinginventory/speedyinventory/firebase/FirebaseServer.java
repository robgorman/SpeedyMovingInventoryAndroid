package com.speedymovinginventory.speedyinventory.firebase;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.speedymovinginventory.speedyinventory.model.Company;
import com.speedymovinginventory.speedyinventory.model.Job;
import com.speedymovinginventory.speedyinventory.model.User;
import com.speedymovinginventory.speedyinventory.model.UserCompanyAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static com.speedymovinginventory.speedyinventory.firebase.FirebaseServer.OrderBy.ItemNumber;

/**
 * Created by rob on 5/7/17.
 */


public class FirebaseServer {

  private FirebaseDatabase database;

  public FirebaseServer(){
    database = FirebaseDatabase.getInstance();
  }

  public interface Failure{
    void error(String message);
  }
  public interface LookupDatabaseUserSuccess{
    void success(User user);
  }
  public interface Success{
    void success();
  }

  public interface SignInSuccess{
    void success(FirebaseUser firebaseUser);
  }

  private class ThreadPerTaskExecutor implements Executor {
    public void execute(Runnable r) {
      new Thread(r).start();
    }
  }

  public void signIn(String email, String password, final SignInSuccess success, final Failure failure){
    FirebaseAuth auth = FirebaseAuth.getInstance();

    ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(executor, new OnCompleteListener<AuthResult>() {
      @Override
      public void onComplete(@NonNull final Task<AuthResult> task) {
        if (!task.isSuccessful()) {
          new Handler(Looper.getMainLooper()).post(new Runnable(){

            @Override
            public void run() {
              failure.error(task.getException().getLocalizedMessage());
            }
          });

        } else {
          new Handler(Looper.getMainLooper()).post(new Runnable(){

            @Override
            public void run() {
              success.success(task.getResult().getUser());
            }
          });
        }
      }
    });



  }

  public void lookupDatabaseUser(FirebaseUser firebaseUser, final LookupDatabaseUserSuccess successCallback,
                                 final Failure failureCallback){
    final String uid = firebaseUser.getUid();
    DatabaseReference userReference = database.getReference("/users/" + uid);
    userReference.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(final DataSnapshot dataSnapshot) {
        // send on UI Thread only
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            User user = dataSnapshot.getValue(User.class);
            if (user != null){
              successCallback.success(user);
            } else {
              failureCallback.error("User with uid " + uid + " does not exist.");
            }
          }
        });
      }

      @Override
      public void onCancelled(final DatabaseError databaseError) {
        // send on UI Thread only
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            String message = databaseError.getMessage();

            failureCallback.error(message);

          }
        });
      }
    });
  }

  public interface CompanyAssignmentsSuccess{
    void success(List<UserCompanyAssignment> assignments);
  }




  public void getCompanyAssignmentsForUser(User user, final CompanyAssignmentsSuccess success, final Failure failure){

    final Query companiesQuery = FirebaseDatabase.getInstance().getReference("/companyUserAssignments/").orderByChild("uid").startAt(user.getUid()).endAt(user.getUid());
    final List<UserCompanyAssignment> assignments = new ArrayList<>();
    final ChildEventListener childListener = new ChildEventListener(){

      @Override
      public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        UserCompanyAssignment uca = dataSnapshot.getValue(UserCompanyAssignment.class);
        if (!uca.getIsDisabled()){
          assignments.add(uca);
        }
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
    companiesQuery.addChildEventListener(childListener);

    companiesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        // when we get this we know that we have been notified about all kids
        companiesQuery.removeEventListener(childListener);
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            success.success(assignments);

          }
        });
      }

      @Override
      public void onCancelled(final DatabaseError databaseError) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            String message = databaseError.getMessage();
            failure.error(message);
          }
        });
      }
    });

  }

  public interface GetCompanySuccess{
    void success(Company company);
  }

  public void getCompany(String companyKey, final GetCompanySuccess success, final Failure failure){

    DatabaseReference companyReference = database.getReference("/companies/" + companyKey);
    companyReference.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        final Company company = dataSnapshot.getValue(Company.class);
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            success.success(company);
          }
        });
      }

      @Override
      public void onCancelled(final DatabaseError databaseError) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            failure.error(databaseError.getMessage());
          }
        });
      }
    });
  }

  public Query getJobsListQuery(String companyKey){
    return FirebaseDatabase.getInstance().getReference("/joblists/" + companyKey + "/jobs")
            .orderByChild("jobNumber");
  }

  public interface GetJobSuccess {
    void success(Job job);
  }

  public void getJob(String companyKey, String jobKey, final GetJobSuccess success, final Failure failure){
    DatabaseReference jobReference = database.getReference("/joblists/" + companyKey + "/jobs/" + jobKey);
    jobReference.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        final Job job = dataSnapshot.getValue(Job.class);
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            success.success(job);
          }
        });
      }

      @Override
      public void onCancelled(final DatabaseError databaseError) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            failure.error(databaseError.getMessage());
          }
        });
      }
    });
  }

  public interface GetSchemaSuccess{
    void success(Long schemaVersion);
  }
  public void getSchema(final GetSchemaSuccess success, final Failure failure){
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    database.getReference("schema").addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(final DataSnapshot dataSnapshot) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {

            Object o = dataSnapshot.getValue();
            if (o instanceof Long) {
              Long schema = (Long) dataSnapshot.getValue();
              success.success(schema);
            }
          }
        });
      }

      @Override
      public void onCancelled(final DatabaseError databaseError) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){

          @Override
          public void run() {
            failure.error(databaseError.getMessage());
          }
        });
      }
    });
  }

  public Query getRecipientListQuery(String companyKey){
    return FirebaseDatabase.getInstance().getReference("/companyUserAssignments/").orderByChild("companyKey").startAt(companyKey).endAt(companyKey);
  }

  private String convertToString(OrderBy orderBy){
    String returnValue = "";
    switch (orderBy){
      case Value:
        returnValue = "monetaryValueInverse";
        break;
      case Volume:
        returnValue = "volumeInverse";
        break;
      case Category:
        returnValue = "category";
        break;
      case Scanned:
        returnValue = "isScanned";
        break;
      case Weight:
        returnValue = "weightLbsInverse";
        break;
      case ActiveClaim:
        returnValue = "isClaimActiveInverse";
        break;
      case NumberOfPads:
        returnValue = "numberOfPadsInverse";
        break;
      case ItemNumber:
        returnValue = "";
        break;
    }
    return returnValue;
  }

  public enum OrderBy {Value, Volume, Category, Scanned, Weight, ActiveClaim, NumberOfPads, ItemNumber};
  public Query getItemsQuery( String jobKey, OrderBy orderBy){
    String orderByString = convertToString(orderBy);
    if (orderBy == ItemNumber){
      // the natural order
      return FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items");
    } else {
      return FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild(orderByString);
    }
  }

  public void setItemScanned(String jobKey, String itemKey, boolean value){
    DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey +
            "/items/");
    itemsRef.child(itemKey + "/isScanned").setValue(false);

  }

}
