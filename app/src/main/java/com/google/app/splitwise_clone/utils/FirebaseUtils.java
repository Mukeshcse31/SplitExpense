package com.google.app.splitwise_clone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.app.splitwise_clone.SignIn;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {

    private static String userName;
    private static String TAG = "TOTAL";

    public static String getUserName() {

        if(userName == null || TextUtils.isEmpty(userName)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userName = user.getDisplayName();
            }
        }
        return userName;
    }

    public static void signOut(Context context) {

        //unsubscribe from notification
        FirebaseMessaging.getInstance().unsubscribeFromTopic(userName);

        //sign out
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        //clear the shared preferences user credentials
        SharedPreferences prefs = context.getSharedPreferences(SignIn.SPLIT_PREFS, 0);
        prefs.edit().remove(SignIn.USERNAME_KEY).commit();
        prefs.edit().remove(SignIn.PASSWORD_KEY).commit();
    }
}