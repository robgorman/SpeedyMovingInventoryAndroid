package com.ranchosoftware.speedymovinginventory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.utility.Permissions;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ItemClaimActivity extends BaseActivity {

  private static final String TAG = ItemClaimActivity.class.getSimpleName();

  private String companyKey;
  private String jobKey;
  private String qrcCode;
  private Item item;
  private GridView photoGridView;
  private ImageAdapter adapter;
  private TextView tvNoPhotosMessage;
  private CheckBox checkBoxDamaged;
  private EditText claimNumber;
  private EditText damageDescription;
  private CheckBox checkBoxIsBox;
  private TextView descriptionView;
  private TextView insuranceView;
  private CheckBox checkBoxActiveClaim;
  private ToggleButton buttonScanOverride;

  private MediaPlayer positivePlayer;
  private DatabaseReference itemRef;
  DatabaseReference qrcListRef;

  private ValueEventListener itemRefEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      if (dataSnapshot.getValue() == null){
        // there is some sort of error
        Log.d(TAG, "Serious Error");
        Utility.error(getRootView(), thisActivity, "Error: Item Detail Activity, unexpected null.");

      } else {
        item = dataSnapshot.getValue(Item.class);
        updateValuesFromItem();
        showProgress(false, findViewById(R.id.progressLayout), findViewById(R.id.itemFormLayout) );
      }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  };

  private void updateValuesFromItem(){
    // cleare the progrees bar/

    checkBoxIsBox.setChecked(item.getIsBox());
    descriptionView.setText(item.getDescription());
    checkBoxDamaged.setChecked(item.getHasClaim());
    damageDescription.setText(item.getDamageDescription());
    insuranceView.setText(item.getInsurance());
    claimNumber.setText(item.getClaimNumber());
    checkBoxActiveClaim.setChecked(item.getIsClaimActive());
    adapter.clear();
    for (String key : item.getImageReferences().keySet()){
      String stringUri = item.getImageReferences().get(key);
      adapter.add(new ImageAdapterItem(key, stringUri));
    }


    if ( !app().userIsAtLeastForeman()){
      buttonScanOverride.setVisibility(View.INVISIBLE);
    } else {
      buttonScanOverride.setVisibility(View.VISIBLE);
    }

    buttonScanOverride.setChecked(item.getIsScanned());
    adapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_item_claim);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Moving Item Claim Mgmt.");
    setSupportActionBar(toolbar);

    positivePlayer = MediaPlayer.create(thisActivity, R.raw.positive_beep);
    positivePlayer.setVolume(1.0f, 1.0f);

    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    qrcCode = params.getString("itemCode");

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    showProgress(true, findViewById(R.id.progressLayout), findViewById(R.id.itemFormLayout) );
    itemRef = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + qrcCode);
    qrcListRef = FirebaseDatabase.getInstance().getReference("qrcList/" + qrcCode);

    photoGridView = (GridView) findViewById(R.id.photoGridView);
    tvNoPhotosMessage = (TextView) findViewById(R.id.tvNoPhotosMessage);
    checkBoxDamaged = (CheckBox) findViewById(R.id.cbDamaged);

    claimNumber = (EditText) findViewById(R.id.tvClaimNumber);
    damageDescription = (EditText) findViewById(R.id.tvDamageDescription);
    checkBoxIsBox = (CheckBox) findViewById(R.id.cbIsBox);
    checkBoxActiveClaim = (CheckBox) findViewById(R.id.cbActiveClaim);
    insuranceView = (TextView) findViewById(R.id.tvInsurance);
    descriptionView = (TextView) findViewById(R.id.tvDescription);
    buttonScanOverride = (ToggleButton) findViewById(R.id.buttonScanOverride);

    buttonScanOverride.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (item != null){
          if (checked) {
            item.setIsScanned(checked);
            positivePlayer.start();
          } else {
            item.setIsScanned(false);
          }
        }
      }
    });

    FloatingActionButton addPhotos = (FloatingActionButton) findViewById(R.id.fabAddPhotos);
    addPhotos.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        attemptTakePhoto();
      }
    });

    adapter = new ImageAdapter(thisActivity, new ArrayList<ImageAdapterItem>());
    photoGridView.setVisibility(View.INVISIBLE);
    photoGridView.setAdapter(adapter);
  }


  private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;
  private static final int TAKE_PHOTO_CODE = 5;

  private void attemptTakePhoto() {
    if (Permissions.hasPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      launchCameraForResult();
    } else {
      Permissions.requestPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
              REQUEST_WRITE_EXTERNAL_STORAGE);
    }
  }
  private void launchCameraForResult() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        createImageFile();
        photoFile = imageFile;

      } catch (IOException ex) {
        // Error occurred while creating the File
        int x = 5;
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
      }
    }
  }


  private File imageFile;
  private void createImageFile() throws IOException {
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";

    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
    );
    imageFile = image;

  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_WRITE_EXTERNAL_STORAGE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

          // permission was granted, yay! Do the
          // contacts-related task you need to do.
          launchCameraForResult();

        } else {

          DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dialog.dismiss();
            }
          };

          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("Speedy Moving Inventory")
                  .setMessage(R.string.no_external_storage_permission)
                  .setPositiveButton(R.string.ok, listener)
                  .show();
        }
        return;
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == TAKE_PHOTO_CODE && resultCode == Activity.RESULT_OK) {

      handleTakePictureResult();

    }
  }

  private Bitmap rotateImage(Bitmap source, float angle){
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

  }


  private Bitmap scaleBitmapIfNecessary(Bitmap bitmap){
    int maxSize = Math.max(bitmap.getWidth(), bitmap.getHeight());
    // bitmap to be at most 800
    int newWidth = 0;
    int newHeight = 0;
    if (maxSize > 800){
      if (bitmap.getWidth() > bitmap.getHeight()){
        newWidth = 800;
        newHeight = (int) (bitmap.getHeight() * (800.0/ (double)bitmap.getWidth()));
      } else {
        newHeight = 800;
        newWidth = (int) (bitmap.getWidth() * (800.0/(double) bitmap.getHeight()));
      }
      bitmap = bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }
    return bitmap;
  }

  private void handleTakePictureResult(){
    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.toString());
    bitmap = scaleBitmapIfNecessary(bitmap);
    try {
      ExifInterface ei = new ExifInterface(imageFile.toString());
      int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

      switch(orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          bitmap = rotateImage(bitmap, 90);
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          bitmap = rotateImage(bitmap, 180);
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          bitmap = rotateImage(bitmap, 270);
          break;
      }
      // etc.
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (bitmap != null) {
      //
      writeBitmapBackToFile(bitmap);
      //photoGridView
      //adapter.add(imageFile.toString());
      //adapter.notifyDataSetChanged();
      FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
      StorageReference storageRef  = firebaseStorage.getReferenceFromUrl(app().getStorageUrl());

      DateTime dateTime = new DateTime();
      final long milliseconds = dateTime.getMillis();


      final StorageReference imagesRef = storageRef.child("images/"+ companyKey + "/" + jobKey + "/" + qrcCode + "/" + Long.toString(milliseconds));

      Uri fileUri = Uri.fromFile(imageFile);
      UploadTask uploadTask = imagesRef.putFile(fileUri);
      uploadTask.addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {

          // updload not successfule
          imageFile.delete();
        }
      }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
        @Override
        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

          Uri downloadUrl = taskSnapshot.getDownloadUrl();
          item.getImageReferences().put(Long.toString(milliseconds), downloadUrl.toString());
          itemRef.child("imageReferences").setValue(item.getImageReferences());
          imageFile.delete();
        }
      });



    } else {
      Toast.makeText(this, "Take Before failed", Toast.LENGTH_SHORT).show();
    }
  }

  private void writeBitmapBackToFile(Bitmap bitmap){
    try {
      FileOutputStream out = new FileOutputStream(imageFile.toString());
      bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
      out.flush();
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private class ImageAdapterItem {
    private String dateKey;
    private String imageUrl;
    public ImageAdapterItem(String dateKey, String imageUrl){
      this.dateKey = dateKey;
      this.imageUrl = imageUrl;
    }
  }

  private class ImageAdapter extends ArrayAdapter<ImageAdapterItem> {
    private Context context;
    private LayoutInflater inflater;


    public ImageAdapter(Context context, List<ImageAdapterItem> images) {
      super(context, R.layout.item_photo_grid_item, images);
      this.context = context;
      inflater = LayoutInflater.from(context);
    }

    // comment


    @Override
    public int getItemViewType(int position) {
      int type =  super.getItemViewType(position);
      return type;
    }

    @Override
    public boolean hasStableIds() {
      boolean answer =  super.hasStableIds();
      return answer;
    }

    @Override
    public int getViewTypeCount() {
      int count =  super.getViewTypeCount();
      return count;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
      photoGridView.setVisibility(View.VISIBLE);
      tvNoPhotosMessage.setVisibility(View.INVISIBLE);
      if (convertView == null){
        convertView = inflater.inflate(R.layout.item_photo_grid_item, null);
      }

      ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
      TextView timestamp = (TextView) convertView.findViewById(R.id.tvTimeStamp);

      ImageAdapterItem item = getItem(position);
      if (timestamp != null){
        timestamp.setText(app().getImageDateTimeFormatter().print(Long.parseLong(item.dateKey)));
      }


      ImageLoader loader = MyVolley.getImageLoader();
      loader.get(item.imageUrl, ImageLoader.getImageListener(imageView,
              R.drawable.loading, R.drawable.load_failed));

      return convertView;
    }
  }
  @Override
  public void onStart() {
    super.onStart();
    itemRef.addValueEventListener(itemRefEventListener);
  }

  private void updateItemFromControls(){
    item.setIsClaimActive(checkBoxActiveClaim.isChecked());
    item.setHasClaim(checkBoxDamaged.isChecked());
    item.setClaimNumber(claimNumber.getText().toString());
    item.setDamageDescription(damageDescription.getText().toString());
    item.setIsClaimActive(checkBoxActiveClaim.isChecked());

  }

  @Override
  public void onStop() {
    super.onStop();
    updateItemFromControls();
    if (item != null) {
      itemRef.setValue(item);
    }
    qrcListRef.setValue(jobKey);
    itemRef.removeEventListener(itemRefEventListener);
  }
}
