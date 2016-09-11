package com.ranchosoftware.speedymovinginventory;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObject;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObjectEventListener;
import com.ranchosoftware.speedymovinginventory.model.Address;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.Signature;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import java.io.ByteArrayOutputStream;

public class SignOffActivity extends BaseActivity {

  private static final String TAG = SignOffActivity.class.getSimpleName();

  private SignaturePad signaturePad;
  private Button clearPad;
  private Button accept;
  private TextView signHere;
  private TextView tvName;
  private TextView tvTitle;
  private String companyKey;
  private String jobKey;
  private Job.Lifecycle entryLifecycle;
  private Boolean storageInTransit;

  private ProgressDialog progressDialog;

  private TextView tvCompanyName;
  private TextView tvCompanyAddress;
  private TextView tvCompanyPhone;
  private TextView tvShipperName;
  private TextView tvShipperEmail;
  private TextView tvMoveSummary;
  private ImageView ivCompanyLogo;

  private int totalItems;
  private int totalValue;
  private int totalPads;
  private float totalVolumeCubicFeet;
  private float totalWeightLbs;
  private int totalDamagedItems;

  private ImageLoader imageLoader;
  private Company company;
  private Job job;

  private void updateFromJob(){
    if (job == null)
      return; // this is an error

    tvShipperName.setText( job.getCustomerFirstName() + " " + job.getCustomerLastName());
    tvShipperEmail.setText(job.getCustomerEmail());
  }
  private void loadJob(){
    DatabaseObject<Job> jobRef = new DatabaseObject<Job>(Job.class, companyKey, jobKey);
    jobRef.addValueEventListener(new DatabaseObjectEventListener<Job>() {
      @Override
      public void onChange(String key, Job job) {
        SignOffActivity.this.job = job;
        updateFromJob();
        tvMoveSummary.setText(constructSummary());
      }
    });

  }

  private String formAddressString(Address address){
    String result = address.getStreet() + " " + address.getAddressLine2() + ", " + address.getCity() + ", " + address.getState() + " " + address.getZip();
    return result;
  }

  private void updateFromCompany(){
    if (company == null){
      return; // this is an error
    }
    tvCompanyName.setText(company.getName());
    String address = formAddressString(company.getAddress());
    tvCompanyAddress.setText(address);
    tvCompanyPhone.setText(company.getPhoneNumber());

    String logoImage = app().getCompanyLogoUrl();
    if (logoImage != null && logoImage.length() > 0){
      imageLoader.get(logoImage, ImageLoader.getImageListener(ivCompanyLogo,
              R.drawable.yourlogohere, R.drawable.yourlogohere));
    }

  }

  private String moveDestination(){
    String result = "";
    switch (job.getLifecycle()){
      case New:
        if (job.getStorageInTransit()){
          result = "Storage";
        } else {
          result =  formAddressString(job.getDestinationAddress());
        }
        break;
      case LoadedForStorage:
        result =  "Storage";
        break;
      case InStorage:
        result =  formAddressString(job.getDestinationAddress());
        break;
      case LoadedForDelivery:
        result =  formAddressString(job.getDestinationAddress());
        break;
      case Delivered:
        result =  formAddressString(job.getDestinationAddress());
        break;
    }
    return result;
  }

  private String deliveryWindow() {
    String result = "";
    switch (job.getLifecycle()) {
      case New:
        if (job.getStorageInTransit()) {
          result = "";
        } else {
          result = formAddressString(job.getDestinationAddress());
        }
        break;
      case LoadedForStorage:
        result = "";
        break;
      case InStorage:
        result = formAddressString(job.getDestinationAddress());
        break;
      case LoadedForDelivery:
        result = formAddressString(job.getDestinationAddress());
        break;
      case Delivered:
        result = formAddressString(job.getDestinationAddress());
        break;
    }
    if (result.length() == 0) {
      return result;
    } else {
      return "• " + result;
     }
  }

  private String constructSummary(){
    String summary =
                    "• " + Integer.toString(totalItems) + " Items valued at $" + totalValue + "\n" +
                    "• " + "Move destination is " + moveDestination() + "\n" +
                    deliveryWindow();
    return summary;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sign_off);

    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    entryLifecycle = Job.Lifecycle.valueOf(params.getString("lifecycle"));
    storageInTransit = params.getBoolean("storageInTransit");

    imageLoader = MyVolley.getImageLoader();

    totalItems = params.getInt("totalItems");
    totalValue = params.getInt("totalValue");
    totalPads = params.getInt("totalPads");
    totalVolumeCubicFeet = params.getFloat("totalVolumeCubicFeet");
    totalWeightLbs = params.getFloat("totalWeightLbs");
    totalDamagedItems = params.getInt("totalDamagedItems");

    tvCompanyName = (TextView) findViewById(R.id.tvCompanyName);
    tvCompanyAddress = (TextView) findViewById(R.id.tvCompanyAddress);
    tvCompanyPhone = (TextView) findViewById(R.id.tvCompanyPhone);
    tvShipperName = (TextView) findViewById(R.id.tvShipperName);
    tvShipperEmail = (TextView) findViewById(R.id.tvShipperEmail);
    tvMoveSummary = (TextView) findViewById(R.id.tvMoveSummary);
    ivCompanyLogo = (ImageView) findViewById(R.id.ivCompanyLogo);

    // we have to load both the company and job
    loadJob();

    company = app().getCurrentCompany();

    updateFromCompany();

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Sign Off");
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
      }
    });

    signaturePad = (SignaturePad) findViewById(R.id.signature_pad);
    clearPad = (Button) findViewById(R.id.buttonClearPad);
    accept = (Button) findViewById(R.id.buttonAcceptSignature);
    signHere = (TextView) findViewById(R.id.tvSignHere);
    tvName = (TextView) findViewById(R.id.tvCustomerName);

    tvTitle = (TextView) findViewById(R.id.tvTitle);
    tvTitle.setText(determineSignoffTitle());

    signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
      @Override
      public void onStartSigning() {

        Log.d(TAG, "onStartSigning");
        signHere.setVisibility(View.INVISIBLE);
      }

      @Override
      public void onSigned() {
        Log.d(TAG, "onSigned");
        accept.setEnabled(true);
        clearPad.setEnabled(true);

      }

      @Override
      public void onClear() {
        Log.d(TAG, "onClear");

        accept.setEnabled(false);
        clearPad.setEnabled(false);
        signHere.setVisibility(View.VISIBLE);
      }
    });

    accept.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (tvName.getText().length() == 0){
          tvName.setError("Please Enter Your Name");
          return;
        }
        tvName.setError(null);
        progressDialog  = ProgressDialog.show(thisActivity, "Saving Signature", "");
        progressDialog.show();

        Bitmap signaturePage = getScreenShot(getRootView());
        saveChanges(signaturePage, tvName.getText().toString());

      }
    });
    clearPad.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        signaturePad.clear();
        tvName.setError(null);
      }
    });

  }

  public  Bitmap getScreenShot(View view) {
    View screenView = view.getRootView();
    screenView.setDrawingCacheEnabled(true);
    Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
    screenView.setDrawingCacheEnabled(false);
    return bitmap;
  }


  private void saveChanges(Bitmap signatureBitmap, final String name){
    // move the job to Loaded,
    // add the customer signature
    FirebaseStorage storage = FirebaseStorage.getInstance();


    StorageReference customerSignature = storage.getReferenceFromUrl(app().getStorageUrl()).child("signatures/" + companyKey + "/" + jobKey + "/" + entryLifecycle);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 55, outputStream);
    final byte[] data = outputStream.toByteArray();
    UploadTask uploadTask = customerSignature.putBytes(data);
    uploadTask.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        Log.d(TAG, "upload failed");
        Utility.error(getRootView(), thisActivity, "Signature upload failed. Try again");
        progressDialog.hide();
      }
    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){

      @Override
      public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        // signature image upload succeeded. now we need to move the lifecycle and update signatures

        Job.Lifecycle nextState = getNextLifecyle();

        new DatabaseObject<Job>(Job.class, companyKey, jobKey).child("lifecycle").setValue(getNextLifecyle().toString());
        //FirebaseDatabase.getInstance().getReference("/jobs/" + jobKey + "/lifecycle").setValue(getNextLifecyle().toString());
        Uri downloadUrl = taskSnapshot.getDownloadUrl();
        //FirebaseDatabase.getInstance().getReference("/jobs/" + jobKey + "/signature" + entryLifecycle.toString()).setValue(new Signature(name, downloadUrl.toString()));
        new DatabaseObject<Job>(Job.class, companyKey, jobKey).child("signature" + entryLifecycle.toString()).setValue(new Signature(name, downloadUrl.toString()));
        progressDialog.hide();
        setResult(RESULT_OK);
        finish();
      }
    });



  }
  private String determineSignoffTitle(){
    if (entryLifecycle == Job.Lifecycle.New){
      return "Please sign off for items loaded.";
    } else if (entryLifecycle == Job.Lifecycle.LoadedForStorage ){
      return "Please sign off for items unloaded to warehouse.";
    } else if (entryLifecycle == Job.Lifecycle.InStorage){
      return "Please sign off for items loaded to truck for delivery";
    } else if (entryLifecycle == Job.Lifecycle.LoadedForDelivery){
      return "Please sign off for items unloaded at destination.";
    } else {
      // this case shouldn't occure
      return null;
    }
  }
  private Job.Lifecycle getNextLifecyle(){
    if (entryLifecycle == Job.Lifecycle.New && storageInTransit) {
      return Job.Lifecycle.LoadedForStorage;
    } else if (entryLifecycle == Job.Lifecycle.New && !storageInTransit) {
      return Job.Lifecycle.LoadedForDelivery;
    } else if (entryLifecycle == Job.Lifecycle.LoadedForStorage  ){
      return Job.Lifecycle.InStorage;
    }else if (entryLifecycle == Job.Lifecycle.InStorage ){
      return Job.Lifecycle.LoadedForDelivery;
    } else if (entryLifecycle == Job.Lifecycle.LoadedForDelivery){
      return Job.Lifecycle.Delivered;
    } else {
      // some error/ try to fail fast
      return null;
    }
  }
}
