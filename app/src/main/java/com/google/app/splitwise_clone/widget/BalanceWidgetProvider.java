package com.google.app.splitwise_clone.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;

import com.google.app.splitwise_clone.FriendsList;
import com.google.app.splitwise_clone.SignIn;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BalanceWidgetProvider extends AppWidgetProvider {

    static String userName;
    static String TAG = BalanceWidgetProvider.class.getSimpleName();
    static Map<String, Map<String, Float>> amountGroup;
    static Float amountSpentByUser = 0.2f;

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {

//         There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {

            // refresh all widgets
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, BalanceWidgetProvider.class);
            onUpdate(context, mgr, mgr.getAppWidgetIds(cn));
        }
        super.onReceive(context, intent);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static RemoteViews getSingleRemoteView(Context context) {

        Log.i("Widget", "invoked");
        // Construct the RemoteViews object
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_balance_amount);

        Iterator it = amountGroup.entrySet().iterator();
        String text="";
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String friendName = (String) pair.getKey();
            Map<String, Float> allGroups = new HashMap<>();
            allGroups = (Map<String, Float>) pair.getValue();

//create text view for each group
            Iterator it2 = allGroups.entrySet().iterator();
            String stat = "";
            Float amount = 0.2f;
            while (it2.hasNext()) {
                Map.Entry pair2 = (Map.Entry) it2.next();
                String groupName = (String) pair2.getKey();
                amount = (float) pair2.getValue();
                String stat1 = "you owe ";
                if (amount > 0)
                    stat1 = "owes you ";
                stat += String.format("\t%s  %f from group %s \n",stat1, amount, groupName);

            }

            text += String.format("\n%s: %s\n", friendName, stat);
        }
        views.setTextViewText(R.id.widget_balance_amount, "amount spent by you " + amountSpentByUser + "\n" +
                text);


//        views.setImageViewResource(R.id.widget_ingredient_name, R.drawable.launcher_icon);
        // Construct an Intent object includes web adresss.
        Intent intent = new Intent(context, FriendsList.class);//TODO show the correct recipe's step activity

//        intent.putExtra(SignIn.RECIPE_SELECTED, mRecipe);

        // In widget we are not allowing to use intents as usually. We have to use PendingIntent instead of 'startActivity'
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        // Here the basic operations the remote view can do.
        views.setOnClickPendingIntent(R.id.widget_balance_amount, pendingIntent);
        return views;

    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId) {

        SharedPreferences prefs = context.getSharedPreferences(SignIn.SPLIT_PREFS, 0);
        userName = prefs.getString(SignIn.DISPLAY_NAME_KEY, "");

        Map<String, Float> amountSpentByMember = new HashMap<>();
        Map<String, Float> amountDueByMember  = new HashMap<>();
        Map<String, Map<String, Float>> expenseMatrix = new HashMap<>();
        List<String> friends = new ArrayList<>();
        Map<String, SingleBalance> members= new HashMap<>();
        Iterator it = expenseMatrix.entrySet().iterator();
        if(TextUtils.isEmpty(userName)) return;

        //update the participant's total amount
        final DatabaseReference mDatabaseReference;
        mDatabaseReference = AppUtils.getDBReference();

        Query query = mDatabaseReference.child("groups/").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Balance balance = new Balance();
                    amountGroup = new HashMap<>();

                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Group group = i.getValue(Group.class);
                        String groupName = i.getKey();
                        Map<String, SingleBalance> sb = group.getMembers();
                        SingleBalance sb1 = sb.get(userName);
                        Float amountSpentForGroup = sb1.getAmount();
                        Map<String, Float> dues = sb1.getSplitDues();

                        Iterator it = dues.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String expenseParticipant = (String) pair.getKey();
                            Float amount = (Float) pair.getValue();
                            //update user Balance
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
                    amountGroup.remove(userName);
                    balance.setGroups(amountGroup);
                    mDatabaseReference.child("users/" + userName + "/balances/").setValue(balance);
                    Log.i(TAG, "total calculation");

//                  Get current width to decide on single plant vs garden grid view

                    Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                    int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                    final RemoteViews rv;
                    rv = getSingleRemoteView(context);

                    appWidgetManager.updateAppWidget(appWidgetId, rv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

//        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
//        PlantWateringService.startActionUpdateWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Perform any action when one or more AppWidget instances have been deleted
    }

    @Override
    public void onEnabled(Context context) {
        // Perform any action when an AppWidget for this provider is instantiated
        BalanceService.startActionUpdateWidgets(context);
        Log.i("ONEnabled", "called");
    }

    @Override
    public void onDisabled(Context context) {
        // Perform any action when the last AppWidget instance for this provider is deleted
    }
}