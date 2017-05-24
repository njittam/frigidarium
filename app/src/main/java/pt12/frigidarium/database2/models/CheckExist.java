package pt12.frigidarium.database2.models;

import com.google.firebase.database.DatabaseError;

/**
 * Created by mattijn on 24/05/17.
 */

public interface CheckExist<T> {
    public  void onExist(T product);
    public void onDoesNotExist(String uid);
    public void onError(DatabaseError error);
}