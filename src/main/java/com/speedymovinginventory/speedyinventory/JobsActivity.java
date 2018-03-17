package com.speedymovinginventory.speedyinventory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.speedymovinginventory.speedyinventory.firebase.FirebaseListAdapter;
import com.speedymovinginventory.speedyinventory.firebase.FirebaseServer;
import com.speedymovinginventory.speedyinventory.model.Job;
import com.speedymovinginventory.speedyinventory.model.User;
import com.speedymovinginventory.speedyinventory.model.UserCompanyAssignment;
import com.speedymovinginventory.speedyinventory.model.UserIdMapEntry;
import com.speedymovinginventory.speedyinventory.utility.Utility;

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

  private UserCompanyAssignment uca;
  private User user;

  FirebaseListAdapter<Job> adapter;
  DateTimeFormatter formatter;

  public static Intent getLaunchIntent(Context context, String companyKey){
    Bundle params = new Bundle();
    params.putString("companyKey", companyKey);
    Intent intent = new Intent(context, JobsActivity.class);
    intent.putExtras(params);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_jobs);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    Bundle b = getIntent().getExtras();

    companyKey = b.getString("companyKey");
    boolean hasParent = b.getBoolean("hasParent", false);

    ActionBar bar = getSupportActionBar();
    bar.setTitle("Jobs (" + app().getCurrentCompany().getName() + ")");


    getSupportActionBar().setDisplayHomeAsUpEnabled(hasParent);

    formatter = app().getDateFormatter();

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
    uca = app().getUserCompanyAssignment();

    FirebaseServer server = app().getFirebaseServer();
    Query ref = server.getJobsListQuery(companyKey);

    FirebaseListAdapter.IFilter filter = new FirebaseListAdapter.IFilter(){
      @Override
      public boolean filter(DataSnapshot dataSnapshot) {
        Job job = dataSnapshot.getValue(Job.class);
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
        Intent intent = JobActivity.getLaunchIntent(thisActivity, job.getCompanyKey(),
                adapter.getRef(position).getKey());
        startActivity(intent);
      }
    });

    checkSchemaVersionAndWarn();
  }

  private void checkSchemaVersionAndWarn(){
    // speedyMovingItemDataDescriptions is readable by anyone. No security or login
    // necessary.

    FirebaseServer server = app().getFirebaseServer();
    server.getSchema(new FirebaseServer.GetSchemaSuccess() {
      @Override
      public void success(Long schemaVersion) {

        if (schemaVersion > 1){
          showSchemaWarning();
        }
      }
    }, new FirebaseServer.Failure() {
      @Override
      public void error(String message) {
        // nothing to do
      }
    });
  }

  private void showSchemaWarning(){
    new AlertDialog.Builder(this)
            .setTitle("Upgrade Warning!")
            .setMessage("The Speedy Moving Inventory Database has been upgraded. To ensure full functionality, " +
                    "please upgrade to the latest version. This version has limited functionaity and may " +
                    "malfunction.")

            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                // continue with delete
              }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
  }


  private void checkInitialization(){
    if (app().isInitializationDone()){
      if (adapter.getCount() == 0){
        workingView.setVisibility(View.INVISIBLE);
        noJobsView.setVisibility(View.VISIBLE);
        jobsListView.setVisibility(View.INVISIBLE);
      } else {
        workingView.setVisibility(View.INVISIBLE);
        noJobsView.setVisibility(View.INVISIBLE);
        jobsListView.setVisibility(View.VISIBLE);
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

    // TODO this is a hack to avoid crash
    uca = app().getUserCompanyAssignment();
    if (uca == null){
      return true;
    }

    User.Role role = uca.getRoleAsEnum();
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
      if (uca.getCustomerJobKey() != null && uca.getCustomerJobKey().equals(dataSnapshot.getKey())){
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {

      case R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;


      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
