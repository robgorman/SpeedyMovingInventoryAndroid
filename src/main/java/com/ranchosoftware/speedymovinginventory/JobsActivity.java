package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.vision.text.Text;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.User;

import org.joda.time.format.DateTimeFormatter;

public class JobsActivity extends BaseMenuActivity {

  // only one of these two below is visible at once
  private ListView jobsListView;
  private View noJobsView;

  FirebaseListAdapter<Job> adapter;
  DateTimeFormatter formatter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_jobs);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    formatter = app().getDateTimeFormatter();


    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
      }
    });

    jobsListView = (ListView) findViewById(R.id.jobsListView);
    noJobsView = findViewById(R.id.layout_no_jobs);

    noJobsView.setVisibility(View.VISIBLE);
    jobsListView.setVisibility(View.INVISIBLE);

    Query ref = FirebaseDatabase.getInstance().getReference("/jobs");

    User user = app().getCurrentUser();
    if (user != null){
      ref = ref.orderByChild("companyKey").startAt(user.getCompanyKey()).endAt(user.getCompanyKey());
    }

    adapter = new FirebaseListAdapter<Job>(thisActivity, Job.class, R.layout.job_item, ref) {
      @Override
      protected void populateView(View v, Job job, int position) {
        // if there is at least one job hide the no jobs messae
        noJobsView.setVisibility(View.INVISIBLE);
        jobsListView.setVisibility(View.VISIBLE);

        TextView jobNumberView = (TextView) v.findViewById(R.id.tvJobNumber);
        TextView nameView = (TextView) v.findViewById(R.id.tvName);
        TextView statusView = (TextView) v.findViewById(R.id.tvStatus);
        TextView pickupDateView = (TextView) v.findViewById(R.id.tvPickupDate);
        TextView deliveryDateView = (TextView) v.findViewById(R.id.tvDeliveryDate);

        jobNumberView.setText(job.getJobNumber());
        nameView.setText(job.getCustomerLastName() + ", " + job.getCustomerFirstName());
        statusView.setText(job.getLifecycle().toString());
        pickupDateView.setText(formatter.print(job.getPickupDateTime()));
        deliveryDateView.setText(formatter.print(job.getDeliveryEarliestDate()));
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

  @Override
  protected void onDestroy() {
    super.onDestroy();
    adapter.cleanup();
  }

}
