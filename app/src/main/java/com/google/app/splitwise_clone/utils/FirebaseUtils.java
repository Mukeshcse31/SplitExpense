package com.google.app.splitwise_clone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.SignIn;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.notification.MySingleton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {

    private static String userName;
    private static String TAG = "utils";

    public static String getUserName() {

        if(userName == null || TextUtils.isEmpty(userName)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userName = user.getDisplayName();
            }
        }
        return userName;
    }

    public static void signOut(final Context context) {

        //unsubscribe from notification
            FirebaseMessaging.getInstance().unsubscribeFromTopic(userName);
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "getInstanceId failed", task.getException());
                                return;
                            }

                            // Get new Instance ID token
                            String token = task.getResult().getToken();
                            try {
                                FirebaseInstanceId.getInstance().deleteInstanceId();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

//                            unSubscribe(context, token);
                            Log.d("token", token);
                        }
                    });

        //sign out
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        //clear the shared preferences user credentials
        userName = null;
        SharedPreferences prefs = context.getSharedPreferences(SignIn.SPLIT_PREFS, 0);
        prefs.edit().remove(SignIn.USERNAME_KEY).commit();
        prefs.edit().remove(SignIn.PASSWORD_KEY).commit();
    }

//    https://www.itsalif.info/content/android-volley-tutorial-http-get-post-put
    public static void unSubscribe(final Context mContext, String registrationToken) {
        String url = mContext.getString(R.string.unsbuscribe_url) + registrationToken;
        final String serverKey = "key=" + mContext.getString(R.string.server_key);
        StringRequest dr = new StringRequest(Request.Method.DELETE, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.i(TAG, "successfully unsubscribed");
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error.
                        error.printStackTrace();
                        Log.i(TAG, "unsubscribe failure");
                    }
                }
        ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put(mContext.getString(R.string.notif_Authorization), serverKey);
                return params;
            }};

        MySingleton.getInstance(mContext).addToRequestQueue(dr);
}
}