package com.speedymovinginventory.speedyinventory;

/**
 * Created by rob on 10/10/16.
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.speedymovinginventory.speedyinventory.app.RanchoApp;

/**
 * Created by rob on 1/3/16.
 */
public class AboutDialog {


  private Dialog dialog;


  public AboutDialog(final Activity activity) {
    LayoutInflater inflater = LayoutInflater.from(activity);
    View view = inflater.inflate(R.layout.about_dialog, null);

    TextView version = (TextView) view.findViewById(R.id.tvVersion);
    PackageInfo pInfo = null;
    try {
      pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
      String verString =  pInfo.versionName;
      version.setText(verString);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    dialog = new Dialog(activity);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(view);
    dialog.getWindow().setBackgroundDrawable(
            new ColorDrawable(android.graphics.Color.TRANSPARENT));

    Button cancel = (Button) view.findViewById(R.id.cancel_button);
    cancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });

    TextView tvRole = (TextView) view.findViewById(R.id.tvRole);
    TextView tvUserName  = (TextView) view.findViewById(R.id.tvUserName);


    RanchoApp app = (RanchoApp) activity.getApplication();
    if (app.getCurrentUser() == null){
      tvUserName.setText("");
    } else {
      tvUserName.setText(app.getCurrentUser().getFirstName() + " " + app.getCurrentUser().getLastName());
    }

    if (app.getUserCompanyAssignment() == null){
      tvRole.setText("");
    } else {
      tvRole.setText("(" + app.getUserCompanyAssignment().getRole() + ")");
    }




  }



  public Dialog getDialog()
  {
    return dialog;
  }

}
