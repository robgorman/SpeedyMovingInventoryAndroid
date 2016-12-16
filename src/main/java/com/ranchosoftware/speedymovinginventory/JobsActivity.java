package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.ranchosoftware.speedymovinginventory.firebase.FirebaseListAdapter;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.model.UserIdMapEntry;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;

import java.util.Map;

public class JobsActivity extends BaseMenuActivity {

  private static final String TAG = BaseMenuActivity.class.getSimpleName();

  // only one of these two below is visible at once
  private ListView jobsListView;
  private View noJobsView;
  private String companyKey;
  private View workingView;

  private User.Role role;
  private User user;

  FirebaseListAdapter<Job> adapter;
  DateTimeFormatter formatter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_jobs);

    Bundle b = getIntent().getExtras();

    companyKey = b.getString("companyKey");

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    formatter = app().getDateFormatter();

    FirebaseAuth auth = FirebaseAuth.getInstance();
    auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() == null){
          finish();
        }
      }
    });

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
      }
    });

    jobsListView = (ListView) findViewById(R.id.jobsListView);
    if (BuildConfig.FLAVOR.equalsIgnoreCase("dev")){
      // change the background colo
      //jobsListView.setBackgroundColor(Color.RED);
    }
    noJobsView = findViewById(R.id.layout_no_jobs);
    workingView = findViewById(R.id.llLayoutWorking);

    if (!app().isInitializationDone()){
      workingView.setVisibility(View.VISIBLE);
      noJobsView.setVisibility(View.INVISIBLE);
      jobsListView.setVisibility(View.INVISIBLE);

      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          checkInitialization();
        }
      }, 200);
    } else {
      workingView.setVisibility(View.VISIBLE);
      noJobsView.setVisibility(View.VISIBLE);
      jobsListView.setVisibility(View.INVISIBLE);
    }

    user = app().getCurrentUser();
    role = user.getRoleAsEnum();

    Query ref = FirebaseDatabase.getInstance().getReference("/joblists/" + companyKey + "/jobs")
            .orderByChild("jobNumber");


    // Todo; we certainly don't need this order by any more
    //User user = app().getCurrentUser();
    //if (user != null){
    //  ref = ref.orderByChild("companyKey").startAt(user.getCompanyKey()).endAt(user.getCompanyKey());
    //}

    FirebaseListAdapter.IFilter filter = new FirebaseListAdapter.IFilter(){
      @Override
      public boolean filter(DataSnapshot dataSnapshot) {
        Job job = dataSnapshot.getValue(Job.class);
        // TODO getCurrentUser() can return null sometimes. Probably if there is some
        // uncompleted login. The logs show this is null in version 3.3 sometimes

        // filter all cancelled jobs
        if (job.getIsCancelled()){
          return true;
        }

        if (isActive(job)){
          if (canCurrentUserViewJob(job, dataSnapshot)){
            return false;
          }
        }

        return true;

        /*

        if (role == User.Role.AgentCrewMember || role == User.Role.AgentForeman || role == User.Role.CrewMember
          || role == User.Role.Foreman){
          Map<String, UserIdMapEntry> map = job.getUsers();
          if (map.containsKey(app().getCurrentUser().getUid())) {
            return false;
          }
        }
        if (role == User.Role.Customer){
          if (user.getCustomerJobKey() != null && user.getCustomerJobKey().equals(dataSnapshot.getKey())){
            return false;
          }
        }

        if (role == User.Role.CompanyAdmin || role == User.Role.ServiceAdmin) {
          return false;
        }


        return true;
        */
      }


    };

    adapter = new FirebaseListAdapter<Job>(thisActivity, Job.class, R.layout.job_item, ref, filter){

      @Override
      protected void populateView(View v, Job job, int position) {
        // if there is at least one job hide the no jobs messae
        if (app().isInitializationDone()) {
          noJobsView.setVisibility(View.INVISIBLE);
          jobsListView.setVisibility(View.VISIBLE);
        }

        TextView jobNumberView = (TextView) v.findViewById(R.id.tvJobNumber);
        TextView nameView = (TextView) v.findViewById(R.id.tvName);
        TextView statusView = (TextView) v.findViewById(R.id.tvStatus);
        TextView pickupDateView = (TextView) v.findViewById(R.id.tvPickupDate);
        TextView deliveryDateView = (TextView) v.findViewById(R.id.tvDeliveryDate);
        ImageView statusImage = (ImageView) v.findViewById(R.id.statusImage);

        jobNumberView.setText(job.getJobNumber());
        nameView.setText(job.getCustomerLastName() + ", " + job.getCustomerFirstName());
        statusView.setText(job.getLifecycle().toString());
        pickupDateView.setText(formatter.print(job.getPickupDateTime()));
        deliveryDateView.setText(formatter.print(job.getDeliveryEarliestDate()));
        statusImage.setImageDrawable(Utility.imageForLifecycle(thisActivity,job.getLifecycle(), true));
      }
    };


    jobsListView.setAdapter(adapter);

    jobsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Job job = adapter.getItem(position);
        Bundle params = new Bundle();
        params.putString("companyKey", job.getCompanyKey());
        params.putString("jobKey", adapter.getRef(position).getKey());
        Intent intent = new Intent(thisActivity, JobActivity.class);
        intent.putExtras(params);
        startActivity(intent);
      }
    });
  }

  private void checkInitialization(){
    if (app().isInitializationDone()){
      if (adapter.getCount() == 0){
        workingView.setVisibility(View.INVISIBLE);
        noJobsView.setVisibility(View.VISIBLE);
        jobsListView.setVisibility(View.INVISIBLE);
      } else {
        workingView.setVisibility(View.VISIBLE);
        noJobsView.setVisibility(View.INVISIBLE);
        jobsListView.setVisibility(View.INVISIBLE);
      }
    } else {
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          checkInitialization();
        }
      }, 200);
    }
  }
  @Override
  public void onStart(){
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    adapter.cleanup();
  }

  private boolean canCurrentUserViewJob(Job job, DataSnapshot dataSnapshot) {

    if (role == User.Role.AgentCrewMember || role == User.Role.AgentForeman || role == User.Role.CrewMember
            || role == User.Role.Foreman){
      Map<String, UserIdMapEntry> map = job.getUsers();
      if (map.containsKey(app().getCurrentUser().getUid())) {
        return true;
      } else {
        return false;
      }
    }

    if (role == User.Role.Customer){
      if (user.getCustomerJobKey() != null && user.getCustomerJobKey().equals(dataSnapshot.getKey())){
        return true;
      } else {
        return false;
      }
    }

    return true;

  }

  private boolean isActive(Job job){
    if (job.getIsCancelled()){
      return false;
    }
    Job.Lifecycle lifecycle = job.getLifecycle();
    if (lifecycle == Job.Lifecycle.New || lifecycle == Job.Lifecycle.LoadedForStorage || lifecycle == Job.Lifecycle.LoadedForDelivery){
      return true;
    } else if (lifecycle == Job.Lifecycle.InStorage){
      DateTime earliestDeliveryDate = job.getDeliveryEarliestDate();
      DateTime now = DateTime.now();

      int days = Days.daysBetween(now.withTimeAtStartOfDay(), earliestDeliveryDate.withTimeAtStartOfDay()).getDays();
      if (days < 14){
        return true;
      }
    } else if (lifecycle == Job.Lifecycle.Delivered && job.getSignatureDelivered() != null){
      if (job.getSignatureDelivered() == null){
        return false;
      } else {
        DateTime signOffDate = new DateTime(job.getSignatureDelivered().getSignOffDateTime());
        DateTime now = DateTime.now();
        int days = Days.daysBetween(signOffDate, now).getDays();
        if (days < 14) {
          return true;
        }
      }
    }
    return false;
  }
}
