
package com.ranchosoftware.speedymovinginventory.database;


import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ranchosoftware.speedymovinginventory.model.Company;
import com.ranchosoftware.speedymovinginventory.model.Item;
import com.ranchosoftware.speedymovinginventory.model.Job;
import com.ranchosoftware.speedymovinginventory.model.Model;
import com.ranchosoftware.speedymovinginventory.model.User;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;



public class DatabaseObject<T>{

  private static final String TAG = DatabaseObject.class.getSimpleName();

  private List<DatabaseObjectEventListener<T>> valueListeners = new ArrayList<>();

  private DatabaseReference modelReference;
  Class c;

  public DatabaseObject(Class c, String key1, String key2){

    this.c = c;

    String object = "companies/";
    if (c == Job.class){
      object = "joblists/" + key1 + "/jobs/"+ key2;
    }

    if (c == Item.class){
      object = "itemlists/" + key1 + "/items/" + key2;
    }
    modelReference = FirebaseDatabase.getInstance().getReference(object);
    modelReference.addValueEventListener(valueEventListener);
  }
  public DatabaseObject(Class c, String key){

    this.c = c;
    String object = "companies/";
    if (c == Company.class){
      object = "companies/";
    } else if (c == Job.class){
      object = "jobs/";
    } else if (c == User.class){
      object = "users/";
    } else if (c == String.class){
      object = "qrcList/";
    }
    modelReference = FirebaseDatabase.getInstance().getReference(object + key);
    //modelReference.addValueEventListener(valueEventListener);
  }

  private ValueEventListener valueEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

      for (DatabaseObjectEventListener<T> listener : valueListeners){
        try {
          Object o = dataSnapshot.getValue(c);
          listener.onChange(dataSnapshot.getKey(), (T) o);
        } catch (Exception e){
          Log.d(TAG, "Error reading object of class " + c.getSimpleName() + " from database. Error="+ e.getMessage());
        }
      }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
  };

  public boolean addValueEventListener(DatabaseObjectEventListener<T> listener){


    if (!valueListeners.contains(listener)){
      modelReference.addValueEventListener(valueEventListener);
      valueListeners.add(listener);
      return true;
    } else {
      return false;
    }

  }

  public boolean removeValueEventListener(DatabaseObjectEventListener<T> listener){
    return valueListeners.remove(listener);
  }

  public void setValue(T t){
    modelReference.setValue(t);
  }

  public DatabaseReference child(String fieldName){
    return modelReference.child(fieldName);
  }


}
