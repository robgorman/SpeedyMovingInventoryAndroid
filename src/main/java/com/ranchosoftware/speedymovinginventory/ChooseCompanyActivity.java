package com.ranchosoftware.speedymovinginventory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.android.volley.toolbox.ImageLoader;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.model.UserCompanyAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChooseCompanyActivity extends BaseActivity {

  private static final String TAG = BaseMenuActivity.class.getSimpleName();

  private ListView companiesListView;
  private View mainLayout;
  private ProgressBar loadingIndicator;

  private User user;

  private CompanyAdapter adapter;

  private class CompanyAndUca {
    private Company company;
    private UserCompanyAssignment uca;

    public CompanyAndUca(Company company, UserCompanyAssignment uca) {
      this.company = company;
      this.uca = uca;
    }

    public Company getCompany() {
      return company;
    }

    public UserCompanyAssignment getUserCompanyAssignment() {
      return uca;
    }
  }

  private List<UserCompanyAssignment> assignments = new ArrayList<>();

  private List<CompanyAndUca> companies = new ArrayList<>();

  class CustomValueEventListener implements ValueEventListener {
    private DatabaseReference ref;
    private UserCompanyAssignment uca;

    CustomValueEventListener(DatabaseReference ref, UserCompanyAssignment uca) {
      this.ref = ref;
      this.uca = uca;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      ref.removeEventListener(this);
      Company company = dataSnapshot.getValue(Company.class);
      companies.add(new CompanyAndUca(company,uca));
      adapter.notifyDataSetChanged();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  }


/*
  private int countActiveCompaniesForUser(User user){
    if (user.getCompanies() == null){
      // old style can only be 1
      return 1;
    }

    if (user.getCompanies().size() == 1){
      return 1;
    }

    int count = 0;
    for (Map.Entry<String, UserCompanyAssignment> assignment : user.getCompanies().entrySet()) {
      UserCompanyAssignment next = assignment.getValue();
      if (next.getIsDisabled() == false){
        count = count + 1;
      }
    }
    return count;
  }
  */
  private ChildEventListener childListener = new ChildEventListener(){

  @Override
  public void onChildAdded(DataSnapshot dataSnapshot, String s) {
    UserCompanyAssignment uca = dataSnapshot.getValue(UserCompanyAssignment.class);
    if (!uca.getIsDisabled()){
      int x = 5;
      assignments.add(uca);
      // TODO: this is confustion
      // we need to set the app stuff here.
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_choose_company);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

    toolbar.setTitle("Choose A Mover");
    setSupportActionBar(toolbar);

    user = app().getCurrentUser();
    // if the user is only associated with one company we just send them
    // there. Otherwise they need to choose.
    final Query companiesQuery = FirebaseDatabase.getInstance().getReference("/companyUserAssignments/").orderByChild("uid").startAt(user.getUid()).endAt(user.getUid());


    companiesQuery.addChildEventListener(childListener);

    companiesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {

        Log.d(TAG, "all done");
        companiesQuery.removeEventListener(childListener);
        if (assignments.size() == 0){
          DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dialog.dismiss();
            }
          };
          AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
          builder.setTitle("Error")
                  .setMessage("There are no company assignments for this user. Contact support.")
                  .setPositiveButton(R.string.ok, listener)
                  .show();
        } else if (assignments.size() == 1){
          // we just launch jobs

          app().setUserCompanyAssignment(assignments.get(0));
          app().setCurrentCompany(assignments.get(0).getCompanyKey());
          Intent intent = new Intent(thisActivity, JobsActivity.class);
          Bundle params = new Bundle();
          params.putString("companyKey", assignments.get(0).getCompanyKey());


          intent.putExtras(params);
          startActivity(intent);
          finish();
          return;
        } else {
          for (UserCompanyAssignment uca : assignments) {

            final DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("/companies/" + uca.getCompanyKey());
            CustomValueEventListener listener = new CustomValueEventListener(companyRef, uca);
            companyRef.addValueEventListener(listener);
          }
        }
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });


    companiesListView = (ListView) findViewById(R.id.companiesListView);
    loadingIndicator = (ProgressBar) findViewById((R.id.loadingIndicator));
    mainLayout = findViewById(R.id.mainLayout);

    adapter = new CompanyAdapter(thisActivity, companies);
    companiesListView.setAdapter(adapter);

    /*
    for (Map.Entry<String, UserCompanyAssignment> assignment : user.getCompanies().entrySet()) {
      String companyKey = assignment.getValue().getCompanyKey();
      final DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("/companies/" + companyKey);
      CustomValueEventListener listener = new CustomValueEventListener(companyRef);
      companyRef.addValueEventListener(listener);

    }
    */

    companiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        CompanyAndUca companyAndUca = adapter.getItem(position);

        app().setUserCompanyAssignment(assignments.get(0));
        app().setCurrentCompany(assignments.get(0).getCompanyKey());

        Intent intent = new Intent(thisActivity, JobsActivity.class);
        Bundle params = new Bundle();
        params.putString("companyKey", companyAndUca.getUserCompanyAssignment().getCompanyKey());
        params.putBoolean("hasParent", true);
        intent.putExtras(params);
        startActivity(intent);
      }
    });


  }

  private void updateUiFromData(){
    if (companies.size() >= 1) {
      mainLayout.setVisibility(View.VISIBLE);
      loadingIndicator.setVisibility(View.INVISIBLE);

    }

  }


  private class CompanyAdapter extends ArrayAdapter<CompanyAndUca> {
    private Context context;
    private LayoutInflater inflater;

    public CompanyAdapter(Context context, List<CompanyAndUca> companies) {
      super(context, R.layout.choose_company_item, companies);
      this.context = context;
      inflater = LayoutInflater.from(context);
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {

      // we have loaded at least one company, adjust the ui
      updateUiFromData();
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.choose_company_item, null);
      }
      TextView companyName = (TextView) convertView.findViewById(R.id.tvName);
      TextView companyPhone = (TextView) convertView.findViewById(R.id.tvPhone);
      TextView contact = (TextView) convertView.findViewById(R.id.tvContact);
      Company company = companies.get(position).getCompany();
      companyName.setText(company.getName());

      if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
        companyPhone.setText(PhoneNumberUtils.formatNumber(company.getPhoneNumber(), "US"));
      } else {
        companyPhone.setText(PhoneNumberUtils.formatNumber(company.getPhoneNumber()));
      }

      if (company.getContactPerson().length() > 0) {
        contact.setText(company.getContactPerson());
      } else {
        contact.setText("None");
      }


      return convertView;
    }
  }
}
