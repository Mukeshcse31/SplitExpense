package com.google.app.splitwise_clone.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.SignIn;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.notification.MySingleton;
import com.google.app.splitwise_clone.widget.BalanceWidgetProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FirebaseUtils {

    private static String userName;
    private final static String TAG = "utils";

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
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //sign out
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        //clear the shared preferences user credentials
        userName = null;
        SharedPreferences prefs = context.getSharedPreferences(SignIn.SPLIT_PREFS, 0);
        prefs.edit().remove(SignIn.USERNAME_KEY).commit();
        prefs.edit().remove(SignIn.PASSWORD_KEY).commit();
        prefs.edit().remove(SignIn.DISPLAY_NAME_KEY).commit();

        //update account_image
        int[] ids = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(new ComponentName(context.getApplicationContext(), BalanceWidgetProvider.class));
        BalanceWidgetProvider myWidget = new BalanceWidgetProvider();
        myWidget.onUpdate(context, AppWidgetManager.getInstance(context),ids);
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


    public static void updateUsersAmount(Context context, final String member) {

        final String db_groups = context.getString(R.string.db_groups);
        final String db_members = context.getString(R.string.db_members);
        final String db_name = context.getString(R.string.db_name);
        final String db_users = context.getString(R.string.db_users);
        final String db_balances = context.getString(R.string.db_balances);

        //update the participant's total amount
        final DatabaseReference mDatabaseReference = AppUtils.getDBReference();
        Query query = mDatabaseReference.child(db_groups).orderByChild(db_members+ "/" + member + "/" + db_name).equalTo(member);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    float balanceAmount = 0f;
                    float amountSpentByUser = 0.0f;
                    Balance balance = new Balance();
                    Map<String, Map<String, Float>> amountGroup = new HashMap<>();

                    //loop through the groups
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Group group = i.getValue(Group.class);
                        String groupName = i.getKey();
                        Map<String, SingleBalance> sb = group.getMembers();
                        SingleBalance sb1 = sb.get(member);
                        Float amountSpentForGroup = sb1.getAmount();
                        Map<String, Float> dues = sb1.getSplitDues();

                        Iterator it = dues.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String expenseParticipant = (String) pair.getKey();
                            Float amount = (Float) pair.getValue();

                            //update user Balance
                            balanceAmount += amount;

                            //group-wise balance
                            Map<String, Float> eachGroup = new HashMap<>();
                            eachGroup.put(groupName, amount);
                            if (amountGroup.containsKey(expenseParticipant)) {
                                amountGroup.get(expenseParticipant).put(groupName, amount);
                            } else
                                amountGroup.put(expenseParticipant, eachGroup);

                        }
                        amountSpentByUser += amountSpentForGroup;
                    }
                    balance.setAmount(amountSpentByUser);
                    amountGroup.remove(member);
                    balance.setGroups(amountGroup);
                    mDatabaseReference.child(db_users + "/" + member + "/" + db_balances).setValue(balance);
                    Log.i(TAG, "total calculation");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

}