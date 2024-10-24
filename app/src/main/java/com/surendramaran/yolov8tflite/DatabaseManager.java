package com.surendramaran.yolov8tflite;



import android.app.Application;
import android.os.Bundle;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DatabaseManager extends Application {
    private static DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private static DatabaseReference mUserRef = mDatabase.child("Users");
    private static DatabaseReference mDeliveryRef = mDatabase.child("Deliveries");
    private static DatabaseReference mInventoryRef = mDatabase.child("Medicines");
    private static DatabaseReference mMedicineRef = mDatabase.child("Inventory");
    private static DatabaseReference mTransactionRef = mDatabase.child("Transactions");

    public static DatabaseReference getUserRef(){
        return mUserRef;
    }
    public static DatabaseReference getDeliveryRef(){
        return mDeliveryRef;
    }
    public static DatabaseReference getMedicineRef(){
        return mMedicineRef;
    }
    public static DatabaseReference getTransactionRef(){
        return mTransactionRef;
    }
    public static DatabaseReference getInventoryRef(){
        return mInventoryRef;
    }
}
