/*
package com.ranchosoftware.speedymovinginventory;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ranchosoftware.speedymovinginventory.app.MyVolley;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.utility.Utility;

import java.util.Map;


 * Created by rob on 8/11/16

public class JobRecyclerAdapter extends FirebaseRecyclerAdapter<Item, JobRecyclerAdapter.MovingItemViewHolder> {

  private final static  String TAG = JobRecyclerAdapter.class.getSimpleName();

  private BaseActivity context;
  private boolean allowDelete;
  private String companyKey;
  private String jobKey;
  private ImageLoader imageLoader;
  private Class activityToLaunchOnItemTouch;
  private String sortBy;

  public JobRecyclerAdapter( BaseActivity context, Boolean allowDelete, String companyKey,
                             String jobKey, Class activityToLaunch,   JobActivity.SortBy ref){
    super(Item.class, R.layout.moving_item, MovingItemViewHolder.class, ref.query);
    this.context = context;
    this.allowDelete = allowDelete;
    this.companyKey = companyKey;
    this.jobKey = jobKey;
    imageLoader = MyVolley.getImageLoader();
    this.activityToLaunchOnItemTouch = activityToLaunch;
    this.sortBy = ref.sortBy;
  }

  public void remove(int position){

    Item item = getItem(position);
    if (allowDelete){
      // its ok to delete
      String itemKey = getRef(position).getKey();
      removeItem(companyKey, jobKey, itemKey, item.getImageReferences());
      notifyItemRemoved(position);
    } else {
      // tell the user we can't delete

      Utility.error(context.getRootView(), context, "Can't delete items after items have been loaded and signed off.");
      notifyDataSetChanged();
    }

  }

  private void removeItem(String companyKey, String jobKey, String itemKey, Map<String, String> imageReferences){
    // have to remove the item in itemlist,
    FirebaseDatabase.getInstance().getReference("itemlists/" + jobKey + "/items/" + itemKey).removeValue();

    // have to remove the qrc code in qrcList
    FirebaseDatabase.getInstance().getReference("qrcList/" + itemKey ).removeValue();

    // have to remove the images ate storage/images/companyKey/Jobkey/qrccode
    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    StorageReference storageRef  = firebaseStorage.getReferenceFromUrl(context.app().getStorageUrl());
    for (String key : imageReferences.keySet()){
      storageRef.child("images/"+ companyKey + "/" + jobKey + "/" + itemKey + "/" + key).delete()
              .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                  Log.d(TAG, "Delete succeeded");
                }
              }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          Log.d(TAG, "delete failed");
        }
      });

    }

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

  private void highlight(TextView label){

     int color = this.context.getResources().getColor(R.color.speedyMed);
    label.setTextColor(color);
  }

  private void highlightSortByLabel(MovingItemViewHolder holder){
    if (sortBy.equals("By Value")) {
      highlight(holder.valueLabel);
    } else if (sortBy.equals("By Volume")){
      highlight(holder.volumeLabel);
    }else if (sortBy.equals("By Category")){
      highlight(holder.categoryLabel);
    }else if (sortBy.equals("By Weight")){
      highlight(holder.weightLabel);
    }

  }

  @Override
  protected void populateViewHolder(MovingItemViewHolder holder, Item model, final int position) {

    //itemRecyclerView.setVisibility(View.VISIBLE);
   // noItemsMessage.setVisibility(View.INVISIBLE);

    Item item = getItem(position);
    int numberOfImages = item.getImageReferences().keySet().size();

    if (numberOfImages > 0) {
      String key = item.getImageReferences().keySet().iterator().next();
      String urlString = item.getImageReferences().get(key);
      imageLoader.get(urlString, ImageLoader.getImageListener(holder.itemImage,
              R.drawable.noimage, R.drawable.load_failed));


    } else if (item.getIsBox()) {
      holder.itemImage.setImageDrawable(context.getResources().getDrawable(R.drawable.closedbox));
    } else {
      holder.itemImage.setImageDrawable(context.getResources().getDrawable(R.drawable.noimage));
    }
    if (numberOfImages > 1) {
      holder.moreImages.setVisibility(View.VISIBLE);
      holder.moreImages.setText(Integer.toString(numberOfImages - 1) + " more");
    } else {
      holder.moreImages.setVisibility(View.INVISIBLE);
    }

    setClaimInfo(model, holder);

    holder.description.setText(item.getDescription());
    holder.monetaryValue.setText("$" + String.format("%.2f",item.getMonetaryValue()));
    holder.insurance.setText(item.getInsurance());
    holder.numberOfPads.setText(Integer.toString(item.getNumberOfPads()));
    holder.category.setText(item.getCategory());
    holder.packedBy.setText(item.getPackedBy());

    if (item.getIsScanned()) {
      holder.scannedCheck.setVisibility(View.VISIBLE);
    } else {
      holder.scannedCheck.setVisibility(View.INVISIBLE);
    }

    String styled = String.format("%.1f", (float) item.getVolume()) + " ft3";

    SpannableStringBuilder superScript = new SpannableStringBuilder(styled);
    superScript.setSpan(new SuperscriptSpan(), styled.length() - 1, styled.length(), 0);
    superScript.setSpan(new RelativeSizeSpan(0.5f), styled.length() - 1, styled.length(), 0);

    holder.volume.setText(superScript);
    holder.weight.setText(String.format("%.0f",item.getWeightLbs()) + " lbs");
    holder.claimNumber.setText(item.getClaimNumber());

    highlightSortByLabel(holder);

    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

          Intent intent = new Intent(context, activityToLaunchOnItemTouch);
          Bundle params = new Bundle();
          params.putString("companyKey", companyKey);
          params.putString("jobKey", jobKey);
          String itemCode = getRef(position).getKey();
          params.putString("itemCode", itemCode);
          intent.putExtras(params);
          context.startActivity(intent);

      }
    });
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

    // labels one of these will get highlighted due to sort order
    TextView valueLabel;
    TextView padsLabel;
    TextView categoryLabel;
    TextView volumeLabel;
    TextView weightLabel;

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
      valueLabel = (TextView) v.findViewById(R.id.tvValueLabel);

      valueLabel = (TextView) v.findViewById(R.id.tvValueLabel);
      padsLabel = (TextView) v.findViewById(R.id.tvPadsLabel);
      categoryLabel = (TextView) v.findViewById(R.id.tvCategoryLabel);
      volumeLabel = (TextView) v.findViewById(R.id.tvVolumeLabel);
      weightLabel = (TextView) v.findViewById(R.id.tvWeightLabel);
    }
  }
}

*/