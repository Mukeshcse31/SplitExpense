package com.google.app.splitwise_clone.expense;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.utils.ExpenseAdapter;

public class ExportUtility {

    private static InterstitialAd mInterstitialAd;
    private static String TAG = "AD";

    public static void exportExpenses(final Context context, final String group_name) {

//        https://developers.google.com/admob/android/interstitial#create_an_interstitial_ad_object

        if (mInterstitialAd == null)
            MobileAds.initialize(context, context.getString(R.string.ad_application_code));
        mInterstitialAd = new InterstitialAd(context);
        mInterstitialAd.setAdUnitId(context.getString(R.string.ad_unit_id));

        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
            }

            @Override
            public void onAdClosed() {
                Log.i(TAG, "add closed");

                String expensePayload = context.getString(R.string.export_heading);
                expensePayload += ExpenseAdapter.expensePayload;
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, group_name);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, expensePayload);

                //verify that this intent can be launched before starting
                if (sharingIntent.resolveActivity(context.getPackageManager()) != null)
                    context.startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.export_title)));
            }
        });
    }
}
