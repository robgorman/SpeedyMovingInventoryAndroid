package com.ranchosoftware.speedymovinginventory;

import android.Manifest;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.ranchosoftware.speedymovinginventory.utility.Permissions;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NewItemActivity extends BaseActivity {

  private String jobKey;
  private String qrcCode;
  private String companyKey;
  private EditText descriptionEdit;
  private Spinner categorySpinner;
  private CheckBox isBoxCheck;
  private View numberOfPadsLayout;
  private Spinner insuranceSpinner;
  private SeekBar numberOfPadsSeek;
  private TextView numberOfPadsTextView;
  private SeekBar monetaryValueSeek;
  private TextView monetaryValueTextView;
  private ToggleButton syncWeightAndVolumeButton;
  private SeekBar weightSeek;
  private TextView weightTextView;
  private SeekBar volumeSeek;
  private TextView volumeTextView;
  private EditText specialHandlingEdit;
  private GridView photoGridView;
  private ImageAdapter adapter;
  private Item item;
  private boolean syncWeightAndVolume;
  private TextView tvNoPhotosMessage;

  DatabaseReference itemRef;
  DatabaseReference qrcListRef;

  ToggleButton syncVolumeWeightButton;
  /**
   * ATTENTION: This was auto-generated to implement the App Indexing API.
   * See https://g.co/AppIndexing/AndroidStudio for more information.
   */
  private GoogleApiClient client;

  private ValueEventListener itemRefEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      if (dataSnapshot.getValue() == null){
        Item item = createNewItem();
        itemRef.setValue(item);
        qrcListRef.setValue(jobKey);
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


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_item);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Edit Moving Item");
    setSupportActionBar(toolbar);

    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    qrcCode = params.getString("itemCode");

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    showProgress(true, findViewById(R.id.progressLayout), findViewById(R.id.itemFormLayout) );
    itemRef = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + qrcCode);
    qrcListRef = FirebaseDatabase.getInstance().getReference("qrcList/" + qrcCode);

    //itemRef.addValueEventListener(itemRefEventListener);

    FloatingActionButton takePhoto = (FloatingActionButton) findViewById(R.id.fab_takePhoto);
    takePhoto.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        attemptTakePhoto();
      }
    });
    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    descriptionEdit = (EditText) findViewById(R.id.etDescription);
    categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
    isBoxCheck = (CheckBox) findViewById(R.id.cbIsBox);
    insuranceSpinner = (Spinner) findViewById(R.id.insuranceSpinner);
    numberOfPadsLayout = findViewById(R.id.padsLayout);
    numberOfPadsSeek = (SeekBar) findViewById(R.id.seekNumberOfPads);
    numberOfPadsTextView = (TextView) findViewById(R.id.tvNumberOfPads);
    monetaryValueSeek = (SeekBar) findViewById(R.id.seekMonetaryValue);
    monetaryValueTextView = (TextView) findViewById(R.id.tvMonetaryValue);
    weightSeek = (SeekBar) findViewById(R.id.seekWeight);
    weightTextView = (TextView) findViewById(R.id.tvWeight);
    volumeSeek = (SeekBar) findViewById(R.id.seekVolume);
    volumeTextView = (TextView) findViewById(R.id.tvVolume);
    syncVolumeWeightButton = (ToggleButton) findViewById(R.id.weightVolumeSyncButton);
    photoGridView = (GridView) findViewById(R.id.photoGridView);
    specialHandlingEdit = (EditText) findViewById(R.id.etSpecialHandling);
    tvNoPhotosMessage = (TextView) findViewById(R.id.tvNoPhotosMessage);

    isBoxCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (item != null){
          item.setIsBox(isChecked);
          if (isChecked){
            item.setNumberOfPads(0);
          } else {

          }
        }
        if (isChecked){
          numberOfPadsLayout.setVisibility(View.GONE);
        } else {
          numberOfPadsLayout.setVisibility(View.VISIBLE);
        }
      }
    });

    adapter = new ImageAdapter(thisActivity, new ArrayList<String>());
    photoGridView.setAdapter(adapter);
    photoGridView.setVisibility(View.INVISIBLE);

    setupCategorySpinner();
    setupInsuranceSpinner();
    setupNumberOfPads();
    setupMonetaryValue();
    setupWeightAndVolume();


    syncVolumeWeightButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
          syncWeightAndVolume = true;
        } else {
          syncWeightAndVolume = false;
        }
      }
    });

  }

  private Item createNewItem() {
    // item does not exist, create it
    Item.Category newItemCategory = app().getCurrentCategory();
    if (newItemCategory == null){
      newItemCategory = Item.Category.Bedroom1;
      app().setCurrentCategory(newItemCategory);
    }
    // TODO need insurance
    Item item = new Item(newItemCategory, 0, app().getCurrentUser().getUid(), "",
            Item.Defaults.monetaryValue(),
            Item.Defaults.weightLbs(),
            Item.Defaults.volume(),
            "",
            jobKey,
            Item.Defaults.packedBy(), "None", false);
    return item;
  }

  private void updateItemFromControls(){
    // keep item up to date, thats probably better than this
    item.setDescription(descriptionEdit.getText().toString());
    // skip spinners they keep item up to date
    // box is already ond
    item.setSpecialHandling(specialHandlingEdit.getText().toString());

  }

  private void updateValuesFromItem(){
    int volume = getVolumeProgessFromValue(item.getVolume());
    volumeSeek.setProgress(volume);

    int weight = getWeightProgessFromValue(item.getWeightLbs());
    weightSeek.setProgress(weight);

    numberOfPadsSeek.setProgress(item.getNumberOfPads()-1);

    int monetaryValue = getMonetaryValueProgessFromValue(item.getMonetaryValue());
    monetaryValueSeek.setProgress(monetaryValue);

    // just in case not found
   // categorySpinner.setSelection(0);
    for (int i = 0; i < categorySpinner.getAdapter().getCount(); i++){
      if (categorySpinner.getAdapter().getItem(i).equals(item.getCategory())){
        categorySpinner.setSelection(i);
        break;

      }
    }
    descriptionEdit.setText(item.getDescription());
    specialHandlingEdit.setText(item.getSpecialHandling());
    for (int i = 0; i < insuranceSpinner.getAdapter().getCount(); i++){
      if (insuranceSpinner.getAdapter().getItem(i).equals(item.getInsurance())){
        insuranceSpinner.setSelection(i);
        break;

      }
    }

    isBoxCheck.setChecked(item.getIsBox());


    adapter.clear();
    for (String key : item.getImageReferences().keySet()){
      String stringUri = item.getImageReferences().get(key);
      adapter.add(stringUri);
    }

    adapter.notifyDataSetChanged();

  }


  private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;
  private static final int TAKE_PHOTO_CODE = 5;

  private void attemptTakePhoto() {
    if (Permissions.hasPermission(thisActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      launchCameraForResult();
    } else {
      Permissions.requestPermission(thisActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE,
              REQUEST_WRITE_EXTERNAL_STORAGE);
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


  private static final int possibleWeights[] = {1, 2, 5, 10, 20, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
  private void setupWeightAndVolume(){
    // 1 thru 10 so 0..9
    setupWeight();
    setupVolume();
  }
  private static final int possibleVolumes[] = {1, 2, 3, 4, 5, 7, 10, 20, 30, 40, 50, 100, 150, 200, 300, 400, 500};
  private void setupVolume() {
    volumeSeek.setMax(possibleVolumes.length-1);
    volumeSeek.setProgress(possibleVolumes.length);
    volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {



        String styled = Integer.toString(possibleVolumes[progress]) + " ft3" ;

        SpannableStringBuilder superScript = new SpannableStringBuilder(styled);
        superScript.setSpan(new SuperscriptSpan(),styled.length() -1, styled.length(), 0);
        superScript.setSpan(new RelativeSizeSpan(0.5f), styled.length() -1, styled.length(), 0);

        volumeTextView.setText(superScript);
        item.setVolume(possibleVolumes[progress]);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });

  }

  private int getVolumeProgessFromValue(Integer targetValue){
    int i = 0;
    for (int next : possibleVolumes){
      if (targetValue == next){
        return i;
      }
      i += 1;
    }
    // its an error if we get here, but just return the lowest value
    return 0;
  }

  private void setupWeight() {
    weightSeek.setMax(possibleWeights.length-1);
    weightSeek.setProgress(possibleWeights.length );
    weightSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        weightTextView.setText(Integer.toString(possibleWeights[progress]));
        item.setWeightLbs(possibleWeights[progress]);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });


  }

  private int getWeightProgessFromValue(Integer targetValue){
    int i = 0;
    for (int next : possibleWeights){
      if (targetValue == next){
        return i;
      }
      i += 1;
    }
    // its an error if we get here, but just return the lowest value
    return 0;
  }
  private void setupNumberOfPads(){
    // 1 thru 10 so 0..9
    numberOfPadsSeek.setMax(9);
    numberOfPadsSeek.setProgress(10);
    numberOfPadsSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        numberOfPadsTextView.setText(Integer.toString(progress+1));
        item.setNumberOfPads(progress+1);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });


  }
  private static final int monetaryValues[] = {1, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000};



  private void setupMonetaryValue() {
    monetaryValueSeek.setProgress(monetaryValues.length);
    monetaryValueSeek.setMax(monetaryValues.length-1);

    monetaryValueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        item.setMonetaryValue(monetaryValues[progress]);
        monetaryValueTextView.setText("$"+Integer.toString(monetaryValues[progress]));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
  }

  private void setupInsuranceSpinner() {
    // fill category spinner
    final List<String> insuranceOptions = new ArrayList<String>();
    for (Item.Insurance ins : Item.Insurance.values())
    {
      insuranceOptions.add(ins.toString());
    }
    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(thisActivity, android.R.layout.simple_spinner_item, insuranceOptions );
    insuranceSpinner.setAdapter(categoryAdapter);
    insuranceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //itemRef.child("category").setValue(categories.get(position));
        item.setInsurance(insuranceOptions.get(position));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

  }

  private void setupCategorySpinner() {
    // fill category spinner
    final List<String> categories = new ArrayList<String>();
    for (Item.Category cat : Item.Category.values())
    {
      categories.add(cat.toString());
    }
    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(thisActivity, android.R.layout.simple_spinner_item, categories );
    categorySpinner.setAdapter(categoryAdapter);
    // TODO this might not work
    categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //itemRef.child("category").setValue(categories.get(position));
        item.setCategory(categories.get(position));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

  }
  private int getMonetaryValueProgessFromValue(Integer targetValue){
    int i = 0;
    for (int next : monetaryValues){
      if (targetValue == next){
        return i;
      }
      i += 1;
    }
    // its an error if we get here, but just return the lowest value
    return 0;
  }

  /**
   * ATTENTION: This was auto-generated to implement the App Indexing API.
   * See https://g.co/AppIndexing/AndroidStudio for more information.
   */
  public Action getIndexApiAction() {
    Thing object = new Thing.Builder()
            .setName("NewItem Page") // TODO: Define a title for the content shown.
            // TODO: Make sure this auto-generated URL is correct.
            .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
            .build();
    return new Action.Builder(Action.TYPE_VIEW)
            .setObject(object)
            .setActionStatus(Action.STATUS_TYPE_COMPLETED)
            .build();
  }

  @Override
  public void onStart() {
    super.onStart();
    itemRef.addValueEventListener(itemRefEventListener);

    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    client.connect();
    AppIndex.AppIndexApi.start(client, getIndexApiAction());
  }

  @Override
  public void onStop() {
    super.onStop();
    updateItemFromControls();
    itemRef.setValue(item);
    qrcListRef.setValue(jobKey);
    itemRef.removeEventListener(itemRefEventListener);

    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    AppIndex.AppIndexApi.end(client, getIndexApiAction());
    client.disconnect();
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
      final Long milliseconds = dateTime.getMillis();

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

  private static class KeyValue{
    String key;
    String value;
    public KeyValue(String key, String value){
      this.key = key;
      this.value = value;
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

  private class ImageAdapter extends ArrayAdapter<String> {
    private Context context;
    private LayoutInflater inflater;


    public ImageAdapter(Context context, List<String> images) {
      super(context, 0, images);
      this.context = context;
      inflater = LayoutInflater.from(context);
    }

    // comment


    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
      photoGridView.setVisibility(View.VISIBLE);
      tvNoPhotosMessage.setVisibility(View.INVISIBLE);
      if (convertView == null){
        convertView = inflater.inflate(R.layout.item_photo_grid_item, null);
      }


      ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);

      ImageLoader loader = MyVolley.getImageLoader();
      loader.get(getItem(position), ImageLoader.getImageListener(imageView,
              R.drawable.loading, R.drawable.load_failed));

      return imageView;
    }

  }
}
