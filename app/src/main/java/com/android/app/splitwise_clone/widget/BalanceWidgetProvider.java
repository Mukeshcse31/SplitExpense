package com.android.app.splitwise_clone.widget;

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

import com.android.app.splitwise_clone.R;
import com.android.app.splitwise_clone.model.Balance;
import com.android.app.splitwise_clone.utils.AppUtils;
import com.android.app.splitwise_clone.SignIn;
import com.android.app.splitwise_clone.SummaryActivity;
import com.android.app.splitwise_clone.model.SingleBalance;
import com.android.app.splitwise_clone.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BalanceWidgetProvider extends AppWidgetProvider {

    static String userName;
    static String TAG = BalanceWidgetProvider.class.getSimpleName();
    static Map<String, Map<String, Float>> amountGroup;
    static private Float amountSpentByUser = 0.0f;
    static private String userSummary = "";

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

    public static RemoteViews getSingleRemoteView(Context mContext, String summary) {

        Log.i("Widget", "invoked");

        // Construct the RemoteViews object
        final RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_balance_amount);

        views.setTextViewText(R.id.widget_balance_amount, String.format("%s $%.2f \n %s", mContext.getString(R.string.amount_spent_by_you), amountSpentByUser, summary));

//        views.setImageViewResource(R.id.widget_ingredient_name, R.drawable.launcher_icon);
        // Construct an Intent object includes web adresss.
        Intent intent = new Intent(mContext, SummaryActivity.class);

        // In account_image we are not allowing to use intents as usually. We have to use PendingIntent instead of 'startActivity'
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        // Here the basic operations the remote view can do.
        views.setOnClickPendingIntent(R.id.widget_balance_amount, pendingIntent);
        return views;

    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId) {

        userSummary = "";
        SharedPreferences prefs = context.getSharedPreferences(SignIn.SPLIT_PREFS, 0);
        userName = prefs.getString(SignIn.DISPLAY_NAME_KEY, "");
        amountSpentByUser = 0.0f;
        Map<String, Float> amountSpentByMember = new HashMap<>();
        Map<String, Float> amountDueByMember = new HashMap<>();
        Map<String, Map<String, Float>> expenseMatrix = new HashMap<>();
        final List<String> friends = new ArrayList<>();
        Map<String, SingleBalance> members = new HashMap<>();
        Iterator it = expenseMatrix.entrySet().iterator();
        if (TextUtils.isEmpty(userName)) {
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_balance_amount);

            views.setTextViewText(R.id.widget_balance_amount, context.getString(R.string.no_user_signed));
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        //update the participant's total amount
        final DatabaseReference mDatabaseReference;
        mDatabaseReference = AppUtils.getDBReference();

        String db_groups = context.getString(R.string.db_groups);
        String db_members = context.getString(R.string.db_members);
        String db_name = context.getString(R.string.db_name);
        final String db_users = context.getString(R.string.db_users);

        final String db_balances = context.getString(R.string.db_balances);

        Query query = mDatabaseReference.child(db_users + "/" + userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String summary = "";
                if (dataSnapshot.exists()) {
                    Balance balance = new Balance();
                    amountGroup = new HashMap<>();
                    User user = dataSnapshot.getValue(User.class);

                    if (user.getBalances() == null) {
                        appWidgetManager.updateAppWidget(appWidgetId, getSingleRemoteView(context, summary));
                    } else {
                        amountSpentByUser = user.getBalances().getAmount();

                        Map<String, Map<String, Float>> friends_balances = user.getBalances().getGroups();
                        Iterator it1 = friends_balances.entrySet().iterator();


                        while (it1.hasNext()) {//for each friend

                            Map.Entry pair = (Map.Entry) it1.next();

                            String friendName = (String) pair.getKey();
                            Map<String, Float> allGroups = new HashMap<>();
                            allGroups = (Map<String, Float>) pair.getValue();

//create text view for each group
                            Iterator it2 = allGroups.entrySet().iterator();
                            while (it2.hasNext()) {
                                Map.Entry pair2 = (Map.Entry) it2.next();
                                String groupName = (String) pair2.getKey();
                                Float amount = (float) pair2.getValue();
                                DecimalFormat df = new DecimalFormat("#.##");
                                String status = String.format("%s %s", context.getString(R.string.you_owe), "$" + df.format(Math.abs(amount)));

                                if (amount > 0) {
                                    status = String.format("%s %s", context.getString(R.string.owes_you), "$" + df.format(Math.abs(amount)));

                                }
                                summary += String.format("%s %s %s", status, context.getString(R.string.from_group), groupName);
                            }
                            summary = friendName + ":\n" + summary;
                        }
                        if (TextUtils.isEmpty(summary))
                            summary = context.getString(R.string.no_expense);
                        appWidgetManager.updateAppWidget(appWidgetId, getSingleRemoteView(context, summary));
                    }
                } else {
//                    appWidgetManager.updateAppWidget(appWidgetId, getSingleRemoteView(context, context.getString(R.string.no_expense)));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(TAG, "error");
            }
        });
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