package com.google.app.splitwise_clone.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseUtils {

    public static String getUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = "";
        if (user != null) {
            displayName = user.getDisplayName();
        }
        return displayName;
    }


}
