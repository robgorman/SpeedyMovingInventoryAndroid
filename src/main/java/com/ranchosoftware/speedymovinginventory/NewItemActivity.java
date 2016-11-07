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
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
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
import android.widget.Switch;
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
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObject;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObjectEventListener;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.MovingItemDataDescription;
import com.ranchosoftware.speedymovinginventory.utility.Permissions;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NewItemActivity extends BaseActivity {

  private static String TAG = NewItemActivity.class.getSimpleName();

  private String jobKey;
  private String qrcCode;
  private String companyKey;
  private EditText descriptionEdit;
  private CheckBox isBoxCheck;
  private View numberOfPadsLayout;
  private Spinner insuranceSpinner;
  private SeekBar numberOfPadsSeek;
  private TextView numberOfPadsTextView;
  private SeekBar monetaryValueSeek;
  private TextView monetaryValueTextView;
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
  private Button categoryButton;
  private Button packedByButton;

  private CheckBox isDisassembled;
  private Button assist;
  private EditText damageDescription;

  //DatabaseObject<Item> itemRef;
  DatabaseReference itemRef;
  DatabaseReference qrcListRef;
  //DatabaseObject<String> qrcListRef;

  private Switch syncVolumeWeightSwitch;
  /**
   * ATTENTION: This was auto-generated to implement the App Indexing API.
   * See https://g.co/AppIndexing/AndroidStudio for more information.
   */
  private GoogleApiClient client;


  private ValueEventListener itemRefEventListener = new ValueEventListener(){

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

      if (!dataSnapshot.exists()){
        Item item = createNewItem();
        itemRef.setValue(item);
        qrcListRef.setValue(jobKey, new DatabaseReference.CompletionListener() {
          @Override
          public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

            if (databaseError != null) {
              Bundle bundle = new Bundle();
              bundle.putString("error_details", databaseError.getDetails());
              bundle.putString("error_message", databaseError.getMessage());
              FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(NewItemActivity.this);
              analytics.logEvent("qrclist_write_error" , bundle );
            }
          }
        });

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
    toolbar.setTitle("New Item");
    setSupportActionBar(toolbar);

    Bundle params = getIntent().getExtras();
    companyKey = params.getString("companyKey");
    jobKey = params.getString("jobKey");
    qrcCode = params.getString("itemCode");

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    showProgress(true, findViewById(R.id.progressLayout), findViewById(R.id.itemFormLayout) );
    itemRef = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + qrcCode);
    qrcListRef = FirebaseDatabase.getInstance().getReference("qrcList/" + qrcCode);

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
    syncVolumeWeightSwitch = (Switch) findViewById(R.id.weightVolumeSynchSwitch);
    photoGridView = (GridView) findViewById(R.id.photoGridView);
    specialHandlingEdit = (EditText) findViewById(R.id.etSpecialHandling);
    tvNoPhotosMessage = (TextView) findViewById(R.id.tvNoPhotosMessage);

    assist = (Button) findViewById(R.id.assistButton);
    assist.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        launchMovingItemDescriptionEntryActivity();
      }
    });

    isBoxCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (item != null){
          item.setIsBox(isChecked);
          if (isChecked){
            item.setNumberOfPads(0);
          }
        }
        updateValuesFromItem();
      }
    });

    descriptionEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void afterTextChanged(Editable editable) {

        String newText = editable.toString();
        item.setDescription(newText);
      }
    });

    damageDescription = (EditText) findViewById(R.id.editDamageDescription);
    damageDescription.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void afterTextChanged(Editable editable) {
        String newText = editable.toString();
        item.setDamageDescription(newText);
      }
    });

    specialHandlingEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void afterTextChanged(Editable editable) {
        String newText = editable.toString();
        item.setSpecialHandling(newText);
      }
    });
    adapter = new ImageAdapter(thisActivity, new ArrayList<String>());
    photoGridView.setAdapter(adapter);
    photoGridView.setVisibility(View.INVISIBLE);

    setupInsuranceSpinner();
    setupNumberOfPads();
    setupMonetaryValue();
    setupWeightAndVolumeSliders();

    syncVolumeWeightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
          syncWeightAndVolume = true;
          item.setSyncWeightAndVolume(true);
        } else {
          syncWeightAndVolume = false;
          item.setSyncWeightAndVolume(false);
        }
      }
    });


    categoryButton = (Button) findViewById(R.id.categoryButton);
    categoryButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        launchSpinnerActivityForResult();

      }
    });

    packedByButton = (Button) findViewById(R.id.packedByButton);
    packedByButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        launchPackedBySpinnerForResult();
      }
    });

    isDisassembled = (CheckBox) findViewById(R.id.cbDisassembled);
    isDisassembled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
        item.setIsDisassembled(true);
        itemRef.setValue(item);
      }
    });

  }

  // TODO we should really pull these strings from resources.
  private String categoryToRoom(Item.Category category){
    switch (category){
      case Basement:
        return MovingItemDataDescription.Room.Basement.name();
      case Bedroom1:
      case Bedroom2:
      case Bedroom3:
      case Bedroom4:
      case Bedroom5:
        return MovingItemDataDescription.Room.Bedroom.name();

      case Garage:
        return MovingItemDataDescription.Room.Garage.name();

      case DiningRoom:
        return MovingItemDataDescription.Room.DiningRoom.name();

      case Den:
        return MovingItemDataDescription.Room.Den.name();

      case Office:
        return MovingItemDataDescription.Room.Office.name();

      case LivingRoom:
        return MovingItemDataDescription.Room.LivingRoom.name();

      case Kitchen:
        return MovingItemDataDescription.Room.Kitchen.name();

      case Bathroom:
        return MovingItemDataDescription.Room.Bathroom.name();

      case Patio:
        return MovingItemDataDescription.Room.Patio.name();

      case Sunroom:
        return MovingItemDataDescription.Room.Sunroom.name();

      case Laundry:
        return MovingItemDataDescription.Room.Laundry.name();

      case Nursery:
        return MovingItemDataDescription.Room.Nursery.name();

      case Other:
        return MovingItemDataDescription.Room.Other.name();

      default:
        assert(false);
        return MovingItemDataDescription.Room.Other.name();

    }
  }

  private void launchMovingItemDescriptionEntryActivity(){
    Intent intent = new Intent(this, MovingItemPickDescriptionActivity.class);
    Item.Category cat = item.getCategoryEnum();
    intent.putExtra("room", categoryToRoom(cat));
    startActivityForResult(intent, PICK_AN_MOVING_ITEM_DESCRIPTION);
    overridePendingTransition(R.xml.slide_in_from_right,R.xml.slide_out_to_left);
  }

  private static int PICK_A_CATEGORY = 345;
  private static int PICK_A_PACKED_BY = 346;
  private static int PICK_AN_MOVING_ITEM_DESCRIPTION = 347;

  private void launchPackedBySpinnerForResult(){
    Intent intent = new Intent(this, SpinnerActivity.class);
    Bundle b = new Bundle();
    b.putInt(SpinnerActivity.paramSelectedIndex,  item.getPackedByEnum().ordinal());

    String labels[] = new String[Item.PackedBy.values().length];

    for (Item.PackedBy cat : Item.PackedBy.values()){
      labels[cat.ordinal()] = cat.name();
    }

    b.putStringArray(SpinnerActivity.paramLabels, labels);
    b.putString(SpinnerActivity.paramTitle, "Choose Packed By");
    intent.putExtras(b);

    startActivityForResult(intent, PICK_A_PACKED_BY);
    overridePendingTransition(R.xml.slide_in_from_right,R.xml.slide_out_to_left);
  }


  private void launchSpinnerActivityForResult(){
    Intent intent = new Intent(this, SpinnerActivity.class);
    Bundle b = new Bundle();
    b.putInt(SpinnerActivity.paramSelectedIndex,  item.getCategoryEnum().ordinal());

    String labels[] = new String[Item.Category.values().length];

    for (Item.Category cat : Item.Category.values()){
      labels[cat.ordinal()] = cat.name();
    }

    b.putStringArray(SpinnerActivity.paramLabels, labels);
    b.putString(SpinnerActivity.paramTitle, "Choose Category");
    intent.putExtras(b);

    startActivityForResult(intent, PICK_A_CATEGORY);
    overridePendingTransition(R.xml.slide_in_from_right,R.xml.slide_out_to_left);
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

  private void updateInterfaceFromItem(){
    if (item.getIsBox()){
      numberOfPadsLayout.setVisibility(View.GONE);

    } else {
      numberOfPadsLayout.setVisibility(View.VISIBLE);
    }
    setupWeightAndVolumeSliders();
  }


  private void updateValuesFromItem(){

    updateInterfaceFromItem();

    syncWeightAndVolume = item.getSyncWeightAndVolume();
    syncVolumeWeightSwitch.setChecked(syncWeightAndVolume);

    int volumeProgress = getVolumeProgessFromValue(item.getVolume());
    volumeSeek.setProgress(volumeProgress);

    int weightProgress = getWeightProgessFromValue(item.getWeightLbs());
    weightSeek.setProgress(weightProgress);

    numberOfPadsSeek.setProgress(item.getNumberOfPads());

    int prog = getMonetaryValueProgessFromValue(item.getMonetaryValue());
    monetaryValueSeek.setProgress(prog);

    damageDescription.setText(item.getDamageDescription());

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

    categoryButton.setText(item.getCategory() + " >");
    packedByButton.setText(item.getPackedBy() + " >");

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
        Uri photoUri = FileProvider.getUriForFile(thisActivity, BuildConfig.APPLICATION_ID + ".provider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
      }
    }
  }

  private int convertPoundsToProgress(float pounds){
    for (int i = 0; i < possibleWeights.length; i++){
      float next = possibleWeights[i];
      if (next > pounds){
        float delta1 = Math.abs(next - pounds);
        if (i == 0){
          return i;
        }
        float delta2 = Math.abs(next - possibleWeights[i-1] );

        if (delta1 < delta2){
          return i;
        } else {
          return i-1;
        }
      }
    }

    return possibleWeights.length - 1;
  }

  private int convertCubicFeetToProgess(float cubicFeet){
    for (int i = 0; i < possibleVolumes.length; i++){
      float next = possibleVolumes[i];
      if (next > cubicFeet){
        float delta1 =  Math.abs(next - cubicFeet);
        if (i == 0){
          return i;
        }
        float delta2 = Math.abs(next - cubicFeet);
        if (delta1 < delta2){
          return i;
        } else {
          return i-1;
        }
      }
    }
    return possibleVolumes.length - 1;
  }


  private static final float possibleWeights[] = {1, 2, 5, 10, 20, 50, 100, 200, 300, 400, 500, 700};
  private void setupWeightAndVolumeSliders(){

    if (item != null && item.getIsBox()){
      possibleVolumes = boxVolumes;
    } else {
      possibleVolumes = normalVolumes;
    }

    int lbsPerCubicFoot = app().getCompanyPoundsPerCubicFoot();
    assert(possibleWeights.length >= possibleVolumes.length);

    for (int i = 0; i < possibleVolumes.length; i++){
      possibleWeights[i] = possibleVolumes[i] * lbsPerCubicFoot;
    }
    setupWeight();
    setupVolume();
  }
  private static float possibleVolumes[] =     {1, 3, 5, 7, 10, 20, 30, 40, 50, 60, 75, 100};
  private static final float normalVolumes[] = {1, 3, 5, 7, 10, 20, 30, 40, 50, 60, 75, 100};
  private static final float boxVolumes[] = {1.5f, 3.0f, 4.5f, 6.0f };
  private static String boxNames[] = {"Small", "Medium", "Large", "XLarge"};
  private void setupVolume() {

    // set out of range so on change fires for first value
    if (item != null){
      volumeSeek.setProgress(getVolumeProgessFromValue(item.getVolume()));
    } else {
      volumeSeek.setProgress(possibleVolumes.length);
    }
    volumeSeek.setMax(possibleVolumes.length-1);


    volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        float cubicFeet = possibleVolumes[progress];
        String styled = String.format("%.1f",possibleVolumes[progress]) + " ft3" ;

        SpannableStringBuilder superScript = new SpannableStringBuilder(styled);
        superScript.setSpan(new SuperscriptSpan(),styled.length() -1, styled.length(), 0);
        superScript.setSpan(new RelativeSizeSpan(0.5f), styled.length() -1, styled.length(), 0);

        // if the change is from the user and we are syching, change the weight seek
        if (fromUser && syncWeightAndVolume){
          weightSeek.setProgress(weightProgressFromVolume(cubicFeet));
        }

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

  private int weightProgressFromVolume(float cubicFeet){

    int lbsPerCubicFoot = app().getCompanyPoundsPerCubicFoot();
    float pounds = cubicFeet * lbsPerCubicFoot;
    // how to convert to
    int progress = convertPoundsToProgress(pounds);
    return progress;
  }

  private int volumeProgessFromWeight(float weight){
    int lbsPerCubicFoot = app().getCompanyPoundsPerCubicFoot();
    float cubicFeet = weight / lbsPerCubicFoot;
    // how to convert to
    int progress = convertCubicFeetToProgess(cubicFeet);
    return progress;
  }

  boolean isClose(float left, float right){
    float delta = Math.abs(left - right);
    return delta < .5;
  }
  private int getVolumeProgessFromValue(float targetValue){
    int i = 0;
    for (float next : possibleVolumes){
      if (isClose(targetValue, next)){
        return i;
      }
      i += 1;
    }
    // its an error if we get here, but just return the lowest value
    return 0;
  }

  private void setupWeight() {
    weightSeek.setMax(possibleWeights.length-1);
    // set out of range so on change fires for first value
    if (item != null){
      weightSeek.setProgress(getWeightProgessFromValue(item.getWeightLbs()));
    } else {
      weightSeek.setProgress(possibleWeights.length);
    }

    weightSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        float weight = possibleWeights[progress];

        // if the change is from the user and we are syching, change the weight seek
        if (fromUser && syncWeightAndVolume){
          volumeSeek.setProgress(volumeProgessFromWeight(weight));
        }

        weightTextView.setText(String.format("%.0f",weight) + " lbs.");
        item.setWeightLbs(possibleWeights[progress]);
        // for the present with no insurance the value is always .60 per pound
        item.setMonetaryValue(possibleWeights[progress]* 0.60f);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
  }

  private int getWeightProgessFromValue(float targetValue){
    int i = 0;
    for (float next : possibleWeights){
      if (isClose(targetValue, next)){
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
        numberOfPadsTextView.setText(Integer.toString(progress));
        item.setNumberOfPads(progress);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });

  }
  private static final Float monetaryValues[] =
          {0.0f, 1.0f, 5.0f, 10.0f, 20.0f, 50.0f, 100.0f, 200.0f, 500.0f, 1000.0f, 2000.0f, 5000.0f};

  private void setupMonetaryValue() {
    monetaryValueSeek.setProgress(monetaryValues.length);
    monetaryValueSeek.setMax(monetaryValues.length-1);

    monetaryValueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        item.setMonetaryValue(monetaryValues[progress]);
        monetaryValueTextView.setText("$"+String.format("%.2f",monetaryValues[progress]));
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
    // fill insurance spinner
    final List<String> insuranceOptions = new ArrayList<String>();
    for (Item.Insurance ins : Item.Insurance.values())
    {
      insuranceOptions.add(ins.toString());
    }
    ArrayAdapter<String> insuranceAdapter = new ArrayAdapter<String>(thisActivity, android.R.layout.simple_spinner_item, insuranceOptions );
    insuranceSpinner.setAdapter(insuranceAdapter);
    insuranceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        item.setInsurance(insuranceOptions.get(position));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

  }

  private boolean isNear(Float target, Float value){
    if (Math.abs(target - value) < .5){
      return true;
    }
    return false;

  }

  private int getMonetaryValueProgessFromValue(Float targetValue){
    int i = 0;
    for (Float next : monetaryValues){
      if (isNear(targetValue, next)){
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

    itemRef.setValue(item);
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
    }
  }

  private void setCategory(int selectedIndex){
    final List<String> categories = new ArrayList<String>();
    for (Item.Category cat : Item.Category.values())
    {
      categories.add(cat.toString());
    }
    item.setCategory(categories.get(selectedIndex));
    app().setCurrentCategory(Item.Category.valueOf(categories.get(selectedIndex)));
    itemRef.setValue(item);
  }

  private void setPackedBy(int selectedIndex){
    final List<String> packedBys = new ArrayList<String>();
    for (Item.PackedBy by : Item.PackedBy.values())
    {
      packedBys.add(by.toString());
    }
    item.setPackedBy(packedBys.get(selectedIndex));

    itemRef.setValue(item);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == TAKE_PHOTO_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        handleTakePictureResult();
      }
    } else if (requestCode == PICK_A_CATEGORY){
      if (resultCode == Activity.RESULT_OK){
        // extract selected index
        int selectedIndex = data.getIntExtra(SpinnerActivity.paramSelectedIndex, 0);
        setCategory(selectedIndex);
      }
    } else if (requestCode == PICK_A_PACKED_BY){
      if (resultCode == Activity.RESULT_OK){
        // extract selected index
        int selectedIndex = data.getIntExtra(SpinnerActivity.paramSelectedIndex, 0);
        setPackedBy(selectedIndex);
      }
    } else if (requestCode == PICK_AN_MOVING_ITEM_DESCRIPTION){
      if (resultCode == Activity.RESULT_OK){
        // extrace data
        String itemName = data.getStringExtra("itemName");
        Float weightLbs = data.getFloatExtra("weightLbs", 0);
        Float cubicFeet = data.getFloatExtra("cubicFeet", 0);
        Boolean isBox = data.getBooleanExtra("isBox",false);
        String boxSize = data.getStringExtra("boxSize");
        String specialInstructions = data.getStringExtra("specialInstructions");
        // TODO do something
        item.setDescription(itemName);
        item.setWeightLbs(weightLbs);
        // for the present with no insurance the value is always .60 per pound
        item.setMonetaryValue(weightLbs* 0.60f);
        item.setVolume(cubicFeet);

        item.setIsBox(isBox);
        // box size is
        itemRef.setValue(item);
      }
    }
  }

  private void handleTakePictureResult(){
    BitmapFactory.Options options = new BitmapFactory.Options();
    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.toString());
    bitmap = Utility.scaleBitmapIfNecessary(bitmap);
    try {
      ExifInterface ei = new ExifInterface(imageFile.toString());
      int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

      switch(orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          bitmap = Utility.rotateImage(bitmap, 90);
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          bitmap = Utility.rotateImage(bitmap, 180);
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          bitmap = Utility.rotateImage(bitmap, 270);
          break;
      }
      // etc.
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (bitmap != null) {
      //
      writeBitmapBackToFile(bitmap);
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