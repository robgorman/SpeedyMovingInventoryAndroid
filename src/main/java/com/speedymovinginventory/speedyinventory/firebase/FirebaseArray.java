package com.speedymovinginventory.speedyinventory.firebase;


import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;

import java.util.ArrayList;

/**
 * This class implements an array-like collection on top of a Firebase location.
 */
class FirebaseArray implements ChildEventListener {
  public interface OnChangedListener {
    enum EventType {ADDED, CHANGED, REMOVED, MOVED};
    void onChanged(EventType type, int index, int oldIndex);
    void onCancelled(DatabaseError databaseError);
  }

  private Query mQuery;
  private OnChangedListener mListener;
  private ArrayList<DataSnapshot> mSnapshots;
  private FirebaseListAdapter.IFilter filter;

  public FirebaseArray(Query ref, FirebaseListAdapter.IFilter filter) {
    mQuery = ref;
    mSnapshots = new ArrayList<DataSnapshot>();
    mQuery.addChildEventListener(this);
    this.filter = filter;
  }

  public void cleanup() {
    mQuery.removeEventListener(this);
  }

  public int getCount() {
    return mSnapshots.size();

  }
  public DataSnapshot getItem(int index) {
    return mSnapshots.get(index);
  }

  private int getIndexForKey(String key) {
    int index = 0;
    for (DataSnapshot snapshot : mSnapshots) {
      if (snapshot.getKey().equals(key)) {
        return index;
      } else {
        index++;
      }
    }
    return -1;
    //throw new IllegalArgumentException("Key not found");
  }

  // Start of ChildEventListener methods
  public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {

    if (filter == null || !filter.filter(snapshot)) {
      mSnapshots.add(snapshot);
      notifyChangedListeners(OnChangedListener.EventType.ADDED, mSnapshots.size()-1);
    }

  }

  public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
    int index = getIndexForKey(snapshot.getKey());
    if (index == -1) {
      mSnapshots.add(snapshot);
      index = mSnapshots.size() - 1;
    } else {
      if (filter.filter(snapshot)) {
        mSnapshots.remove(index);
      } else {
        mSnapshots.set(index, snapshot);
      }
    }
    notifyChangedListeners(OnChangedListener.EventType.CHANGED, index);

  }

  public void onChildRemoved(DataSnapshot snapshot) {
    if (filter == null || !filter.filter(snapshot)){
      int index = getIndexForKey(snapshot.getKey());
      mSnapshots.remove(index);
      notifyChangedListeners(OnChangedListener.EventType.REMOVED, index);
    }

  }

  public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
    if (filter == null || !filter.filter(snapshot)) {
      int oldIndex = getIndexForKey(snapshot.getKey());
      mSnapshots.remove(oldIndex);
      int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
      mSnapshots.add(newIndex, snapshot);
      notifyChangedListeners(OnChangedListener.EventType.MOVED, newIndex, oldIndex);
    }
  }

  public void onCancelled(DatabaseError databaseError) {
    notifyCancelledListeners(databaseError);
  }
  // End of ChildEventListener methods

  public void setOnChangedListener(OnChangedListener listener) {
    mListener = listener;
  }

  protected void notifyChangedListeners(OnChangedListener.EventType type, int index) {
    notifyChangedListeners(type, index, -1);
  }

  protected void notifyChangedListeners(OnChangedListener.EventType type, int index, int oldIndex) {
    if (mListener != null) {
      mListener.onChanged(type, index, oldIndex);
    }
  }

  protected void notifyCancelledListeners(DatabaseError databaseError) {
    if (mListener != null) {
      mListener.onCancelled(databaseError);
    }
  }
}
