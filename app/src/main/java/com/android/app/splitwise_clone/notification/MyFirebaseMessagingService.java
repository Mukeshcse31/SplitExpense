package com.android.app.splitwise_clone.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.android.app.splitwise_clone.R;
import com.android.app.splitwise_clone.utils.FirebaseUtils;
import com.android.app.splitwise_clone.SignIn;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    private final String ADMIN_CHANNEL_ID = "admin_channel";
    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    private static String SUBSCRIBE_TO = "userABC";
    String GROUP_KEY_WORK_EMAIL = "com.android.example.EXPENSE";
    static boolean groupPosted = false;

    @Override
    public void onNewToken(String token) {

        //whenever the user signs in, the subscribeToTopic would be updated in Sign in method
        SharedPreferences prefs = getSharedPreferences(SignIn.SPLIT_PREFS, 0);
        String displayName = prefs.getString(SignIn.USERNAME_KEY, "");
        displayName = FirebaseUtils.getUserName();
        if (!TextUtils.isEmpty(displayName))
            FirebaseMessaging.getInstance().subscribeToTopic(displayName);
        Log.i(TAG, "onTokenRefresh completed with token: " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        final Intent intent = new Intent(this, SignIn.class);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationID = new Random().nextInt(3000);
        int GROUP_ID = 3;
      /*
        Apps targeting SDK 26 or above (Android O) must implement notification channels and add its notifications
        to at least one of them. Therefore, confirm if version is Oreo or higher, then setup notification channel
      */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setupChannels(notificationManager);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.small_icon);

        Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.small_icon)
                .setLargeIcon(largeIcon)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_message))
                .setGroupSummary(true)
                .setGroup(GROUP_KEY_WORK_EMAIL)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notif_message)))
                .setContentIntent(pendingIntent);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.small_icon)
                .setLargeIcon(largeIcon)
                .setContentTitle(remoteMessage.getData().get(getString(R.string.notif_title)))
                .setContentText(remoteMessage.getData().get(getString(R.string.notif_message)))
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_WORK_EMAIL)
                .setSound(notificationSoundUri)
                .setContentIntent(pendingIntent);

        //Set notification color to match your app color template
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        notificationManager.notify(notificationID, notificationBuilder.build());


        if (!groupPosted) {
            notificationManager.notify(GROUP_ID, groupBuilder.build());
            groupPosted = true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupChannels(NotificationManager notificationManager) {
        CharSequence adminChannelName = "New notification";
        String adminChannelDescription = "Device to devie notification";

        NotificationChannel adminChannel;
        adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, adminChannelName, NotificationManager.IMPORTANCE_HIGH);
        adminChannel.setDescription(adminChannelDescription);
        adminChannel.enableLights(true);
        adminChannel.setShowBadge(true);
        adminChannel.setLightColor(R.color.red);
        adminChannel.enableVibration(true);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(adminChannel);
        }
    }
}