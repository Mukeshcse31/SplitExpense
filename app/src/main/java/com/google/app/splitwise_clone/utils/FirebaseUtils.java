package com.google.app.splitwise_clone.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {

    private static String TAG="TOTAL";

    public static String getUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = "";
        if (user != null) {
            displayName = user.getDisplayName();
        }
        return displayName;
    }


}
