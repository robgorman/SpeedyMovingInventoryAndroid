package com.speedymovinginventory.speedyinventory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.google.firebase.auth.FirebaseAuth;
import com.speedymovinginventory.speedyinventory.firebase.FirebaseServer;
import com.speedymovinginventory.speedyinventory.model.Company;
import com.speedymovinginventory.speedyinventory.model.User;
import com.speedymovinginventory.speedyinventory.model.UserCompanyAssignment;
import com.speedymovinginventory.speedyinventory.utility.Utility;

import java.util.ArrayList;
import java.util.List;

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



  public static Intent getLaunchIntent(Context context){
    return new Intent(context, ChooseCompanyActivity.class);
  }


  public void noCompanyAssignmentsError(){
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.dismiss();
      }
    };
    Utility.popupError(thisActivity, "Error",
            "There are no company assignments for user " + user.getFirstName() + " " + user.getLastName() + ". Contact support.",
            listener);

  }

  public void errorLoginNotSupportedForCustomers(){
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.dismiss();
        app().resetCredentials();
        FirebaseAuth.getInstance().signOut();
        finish();
      }
    };
    Utility.popupError(thisActivity, "Customer Login Not Supported", "We apologize, but our mobile app does not support customer logins at this time. " +
            "Please try the web interface at https://app.speedymovinginventory.com", listener);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_choose_company);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

    toolbar.setTitle("Choose A Mover");
    setSupportActionBar(toolbar);

    user = app().getCurrentUser();
    FirebaseServer server = app().getFirebaseServer();
    server.getCompanyAssignmentsForUser(user, new FirebaseServer.CompanyAssignmentsSuccess() {
      @Override
      public void success(List<UserCompanyAssignment> assignments) {

        if (assignments.size() == 0){
          noCompanyAssignmentsError();
        } else if (assignments.size() == 1){
          final UserCompanyAssignment assignment = assignments.get(0);
          if (assignment.getRoleAsEnum() == User.Role.Customer){
            errorLoginNotSupportedForCustomers();
            return;
          }
          // we just launch jobs
          app().setUserCompanyAssignment(assignment);
          FirebaseServer server = app().getFirebaseServer();
          server.getCompany(assignment.getCompanyKey(), new FirebaseServer.GetCompanySuccess() {
                    @Override
                    public void success(Company company) {
                      app().setCurrentCompany(company, assignment.getCompanyKey());
                      Intent intent = JobsActivity.getLaunchIntent(thisActivity, assignment.getCompanyKey());
                      startActivity(intent);
                      finish();
                      return;
                    }
                  }, new FirebaseServer.Failure() {
                    @Override
                    public void error(String message) {

                    }
                  });

        } else {
          for (final UserCompanyAssignment uca : assignments) {

            FirebaseServer server = app().getFirebaseServer();
            server.getCompany(uca.getCompanyKey(), new FirebaseServer.GetCompanySuccess() {
              @Override
              public void success(Company company) {
                companies.add(new CompanyAndUca(company, uca));
                adapter.notifyDataSetChanged();
              }
            }, new FirebaseServer.Failure() {
              @Override
              public void error(String message) {
                //TODO what todo do

              }
            });

          }
        }

      }
    }, new FirebaseServer.Failure() {
      @Override
      public void error(String message) {
        // TODO
        Utility.popupError(thisActivity, "Error", message, null);
      }
    });


    companiesListView = (ListView) findViewById(R.id.companiesListView);
    loadingIndicator = (ProgressBar) findViewById((R.id.loadingIndicator));
    mainLayout = findViewById(R.id.mainLayout);

    adapter = new CompanyAdapter(thisActivity, companies);
    companiesListView.setAdapter(adapter);



    companiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        CompanyAndUca companyAndUca = adapter.getItem(position);

        final UserCompanyAssignment assignment = companyAndUca.getUserCompanyAssignment();
        app().setUserCompanyAssignment(assignment);

        FirebaseServer server = app().getFirebaseServer();
        server.getCompany(assignment.getCompanyKey(), new FirebaseServer.GetCompanySuccess() {
          @Override
          public void success(Company company) {
            app().setCurrentCompany(company, assignment.getCompanyKey());
            Intent intent = new Intent(thisActivity, JobsActivity.class);
            Bundle params = new Bundle();
            params.putString("companyKey", assignment.getCompanyKey());
            params.putBoolean("hasParent", true);
            intent.putExtras(params);
            startActivity(intent);
          }
        }, new FirebaseServer.Failure() {
          @Override
          public void error(String message) {
             // TODO
          }
        });


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
