package com.ranchosoftware.speedymovinginventory;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.model.UserCompanyAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChooseCompanyActivity extends BaseMenuActivity {

  private ListView companiesListView;
  private View mainLayout;
  private ProgressBar loadingIndicator;

  private User user;

  private CompanyAdapter adapter;

  private class CompanyAndKey {
    private Company company;
    private String companyKey;

    public CompanyAndKey(Company company, String companyKey) {
      this.company = company;
      this.companyKey = companyKey;
    }

    public Company getCompany() {
      return company;
    }

    public String getCompanyKey() {
      return companyKey;
    }
  }

  private List<CompanyAndKey> companies = new ArrayList<>();

  class CustomValueEventListener implements ValueEventListener {
    private DatabaseReference ref;

    CustomValueEventListener(DatabaseReference ref) {
      this.ref = ref;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      ref.removeEventListener(this);
      Company company = dataSnapshot.getValue(Company.class);
      companies.add(new CompanyAndKey(company, dataSnapshot.getKey()));
      adapter.notifyDataSetChanged();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  }



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
    if (countActiveCompaniesForUser(user) <= 1){
      String companyKey = "";
      String companyName = "";
      if (user.getCompanies() != null && user.getCompanies().size() > 0) {
        for (Map.Entry<String, UserCompanyAssignment> assignment : user.getCompanies().entrySet()) {
          companyKey = assignment.getValue().getCompanyKey();

        }
      } else {
        companyKey = user.getCompanyKey();
      }
      Intent intent = new Intent(thisActivity, JobsActivity.class);
      Bundle params = new Bundle();
      params.putString("companyKey", companyKey);

      intent.putExtras(params);
      startActivity(intent);
      finish();
      return;
    }


    companiesListView = (ListView) findViewById(R.id.companiesListView);
    loadingIndicator = (ProgressBar) findViewById((R.id.loadingIndicator));
    mainLayout = findViewById(R.id.mainLayout);

    adapter = new CompanyAdapter(thisActivity, companies);
    companiesListView.setAdapter(adapter);

    for (Map.Entry<String, UserCompanyAssignment> assignment : user.getCompanies().entrySet()) {
      String companyKey = assignment.getValue().getCompanyKey();
      final DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("/companies/" + companyKey);
      CustomValueEventListener listener = new CustomValueEventListener(companyRef);
      companyRef.addValueEventListener(listener);

    }

    companiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        CompanyAndKey companyAndKey = adapter.getItem(position);
        Intent intent = new Intent(thisActivity, JobsActivity.class);
        Bundle params = new Bundle();
        params.putString("companyKey", companyAndKey.getCompanyKey());
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


  private class CompanyAdapter extends ArrayAdapter<CompanyAndKey> {
    private Context context;
    private LayoutInflater inflater;

    public CompanyAdapter(Context context, List<CompanyAndKey> companies) {
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

      if (company.getContact().length() > 0) {
        contact.setText(company.getContact());
      } else {
        contact.setText("None");
      }


      return convertView;
    }
  }
}
