package com.ranchosoftware.speedymovinginventory;

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
import android.widget.TextView;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sign_off);

    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    entryLifecycle = Job.Lifecycle.valueOf(params.getString("lifecycle"));
    storageInTransit = params.getBoolean("storageInTransit");
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


        Bitmap signatureBitmap = signaturePad.getSignatureBitmap();

        Bitmap tranparent = signaturePad.getTransparentSignatureBitmap(true);
        Log.d(TAG, "got bitmap");
        saveChanges(signatureBitmap, tvName.getText().toString());
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
        // TODO handl
        Log.d(TAG, "upload failed");
        Utility.error(getRootView(), thisActivity, "Signature upload failed. Try again");
      }
    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){

      @Override
      public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        // signature image upload succeeded. now we need to move the lifecycle and update signatures

        Job.Lifecycle nextState = getNextLifecyle();

        FirebaseDatabase.getInstance().getReference("/jobs/" + jobKey + "/lifecycle").setValue(getNextLifecyle().toString());
        Uri downloadUrl = taskSnapshot.getDownloadUrl();
        FirebaseDatabase.getInstance().getReference("/jobs/" + jobKey + "/signature" + entryLifecycle.toString()).setValue(new Signature(name, downloadUrl.toString()));

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
