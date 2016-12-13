package com.ranchosoftware.speedymovinginventory;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObject;
import com.ranchosoftware.speedymovinginventory.database.DatabaseObjectEventListener;
import com.ranchosoftware.speedymovinginventory.model.Address;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.server.Server;
import com.ranchosoftware.speedymovinginventory.utility.TextUtility;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JobActivity extends BaseMenuActivity {
  private static final String TAG = JobActivity.class.getSimpleName();

  private RecyclerView itemRecyclerView;
  private TextView noItemsMessage;

  private LinearLayoutManager gridLayoutManager;
  private TextView findTv(int tv){
    return (TextView) findViewById(tv);
  }
  private JobRecyclerGridAdapter  adapter;

  private View sortByGroup;
  private Button sortByButton;
  private TextView tvTotalItems;
  private TextView tvScannedItems;
  private TextView tvTotalValue;
  private TextView tvTotalPads;
  private TextView tvTotalVolume;
  private TextView tvTotalWeight;
  private TextView tvTotalDamagedItems;
  private TabHost tabHost;

  private TextView newLabelIndicator;
  private TextView loadedForStorageIndicator;
  private TextView inStorageIndicator;
  private TextView loadedForDeliveryIndicator;
  private TextView deliveredIndicator;

  private ImageView ivStatus[] = new ImageView[Job.Lifecycle.values().length];
  private Drawable ivStatusInactive[] = new Drawable[Job.Lifecycle.values().length];
  private Drawable ivStatusActive[] = new Drawable[Job.Lifecycle.values().length];

  String[] sortByLabels ={"Value", "Volume", "Category", "Scanned", "Weight", "Claim"};

  private int sortByPosition = 0;
  private int tabIndex = 0;

  public static class SortBy{
    public Query query;
    public String sortBy;
    public SortBy(Query query, String sortBy){
      this.query = query;
      this.sortBy = sortBy;
    }

  }
  List<SortBy> queries = new ArrayList<SortBy>();

  private String companyKey;
  private String jobKey;
  private Job job;
  DatabaseObject<Job> jobRef;

  private Query recipientListQuery;
  Job.Lifecycle initialJobLifecycle = null;

  // recipient list for signoff; this is maintained while this activity is up
  private String recipientList = "";

  //private Configuration mailgunConfiguration;

  private void setupInactiveImages(){
    ivStatusInactive[0] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.new_, null);
    ivStatusInactive[1] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.loaded_for_storage, null);
    ivStatusInactive[2] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.in_storage, null);
    ivStatusInactive[3] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.loaded_for_delivery, null);
    ivStatusInactive[4] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.delivered, null);

  }

  private void setupActiveImages(){
    ivStatusActive[0] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.new_active, null);
    ivStatusActive[1] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.loaded_for_storage_active, null);
    ivStatusActive[2] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.in_storage_active, null);
    ivStatusActive[3] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.loaded_for_delivery_active, null);
    ivStatusActive[4] = ResourcesCompat.getDrawable(thisActivity.getResources(),R.drawable.delivered_active, null);

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_job);

    Bundle b = getIntent().getExtras();
    jobKey = b.getString("jobKey");
    companyKey = b.getString("companyKey");

    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    jobRef = new DatabaseObject<>(Job.class, companyKey, jobKey);
    recipientListQuery = FirebaseDatabase.getInstance().getReference("users/").orderByChild("companyKey").startAt(companyKey).endAt(companyKey);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    setupQueries();

    ivStatus[0] = (ImageView) findViewById(R.id.ivNew);
    ivStatus[1] = (ImageView) findViewById(R.id.ivLoadedForStorage);
    ivStatus[2] = (ImageView) findViewById(R.id.ivInStorage);
    ivStatus[3] = (ImageView) findViewById(R.id.ivLoadedForDelivery);
    ivStatus[4] = (ImageView) findViewById(R.id.ivDelivered);
    setupActiveImages();
    setupInactiveImages();

    newLabelIndicator = (TextView) findViewById(R.id.tvNew);
    loadedForStorageIndicator = (TextView) findViewById(R.id.tvLoadedForStorage);
    inStorageIndicator = (TextView) findViewById(R.id.tvInStorage);
    loadedForDeliveryIndicator = (TextView) findViewById(R.id.tvLoadedForDelivery);
    deliveredIndicator = (TextView) findViewById(R.id.tvDelivered);

    FirebaseAuth auth = FirebaseAuth.getInstance();
    auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (firebaseAuth.getCurrentUser() == null){
          finish();
        }
      }
    });

    itemRecyclerView = (RecyclerView) findViewById(R.id.itemsList);
    setupRecyclerView();

    noItemsMessage = (TextView) findViewById(R.id.tvNoItemsMessage);


    // create the item and subscribe
    FloatingActionButton scan = (FloatingActionButton) findViewById(R.id.floatingActionScan);
    scan.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (job.getLifecycle() == Job.Lifecycle.Delivered){
          Utility.error(getRootView(), thisActivity, R.string.job_is_complete_no_scanning);
        } else {
          launchScanActivity();
        }
      }
    });

    // create the item and subscribe
    FloatingActionButton signOff = (FloatingActionButton) findViewById(R.id.floatingActionSignOff);
    signOff.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        if (adapter.getItemCount() == 0){
          Utility.error(getRootView(), thisActivity, "The job must contain itmes for signoff.");
          return;
        }

        if (job.getLifecycle() == Job.Lifecycle.Delivered){
          Utility.error(getRootView(), thisActivity, R.string.job_is_complete_no_signoff);
          return;
        }
        if (app().getCurrentUser().getRole().equals(User.Role.AgentCrewMember.toString())
                || app().getCurrentUser().getRole().equals(User.Role.CrewMember.toString())
                || app().getCurrentUser().getRole().equals(User.Role.Customer.toString())){
          Utility.error(findViewById(android.R.id.content), thisActivity, R.string.must_be_foreman_to_signoff);
          return;
        }
        // we can sign off if the job lifecycle is new no matter what
        if (job.getLifecycle() == Job.Lifecycle.New){
          launchSignOffActivity();
        } else {
          if (allItemsMarkedAsScanned()){
            launchSignOffActivity();
          } else {
            Log.d(TAG, "All items not scanned");
            Utility.error(findViewById(android.R.id.content), thisActivity, R.string.all_items_not_scanned);
          }
        }
      }
    });


    User.Role userRole = app().getCurrentUser().getRoleAsEnum();
    if (userRole == User.Role.ServiceAdmin || userRole == User.Role.CompanyAdmin
            || userRole == User.Role.Foreman || userRole == User.Role.AgentForeman){
      signOff.setEnabled(true);
    } else {
      signOff.setEnabled(false);
    }

    sortByGroup = findViewById(R.id.llSortByGroup);
    sortByButton = (Button) findViewById(R.id.sortByButton);
    sortByButton.setText(sortByLabels[sortByPosition] + "  >");
    sortByButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        launchSortBySpinner();
      }
    });
    sortByGroup.setVisibility(View.INVISIBLE);
    setupTabs();
    setupSummaryItems();
  }

  private static final int  PICK_SORT_BY = 565;

  private void launchSortBySpinner(){

    Intent intent = new Intent(this, SpinnerActivity.class);
    Bundle b = new Bundle();
    b.putInt(SpinnerActivity.paramSelectedIndex,  sortByPosition);
    b.putStringArray(SpinnerActivity.paramLabels, sortByLabels);
    b.putString(SpinnerActivity.paramTitle, "Choose A Sort");
    intent.putExtras(b);

    startActivityForResult(intent, PICK_SORT_BY);
    overridePendingTransition(R.xml.slide_in_from_right,R.xml.slide_out_to_left);
  }

  private void launchPrintActivity(){

    Bundle params = new Bundle();
    params.putString("companyKey", job.getCompanyKey());
    params.putString("jobKey", jobKey);
    Intent intent = new Intent(thisActivity, PrintActivity.class);
    intent.putExtras(params);
    startActivity(intent);
  }

/*
  private void printSignOff(){
    final PrintHelper photoPrinter = new PrintHelper((thisActivity));
    photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);

    ImageLoader loader = MyVolley.getImageLoader();
    // just print the most recent sign off sheet
    String url = "";
    Job.Lifecycle lifecycle = initialJobLifecycle;
    if (lifecycle == Job.Lifecycle.Delivered){
      // this case cant occur
      if (job.getSignatureDelivered() != null) {
        url = job.getSignatureDelivered().getImageUrl();
      } else {
        Utility.error(getRootView(), this, "Error: Missing delivery signoff, it can't be printed.");
        return;
      }
      loader.get(job.getSignatureDelivered().getImageUrl(), new ImageLoader.ImageListener(){
        @Override
        public void onErrorResponse(VolleyError error) {
          Utility.error(getRootView(), JobActivity.this, "Failed to load Signoff Image: " + error.getLocalizedMessage());
        }

        @Override
        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
          photoPrinter.printBitmap("speedyPrinting", response.getBitmap());
        }
      });

    } else {
      if (job.getSignatureLoadedForStorage() != null) {
        url = job.getSignatureLoadedForStorage().getImageUrl();
      } else {
        Utility.error(getRootView(), this, "Error: Missing new job signoff, it can't be printed.");
        return;
      }
      loader.get(job.getSignatureLoadedForStorage().getImageUrl(), new ImageLoader.ImageListener(){
        @Override
        public void onErrorResponse(VolleyError error) {
          Utility.error(getRootView(), JobActivity.this, "Failed to load Signoff Image: " + error.getLocalizedMessage());
        }

        @Override
        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
          photoPrinter.printBitmap("speedyPrinting", response.getBitmap());
        }
      });
    }


  }
  */

  private void setupSummaryItems(){
    tvTotalItems = (TextView) findViewById(R.id.tvTotalItems);
    tvScannedItems = (TextView) findViewById(R.id.tvScanned);
    tvTotalValue = (TextView) findViewById(R.id.tvTotalValue);
    tvTotalPads = (TextView) findViewById(R.id.tvTotalPads);
    tvTotalVolume = (TextView) findViewById(R.id.tvTotalVolume);
    tvTotalWeight = (TextView) findViewById(R.id.tvTotalWeight);
    tvTotalDamagedItems = (TextView) findViewById(R.id.tvTotalNumberDamagedItems);
  }

  private void setupTabs(){
    tabHost = (TabHost) findViewById(R.id.summaryDetailsTabHost);
    tabHost.setup();

    TabHost.TabSpec spec = tabHost.newTabSpec("Summary");
    spec.setContent(R.id.tab1);
    spec.setIndicator("Summary");

    tabHost.addTab(spec);

    spec = tabHost.newTabSpec("Details");
    spec.setContent(R.id.tab2);
    spec.setIndicator("Details");
    tabHost.addTab(spec);

    // change tab colors wo white and make selected tab a blue coolor
    tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
      @Override
      public void onTabChanged(String s) {
        setTabTitleColors(tabHost);

      }
    });
    setTabTitleColors(tabHost);
    tabHost.setCurrentTab(tabIndex);

  }

  private void setTabTitleColors(TabHost tabHost){
    for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
      //tabHost.getTabWidget().getChildAt(i)
      //       .setBackgroundColor(Color.WHITE); // unselected
      TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title); //Unselected Tabs
      tv.setTextColor(thisActivity.getResources().getColor(R.color.themeBlueDark));
    }
    //tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab())
    //        .setBackgroundColor(thisActivity.getResources().getColor(R.color.themeBase)); // selected
    TextView tv = (TextView) tabHost.getCurrentTabView().findViewById(android.R.id.title); //for Selected Tab
    tv.setTextColor(Color.WHITE);
  }


  private Map<String, Item> children = new TreeMap<String, Item>();

  private int totalItems;
  private int totalValue;
  private int totalPads;
  private float totalVolumeCubicFeet;
  private float totalWeightLbs;
  private int totalDamagedItems;
  private int totalScannedItems;

  private void updateTotals(){
    totalItems = 0;
    totalValue = 0;
    totalPads = 0;
    totalVolumeCubicFeet = 0;
    totalWeightLbs = 0;
    totalDamagedItems = 0;
    totalScannedItems = 0;

    totalItems = children.size();
    for (String key : children.keySet()){
      Item next = children.get(key);
      totalValue += next.getMonetaryValue();
      totalPads += next.getNumberOfPads();
      totalVolumeCubicFeet += next.getVolume();
      totalWeightLbs += next.getWeightLbs();
      if (next.getHasClaim()){
        totalDamagedItems++;
      }
      if (next.getIsScanned()){
        totalScannedItems++;
      }
    }

    tvTotalItems.setText(Integer.toString(totalItems));
    tvScannedItems.setText(Integer.toString(totalScannedItems));

    if (job.getLifecycle() == Job.Lifecycle.New){
      // only show # items, scannded doesn't matter
      findViewById(R.id.tvScannedLabel).setVisibility(View.GONE);
      findViewById(R.id.tvOfLabel).setVisibility(View.GONE);
      tvScannedItems.setVisibility(View.GONE);
    } else {
      // only show # items, scannded doesn't matter
      findViewById(R.id.tvScannedLabel).setVisibility(View.VISIBLE);
      findViewById(R.id.tvOfLabel).setVisibility(View.VISIBLE);
      tvScannedItems.setVisibility(View.VISIBLE);
    }

    tvTotalValue.setText("$" + Integer.toString(totalValue));
    tvTotalPads.setText(Integer.toString(totalPads));
    String styled = String.format("%.1f", (float) totalVolumeCubicFeet) + " ft3";

    SpannableStringBuilder superScript = new SpannableStringBuilder(styled);
    superScript.setSpan(new SuperscriptSpan(), styled.length() - 1, styled.length(), 0);
    superScript.setSpan(new RelativeSizeSpan(0.5f), styled.length() - 1, styled.length(), 0);
    tvTotalVolume.setText(superScript);
    tvTotalWeight.setText(String.format("%.0f", totalWeightLbs) + " lbs");
    tvTotalDamagedItems.setText(Integer.toString(totalDamagedItems));
  }

  private ChildEventListener childEventListener = new ChildEventListener() {

    private void readItem(DataSnapshot dataSnapshot){
      try {
        Item child = dataSnapshot.getValue(Item.class);
        String key = dataSnapshot.getKey();
        children.put(key, child);
        updateTotals();
      } catch (Exception e){
        Utility.error(getRootView(), thisActivity, "Error reading item, contact support. ErrorCode=" + "JobActivity:"+ e.getMessage());
      }
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
      Log.d(TAG, "onChildAdded");
      readItem(dataSnapshot);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
      readItem(dataSnapshot);
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
      Log.d(TAG, "onChildRemoved");
      try {
        children.remove(dataSnapshot.getKey());
      } catch (Exception e){
        Log.d(TAG,"Tried to remove non-existent item. Probably due to previous exception reading item");
      }

      updateTotals();
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
      Log.d(TAG, "onChildMoved");
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
      Log.d(TAG, "onCancelled");
    }
  };


  public void removeAllChildEventListenersAndClearData(){
    for (SortBy sortBy : queries){
      sortBy.query.removeEventListener(childEventListener);
    }
    children.clear();
  }

  private void onChangeSortBy(int position) {

    removeAllChildEventListenersAndClearData();
    SortBy sortBy = queries.get(position);
    sortBy.query.addChildEventListener(childEventListener);

    boolean allowDelete = (job.getLifecycle() == Job.Lifecycle.New);

    if (adapter != null) {
      adapter.unregisterAdapterDataObserver(adapterDataObserver);
      adapterDataObserver.reset();
      adapter.registerAdapterDataObserver(adapterDataObserver);
    }
    adapter = new JobRecyclerGridAdapter(thisActivity, allowDelete, companyKey, jobKey,
            allowDelete ? NewItemActivity.class : ItemDetailsActivity.class, sortBy);
    itemRecyclerView.setAdapter(adapter);
    adapterDataObserver.reset();
    adapter.registerAdapterDataObserver(adapterDataObserver);
  }

  private void updateStatusFromJob(){
    if (job.getStorageInTransit()){
      ivStatus[1].setVisibility(View.VISIBLE);
      ivStatus[2].setVisibility(View.VISIBLE);

    } else {
      ivStatus[1].setVisibility(View.GONE);
      ivStatus[2].setVisibility(View.GONE);
    }



    ivStatus[0].setImageDrawable( (job.getLifecycle() == Job.Lifecycle.New ? ivStatusActive[0] : ivStatusInactive[0]));
    ivStatus[1].setImageDrawable( (job.getLifecycle() == Job.Lifecycle.LoadedForStorage ? ivStatusActive[1] : ivStatusInactive[1]));
    ivStatus[2].setImageDrawable( (job.getLifecycle() == Job.Lifecycle.InStorage ? ivStatusActive[2] : ivStatusInactive[2]));
    ivStatus[3].setImageDrawable( (job.getLifecycle() == Job.Lifecycle.LoadedForDelivery ? ivStatusActive[3] : ivStatusInactive[3]));
    ivStatus[4].setImageDrawable( (job.getLifecycle() == Job.Lifecycle.Delivered ? ivStatusActive[4] : ivStatusInactive[4]));

    newLabelIndicator.setVisibility(View.INVISIBLE);
    loadedForStorageIndicator.setVisibility(View.INVISIBLE);
    inStorageIndicator.setVisibility(View.INVISIBLE);
    loadedForDeliveryIndicator.setVisibility(View.INVISIBLE);
    deliveredIndicator.setVisibility(View.INVISIBLE);

    switch (job.getLifecycle()){
      case New:
        newLabelIndicator.setVisibility(View.VISIBLE);
        break;
      case LoadedForStorage:
        loadedForStorageIndicator.setVisibility(View.VISIBLE);
        break;
      case InStorage:
        inStorageIndicator.setVisibility(View.VISIBLE);
        break;
      case LoadedForDelivery:
        loadedForDeliveryIndicator.setVisibility(View.VISIBLE);
        break;
      case Delivered:
        deliveredIndicator.setVisibility(View.VISIBLE);
        break;
    }

  }

  private void updateFromJob(){

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

    toolbar.setTitle("(" + job.getJobNumber() + ") " + job.getCustomerFirstName() + " " + job.getCustomerLastName());
    findTv(R.id.tvEmail).setText(job.getCustomerEmail());

    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
      findTv(R.id.tvPhone).setText(PhoneNumberUtils.formatNumber(job.getCustomerPhone(), "US"));
    } else {
      findTv(R.id.tvPhone).setText(PhoneNumberUtils.formatNumber(job.getCustomerPhone()));
    }
    updateStatusFromJob();

    DateTimeFormatter dateTimeFormatter = app().getDateTimeFormatter();
    findTv(R.id.tvPickupDate).setText(dateTimeFormatter.print(job.getPickupDateTime()));

    Address origin = job.getOriginAddress();
    String pickupAddress = TextUtility.formMultiLineAddress(origin.getStreet(),
            origin.getAddressLine2(),
            origin.getCity(),
            origin.getState(),
            origin.getZip());
    findTv(R.id.tvPickupAddress).setText(pickupAddress);

    Address destination = job.getDestinationAddress();
    String deliveryAddress = TextUtility.formMultiLineAddress(destination.getStreet(),
            destination.getAddressLine2(),
            destination.getCity(),
            destination.getState(),
            destination.getZip());
    findTv(R.id.tvDeliveryAddress).setText(deliveryAddress);

    findTv(R.id.tvPickupInstructions).setText(job.getPickupInstructions());
    findTv(R.id.tvDeliveryInstructions).setText(job.getDeliveryInstructions());
    DateTimeFormatter dateFormatter = app().getDateFormatter();
    String deliveryWindow = dateFormatter.print(job.getDeliveryEarliestDate()) + " - " + dateFormatter.print(job.getDeliveryLatestDate());
    findTv(R.id.tvDeliveryDateWindow).setText(deliveryWindow);

    boolean allowDelete = (job.getLifecycle() == Job.Lifecycle.New);
    // we know the adapter is null
    if (adapter != null){
      adapter.unregisterAdapterDataObserver(adapterDataObserver);
    }
    removeAllChildEventListenersAndClearData();
    queries.get(sortByPosition).query.addChildEventListener(childEventListener);
    adapter = new JobRecyclerGridAdapter(thisActivity, allowDelete, companyKey, jobKey,
            allowDelete ? NewItemActivity.class : ItemDetailsActivity.class, queries.get(sortByPosition));

    adapterDataObserver.reset();
    adapter.registerAdapterDataObserver(adapterDataObserver);
    itemRecyclerView.setAdapter(adapter);

    // we want to record the inital job lifecycle
    if (initialJobLifecycle == null){
      initialJobLifecycle = job.getLifecycle();
    }

    onChangeSortBy(sortByPosition);

  }

  DatabaseObjectEventListener<Job> jobChangeListener = new DatabaseObjectEventListener<Job> () {

    @Override
    public void onChange(String key, Job modelJob) {
      job  = modelJob;
      updateFromJob();
    }

  };

 // private boolean sortReverse[] = {true, true, false, false, true, true};

  private void setupQueries(){

    queries.add(new SortBy(
            FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("monetaryValueInverse"),
            "By Value"));
    queries.add(new SortBy(
            FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("volumeInverse"),
            "By Volume"));
    queries.add(new SortBy(
            FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("category"),
            "By Category"));
    queries.add( new SortBy(
            FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("isScanned"),
            "By Scanned"));
    queries.add(new SortBy(
            FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("weightLbsInverse"),
            "By Weight"));
    queries.add( new SortBy(
            FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("isClaimActiveInverse"),
            "By Claim"));
  }

  private MyAdapterDataObserver adapterDataObserver = new MyAdapterDataObserver();

  private class MyAdapterDataObserver extends RecyclerView.AdapterDataObserver {
    private float totalVolume;

    private void manageVisibility(){
      if (adapter.getItemCount() > 0){
        itemRecyclerView.setVisibility(View.VISIBLE);
        noItemsMessage.setVisibility(View.INVISIBLE);
        sortByGroup.setVisibility(View.VISIBLE);
      } else {
        itemRecyclerView.setVisibility(View.INVISIBLE);
        noItemsMessage.setVisibility(View.VISIBLE);
        sortByGroup.setVisibility(View.INVISIBLE);
      }
    }

    public void reset(){
      totalVolume = 0.0f;
    }

    @Override
    public void onChanged() {
      super.onChanged();
      manageVisibility();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      super.onItemRangeInserted(positionStart, itemCount);
      manageVisibility();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
      super.onItemRangeRemoved(positionStart, itemCount);
      manageVisibility();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
      super.onItemRangeChanged(positionStart, itemCount, payload);
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
      super.onItemRangeMoved(fromPosition, toPosition, itemCount);
    }
  };

  private float pixelsPerDp() {
    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    float logicalDensity = metrics.density;
    return logicalDensity;
  }

  private void setupRecyclerView(){
    //itemRecyclerView.setHasFixedSize(true);
    int gridItemWidth = (int) Math.ceil(pixelsPerDp() * 130);


    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);

    int numberOfColumns = size.x / gridItemWidth;
    gridLayoutManager = new GridLayoutManager(this, numberOfColumns);

    itemRecyclerView.setLayoutManager(gridLayoutManager);
    //itemRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(thisActivity));

    setUpItemTouchHelper();
    setUpAnimationDecoratorHelper();
  }

  /**
   * This is the standard support library way of implementing "swipe to delete" feature. You can do custom drawing in onChildDraw method
   * but whatever you draw will disappear once the swipe is over, and while the items are animating to their new position the recycler view
   * background will be visible. That is rarely an desired effect.
   */
  private void setUpItemTouchHelper() {

    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

      // we want to cache these and not allocate anything repeatedly in the onChildDraw method
      Drawable background;
      Drawable xMark;
      int xMarkMargin;
      boolean initiated;

      private void init() {
        background = new ColorDrawable(Color.RED);
        xMark = ContextCompat.getDrawable(thisActivity, R.drawable.ic_clear_24dp);
        xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = (int) thisActivity.getResources().getDimension(R.dimen.ic_clear_margin);
        initiated = true;
      }

      // not important, we don't want drag & drop
      @Override
      public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
      }

      @Override
      public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return super.getSwipeDirs(recyclerView, viewHolder);
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        int swipedPosition = viewHolder.getAdapterPosition();

        adapter.remove(swipedPosition);


      }

      @Override
      public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        // not sure why, but this method get's called for viewholder that are already swiped away
        if (viewHolder.getAdapterPosition() == -1) {
          // not interested in those
          return;
        }

        if (!initiated) {
          init();
        }

        // draw red background
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

        // draw x mark
        int itemHeight = itemView.getBottom() - itemView.getTop();
        int intrinsicWidth = xMark.getIntrinsicWidth();
        int intrinsicHeight = xMark.getIntrinsicWidth();

        int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - xMarkMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
        int xMarkBottom = xMarkTop + intrinsicHeight;
        xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

        xMark.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
      }

    };
    ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
    mItemTouchHelper.attachToRecyclerView(itemRecyclerView);
  }

  /**
   * We're gonna setup another ItemDecorator that will draw the red background in the empty space while the items are animating to thier new positions
   * after an item is removed.
   */
  private void setUpAnimationDecoratorHelper() {
    itemRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {

      // we want to cache this and not allocate anything repeatedly in the onDraw method
      Drawable background;
      boolean initiated;

      private void init() {
        background = new ColorDrawable(Color.RED);
        initiated = true;
      }

      @Override
      public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

        if (!initiated) {
          init();
        }

        // only if animation is in progress
        if (parent.getItemAnimator().isRunning()) {

          // some items might be animating down and some items might be animating up to close the gap left by the removed item
          // this is not exclusive, both movement can be happening at the same time
          // to reproduce this leave just enough items so the first one and the last one would be just a little off screen
          // then remove one from the middle

          // find first child with translationY > 0
          // and last one with translationY < 0
          // we're after a rect that is not covered in recycler-view views at this point in time
          View lastViewComingDown = null;
          View firstViewComingUp = null;

          // this is fixed
          int left = 0;
          int right = parent.getWidth();

          // this we need to find out
          int top = 0;
          int bottom = 0;

          // find relevant translating views
          int childCount = parent.getLayoutManager().getChildCount();
          for (int i = 0; i < childCount; i++) {
            View child = parent.getLayoutManager().getChildAt(i);
            if (child.getTranslationY() < 0) {
              // view is coming down
              lastViewComingDown = child;
            } else if (child.getTranslationY() > 0) {
              // view is coming up
              if (firstViewComingUp == null) {
                firstViewComingUp = child;
              }
            }
          }

          if (lastViewComingDown != null && firstViewComingUp != null) {
            // views are coming down AND going up to fill the void
            top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
            bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
          } else if (lastViewComingDown != null) {
            // views are going down to fill the void
            top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
            bottom = lastViewComingDown.getBottom();
          } else if (firstViewComingUp != null) {
            // views are coming up to fill the void
            top = firstViewComingUp.getTop();
            bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
          }
          background.setBounds(left, top, right, bottom);
          background.draw(c);
        }
        super.onDraw(c, parent, state);
      }
    });
  }

  private void launchScanActivity() {
    Intent intent = new Intent(thisActivity, ScanActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", job.getCompanyKey());
    params.putString("jobKey", jobKey);

    intent.putExtras(params);
    startActivity(intent);
  }

  private boolean allItemsMarkedAsScanned(){

    for (int i = 0; i < adapter.getItemCount(); i++){
      Item next = adapter.getItem(i);
      if (!next.getIsScanned()){
        return false;
      }
    }
    return true;
  }



  private void markAllItemsAsScanned(){
    DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey +
      "/items/");
    for (int i = 0; i < adapter.getItemCount(); i++){
      Item item = adapter.getItem(i);
      String key = adapter.getRef(i).getKey();
      itemsRef.child(key + "/isScanned").setValue(false);
    }
  }

  @Override
  protected void onStart(){
    super.onStart();
    if (jobRef != null){
      jobRef.addValueEventListener(jobChangeListener);
    }
    if (adapter != null){
      adapter.notifyDataSetChanged();
    }
    if (recipientListQuery != null){
      recipientListQuery.addValueEventListener(recipientListEventLister);
    }
  }

  @Override
  protected  void onStop(){
    super.onStop();
    jobRef.removeValueEventListener(jobChangeListener);
    recipientListQuery.removeEventListener(recipientListEventLister);
    //adapter.unregisterAdapterDataObserver(adapterDataObserver);
  }

  public static final int SIGNOFF_REQUEST = 1;

  private void launchSignOffActivity() {
    Intent intent = new Intent(thisActivity, SignOffActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", job.getCompanyKey());
    params.putString("jobKey", jobKey);;
    params.putString("lifecycle", job.getLifecycle().toString());
    params.putBoolean("storageInTransit", job.getStorageInTransit());
    params.putInt("totalItems", totalItems);
    params.putInt("totalValue", totalValue );
    params.putInt("totalPads",  totalPads);
    params.putFloat("totalVolumeCubicFeet",  totalVolumeCubicFeet);
    params.putFloat("totalWeightLbs",  totalWeightLbs);
    params.putInt("totalDamagedItems",  totalDamagedItems);

    intent.putExtras(params);
    startActivityForResult(intent, SIGNOFF_REQUEST);
  }



  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Check which request we're responding to
    if (requestCode == SIGNOFF_REQUEST) {
      // Make sure the request was successful
      if (resultCode == RESULT_OK) {
        // we got signoff. we need to mark all items as scanned
        markAllItemsAsScanned();
        SendEmailTask task = new SendEmailTask();
        task.execute();


        // The user picked a contact.
        // The Intent's data Uri identifies which contact was selected.
        // Do something with the contact here (bigger example below)
      } else {
        // no signoff so no changes
      }
    } else if (requestCode == PICK_SORT_BY){
        if (resultCode == Activity.RESULT_OK){
          // extract selected index
          int selectedIndex = data.getIntExtra(SpinnerActivity.paramSelectedIndex, 0);
          sortByPosition = selectedIndex;
          sortByButton.setText(sortByLabels[sortByPosition] + "  >");
          onChangeSortBy(selectedIndex);
        }
    }
  }

  private class SendEmailTask extends AsyncTask<Void, Integer, Long> {
    @Override
    protected Long doInBackground(Void... voids) {
      sendSignOffEmail();
      return 0L;
    }
  }

  //private static final String MAILGUN_API_KEY = "key-c90fa773c9d000ce3cd38a903368ee7b";
  //private static final String BASE_URL = "https://api.mailgun.net/v3/speedymovinginventory.com";

  //private static final String portalUrl = "https://speedymovinginventory.firebaseapp.com";

  private String formSignatureUrl(){
    String result = "";
    switch (job.getLifecycle()){
      case New:
        result =  job.getSignatureLoadedForStorage().getImageUrl();
        break;
      case LoadedForStorage:
        result = job.getSignatureInStorage().getImageUrl();
        break;
      case InStorage:
        result = job.getSignatureLoadedForDelivery().getImageUrl();
        break;
      case LoadedForDelivery:
        result = job.getSignatureDelivered().getImageUrl();
        break;
      case Delivered:
        //result = job.getSignatureDelivered().getImageUrl();
        assert(false)    ;
        break;
    }
    return result;
  }

  private ValueEventListener recipientListEventLister = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      if (dataSnapshot.getValue() == null){
        // there is some sort of error
        Log.d(TAG, "Serious Error");
        Utility.error(getRootView(), thisActivity, "Error: Job Activity, unexpected null.");
      } else {
        for (DataSnapshot nextSnapshot : dataSnapshot.getChildren()){

          try {
            User user = nextSnapshot.getValue(User.class);
            if (!recipientList.contains(user.getEmailAddress())) {
              if (user.getRoleAsEnum() == User.Role.CompanyAdmin) {
                if (recipientList.length() == 0) {
                  recipientList = recipientList + user.getEmailAddress();
                } else {
                  recipientList = recipientList + "," + user.getEmailAddress();
                }
              }
            }
          } catch (Exception e){
            // just skip a user that we can't read
            Log.d(TAG, "Unreadable user skipped");
          }
        }

      }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  };

  private void sendNewJobSignOffEmail(){
    String recipients = job.getCustomerEmail() + "," + recipientList;
    String companyName = app().getCurrentCompany().getName();
    String companyPhone = app().getCurrentCompany().getPhoneNumber();

    String linkUrl = getResources().getString(R.string.firebase_database_url) + "/user-sign-up";
    String customerName = job.getCustomerFirstName() + " " + job.getCustomerLastName();
    String lifecycle = job.getLifecycle().toString();
    String jobNumber = job.getJobNumber();

    Server mailServer = app().getMailServer();
    mailServer.sendNewSignoffEmailMessage(recipients, companyName, linkUrl, customerName, lifecycle, jobNumber,companyPhone,
            new Server.EmailCallback() {
              @Override
              public void success(String message) {

                Utility.error(thisActivity.getRootView(), thisActivity, "Signoff email successfully sent");
              }

              @Override
              public void failure(String message) {
                Utility.error(thisActivity.getRootView(), thisActivity, "Signoff email failed: " + message);
              }
            });
  }

  private void sendLifecycleSignOffEmail(){
    String recipients =  recipientList;
    String companyName = app().getCurrentCompany().getName();
    String companyPhone = app().getCurrentCompany().getPhoneNumber();

    String linkUrl = getResources().getString(R.string.firebase_database_url);
    String customerName = job.getCustomerFirstName() + " " + job.getCustomerLastName();
    String lifecycle = job.getLifecycle().toString();
    String jobNumber = job.getJobNumber();

    Server mailServer = app().getMailServer();
    mailServer.sendNewSignoffEmailMessage(recipients, companyName, linkUrl, customerName, lifecycle, jobNumber,companyPhone,
            new Server.EmailCallback() {
              @Override
              public void success(String message) {

                Utility.error(thisActivity.getRootView(), thisActivity, "Signoff email successfully sent");
              }

              @Override
              public void failure(String message) {
                Utility.error(thisActivity.getRootView(), thisActivity, "Signoff email failed: " + message);
              }
            });
  }

  protected void sendSignOffEmail(){

    if (job.getLifecycle() == Job.Lifecycle.New){
      // this is a new job we send notification to the customer as well as the company admins
      sendNewJobSignOffEmail();
    } else {
      sendLifecycleSignOffEmail();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("sortByPosition", sortByPosition);
    outState.putInt("tabIndex", tabIndex);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    sortByPosition = savedInstanceState.getInt("sortByPosition", 0);
    tabIndex = savedInstanceState.getInt("tabIndex", 0);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.job_activity_menu, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.print_signoff_sheet:
        launchPrintActivity();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }


}
