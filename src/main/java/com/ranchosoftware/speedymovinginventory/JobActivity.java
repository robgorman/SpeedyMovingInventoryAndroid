package com.ranchosoftware.speedymovinginventory;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.model.Address;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.User;
import com.ranchosoftware.speedymovinginventory.uiutility.SimpleDividerItemDecoration;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import org.joda.time.format.DateTimeFormatter;

import java.util.Map;
import java.util.TreeMap;

public class JobActivity extends BaseMenuActivity {
  private static final String TAG = JobActivity.class.getSimpleName();



  private RecyclerView itemRecyclerView;
  private TextView noItemsMessage;
  private LinearLayoutManager listLayoutManager;
  private TextView findTv(int tv){
    return (TextView) findViewById(tv);
  }
  private JobRecyclerAdapter  adapter;

  Query itemRefByValue;
  Query itemRefByVolume;
  Query itemRefByCategory;
  Query itemRefByScanned;
  Query itemRefByWeight;
  Query itemRefByActiveClaim;

  ImageLoader imageLoader;
  private String companyKey;
  private String jobKey;
  private Job job;
  Query jobRef;
  private TabHost tabHost;

  private String synthesizeAddress(String address, String addressLine2, String city, String state, String zip){
    String result = address;
    if (addressLine2 != null && addressLine2.length() > 0){
      result = result + "\n" + addressLine2;
    }
    result = result + "\n" + city + ", " + state + " " + zip;
    return result;
  }

  private void setClaimInfo(Item item, MovingItemViewHolder holder){
    holder.inactiveClaim.setVisibility(View.INVISIBLE);
    holder.activeClaim.setVisibility(View.INVISIBLE);
    holder.claimNumberLayout.setVisibility(View.INVISIBLE);

    if (item.getHasClaim()){
      holder.claimNumberLayout.setVisibility(View.VISIBLE);
      if (item.getIsClaimActive()){
        holder.activeClaim.setVisibility(View.VISIBLE);
      } else {
        holder.inactiveClaim.setVisibility(View.VISIBLE);
      }
    }
  }

  private void updateFromJob(){
    DateTimeFormatter formatter = app().getDateTimeFormatter();
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

    toolbar.setTitle("(" + job.getJobNumber() + ") " + job.getCustomerFirstName() + " " + job.getCustomerLastName());
    findTv(R.id.tvEmail).setText(job.getCustomerEmail());
    findTv(R.id.tvLifecycle).setText(job.getLifecycle().toString());
    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
      findTv(R.id.tvPhone).setText(PhoneNumberUtils.formatNumber(job.getCustomerPhone(), "US"));
    } else {
      findTv(R.id.tvPhone).setText(PhoneNumberUtils.formatNumber(job.getCustomerPhone()));
    }
    findTv(R.id.tvStorageInTransit).setText(job.getStorageInTransit());
    findTv(R.id.tvPickupDate).setText(formatter.print(job.getPickupDateTime()));

    Address origin = job.getOriginAddress();
    String pickupAddress = synthesizeAddress(origin.getStreet(),
            origin.getAddressLine2(),
            origin.getCity(),
            origin.getState(),
            origin.getZip());
    findTv(R.id.tvPickupAddress).setText(pickupAddress);

    Address destination = job.getDestinationAddress();
    String deliveryAddress = synthesizeAddress(destination.getStreet(),
            destination.getAddressLine2(),
            destination.getCity(),
            destination.getState(),
            destination.getZip());
    findTv(R.id.tvDeliveryAddress).setText(deliveryAddress);

    String deliveryWindow = formatter.print(job.getDeliveryEarliestDate()) + " - " + formatter.print(job.getDeliveryLatestDate());
    findTv(R.id.tvDeliveryDateWindow).setText(deliveryWindow);
  }

  ValueEventListener jobChangeListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      job = dataSnapshot.getValue(Job.class);
      updateFromJob();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  };

  private Map<String, Item> itemChildren = new TreeMap<String, Item>();
  private ChildEventListener itemsListener = new ChildEventListener() {
    int childCount = 0;
    int scannedCount = 0;
    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

      itemChildren.put(dataSnapshot.getKey(), dataSnapshot.getValue(Item.class));
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
      itemChildren.put(dataSnapshot.getKey(), dataSnapshot.getValue(Item.class));
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
      itemChildren.remove(dataSnapshot.getKey());
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
    }
  };

  private void setupQueries(){
    itemRefByValue =  FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("monetaryValue");
    itemRefByVolume =  FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("volume");
    itemRefByCategory =  FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("category");
    itemRefByScanned = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("isScanned");
    itemRefByWeight = FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("weightLbs");
    itemRefByActiveClaim =  FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items").orderByChild("isClaimActive");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_job);

    Bundle b = getIntent().getExtras();
    jobKey = b.getString("jobKey");
    companyKey = b.getString("companyKey");

    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    jobRef = database.getReference("jobs/" + jobKey );

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    imageLoader = MyVolley.getImageLoader();

    itemRecyclerView = (RecyclerView) findViewById(R.id.itemsList);

    setupQueries();
    tabHost = (TabHost) findViewById(R.id.tabHost);
    setupTabs();

    noItemsMessage = (TextView) findViewById(R.id.tvNoItemsMessage);

    //itemRecyclerView.setHasFixedSize(true);
    listLayoutManager = new LinearLayoutManager(this);
    listLayoutManager.setReverseLayout(true);
    listLayoutManager.setStackFromEnd(true);

    itemRecyclerView.setLayoutManager(listLayoutManager);
    itemRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(thisActivity));




    itemRefByValue.addChildEventListener(itemsListener);

    adapter = new JobRecyclerAdapter(itemRefByValue);
    itemRecyclerView.setAdapter(adapter);

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
    for (String key : itemChildren.keySet()){
      Item next = itemChildren.get(key);
      if (!next.getIsScanned()){
        return false;
      }
    }
    return true;
  }


  private String tabs[] =      {"^Val", "^Vol", "^Cat",   "^Scn", "^lbs", "^Clm"};
  private int tabResource[] = {R.id.tab1, R.id.tab2, R.id.tab3, R.id.tab4, R.id.tab5, R.id.tab6};
  private Query query[] = {itemRefByValue, itemRefByVolume, itemRefByCategory, itemRefByScanned, itemRefByWeight, itemRefByActiveClaim };
  private boolean sortReverse[] = {true, true, false, false, true, true};
  private void setupTabs(){
    tabHost.setup();

    for (int i = 0; i < tabs.length; i++){
      TabHost.TabSpec tabSpec = tabHost.newTabSpec(tabs[i]);
      tabSpec.setContent(tabResource[i]);
      tabSpec.setIndicator(tabs[i]);
      tabHost.addTab(tabSpec);

    }

    query[0] = itemRefByValue;
    query[1] = itemRefByVolume;
    query[2] = itemRefByCategory;
    query[3] = itemRefByScanned;
    query[4] = itemRefByWeight;
    query[5] = itemRefByActiveClaim;


    tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
      @Override
      public void onTabChanged(String tabId) {
        removeListeners();
        ((ViewGroup)itemRecyclerView.getParent()).removeView(itemRecyclerView);
        for (int i = 0; i < tabs.length; i++){
          if (tabId.equalsIgnoreCase(tabs[i])){
            Query q = query[i];
            adapter = new JobRecyclerAdapter(q);
            itemRecyclerView.setAdapter(adapter);
            query[i].addChildEventListener(itemsListener);
            listLayoutManager.setReverseLayout(sortReverse[i]);
            listLayoutManager.setStackFromEnd(sortReverse[i]);
            ViewGroup group = (ViewGroup) findViewById(tabResource[i]);
            group.addView(itemRecyclerView);
          }
        }

      }
    });

  }


  private void removeListeners(){

    itemRefByValue.removeEventListener(itemsListener);
    itemRefByVolume.removeEventListener(itemsListener);
    itemRefByCategory.removeEventListener(itemsListener);
    itemRefByScanned.removeEventListener(itemsListener);
    itemRefByWeight.removeEventListener(itemsListener);
    itemRefByActiveClaim.removeEventListener(itemsListener);
  }

  private void markAllItemsAsScanned(){
    DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("/itemlists/" + jobKey +
      "/items/");
    for (String key : itemChildren.keySet()){
      Item item = itemChildren.get(key);
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
  }

  @Override
  protected  void onStop(){

    super.onStop();
    jobRef.removeEventListener(jobChangeListener);
  }

  private class JobRecyclerAdapter extends FirebaseRecyclerAdapter<Item, MovingItemViewHolder>{
    public JobRecyclerAdapter( Query ref){
      super(Item.class, R.layout.moving_item, MovingItemViewHolder.class, ref);
    }

    @Override
    protected void populateViewHolder(MovingItemViewHolder holder, Item model, final int position) {

      itemRecyclerView.setVisibility(View.VISIBLE);
      noItemsMessage.setVisibility(View.INVISIBLE);

      Item item = getItem(position);
      int numberOfImages = item.getImageReferences().keySet().size();

      if (numberOfImages > 0) {
        String key = item.getImageReferences().keySet().iterator().next();
        String urlString = item.getImageReferences().get(key);
        imageLoader.get(urlString, ImageLoader.getImageListener(holder.itemImage,
                R.drawable.noimage, R.drawable.load_failed));


      } else if (item.getIsBox()) {
        holder.itemImage.setImageDrawable(thisActivity.getResources().getDrawable(R.drawable.closedbox));
      } else {
        holder.itemImage.setImageDrawable(thisActivity.getResources().getDrawable(R.drawable.noimage));
      }
      if (numberOfImages > 1) {
        holder.moreImages.setVisibility(View.VISIBLE);
        holder.moreImages.setText(Integer.toString(numberOfImages - 1) + " more");
      } else {
        holder.moreImages.setVisibility(View.INVISIBLE);
      }

      setClaimInfo(model, holder);

      holder.description.setText(item.getDescription());
      holder.monetaryValue.setText("$" + item.getMonetaryValue());
      holder.insurance.setText(item.getInsurance());
      holder.numberOfPads.setText(Integer.toString(item.getNumberOfPads()));
      holder.category.setText(item.getCategory());
      holder.packedBy.setText(item.getPackedBy());

      if (item.getIsScanned()) {
        holder.scannedCheck.setVisibility(View.VISIBLE);
      } else {
        holder.scannedCheck.setVisibility(View.INVISIBLE);
      }

      String styled = Integer.toString(item.getVolume()) + " ft3";

      SpannableStringBuilder superScript = new SpannableStringBuilder(styled);
      superScript.setSpan(new SuperscriptSpan(), styled.length() - 1, styled.length(), 0);
      superScript.setSpan(new RelativeSizeSpan(0.5f), styled.length() - 1, styled.length(), 0);

      holder.volume.setText(superScript);
      holder.weight.setText(item.getWeightLbs() + " lbs");
      holder.claimNumber.setText(item.getClaimNumber());

      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (job.getLifecycle() == Job.Lifecycle.New) {
            Intent intent = new Intent(thisActivity, NewItemActivity.class);
            Bundle params = new Bundle();
            params.putString("companyKey", job.getCompanyKey());
            params.putString("jobKey", jobKey);
            String itemCode = adapter.getRef(position).getKey();
            params.putString("itemCode", itemCode);
            intent.putExtras(params);
            startActivity(intent);
          } else {
            Intent intent = new Intent(thisActivity, ItemClaimActivity.class);
            Bundle params = new Bundle();
            params.putString("companyKey", job.getCompanyKey());
            params.putString("jobKey", jobKey);
            String itemCode = adapter.getRef(position).getKey();
            params.putString("itemCode", itemCode);
            intent.putExtras(params);
            startActivity(intent);
          }
        }
      });
    }
  }

  public static final int SIGNOFF_REQUEST = 1;

  private void launchSignOffActivity() {
    Intent intent = new Intent(thisActivity, SignOffActivity.class);
    Bundle params = new Bundle();
    params.putString("companyKey", job.getCompanyKey());
    params.putString("jobKey", jobKey);;
    params.putString("lifecycle", job.getLifecycle().toString());
    params.putString("storageInTransit", job.getStorageInTransit());
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
        // The user picked a contact.
        // The Intent's data Uri identifies which contact was selected.

        // Do something with the contact here (bigger example below)
      } else {
        // no signoff so no changes
      }
    }
  }
  public static class MovingItemViewHolder extends RecyclerView.ViewHolder {
    ImageView itemImage;
    ImageView activeClaim;
    ImageView inactiveClaim;
    ImageView scannedCheck;
    TextView description;
    TextView monetaryValue;
    TextView insurance;
    TextView numberOfPads;
    TextView category;
    TextView packedBy;
    TextView volume;
    TextView weight;
    View claimNumberLayout;
    TextView claimNumber;
    TextView moreImages;

    public MovingItemViewHolder(View v){
      super(v);
      itemImage = (ImageView) v.findViewById(R.id.ivItemImage);
      activeClaim = (ImageView) v.findViewById(R.id.ivActiveClaim);
      inactiveClaim = (ImageView) v.findViewById(R.id.ivInactiveClaim);
      description = (TextView) v.findViewById(R.id.tvDescription);
      monetaryValue = (TextView) v.findViewById(R.id.tvMonetaryValue);
      insurance = (TextView) v.findViewById(R.id.tvInsurance);
      numberOfPads = (TextView) v.findViewById(R.id.tvNumberOfPads);
      category = (TextView) v.findViewById(R.id.tvCategory);
      packedBy = (TextView) v.findViewById(R.id.tvPackedBy);
      volume = (TextView) v.findViewById(R.id.tvVolume);
      weight = (TextView) v.findViewById(R.id.tvWeight);
      claimNumberLayout = v.findViewById(R.id.claimNumberLayout);
      claimNumber = (TextView) v.findViewById(R.id.tvClaimNumber);
      moreImages = (TextView) v.findViewById(R.id.tvMoreImages);
      scannedCheck = (ImageView) v.findViewById(R.id.ivScannedCheck);
    }
  }
}
