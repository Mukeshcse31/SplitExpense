package com.google.app.splitwise_clone.notification;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.app.splitwise_clone.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SendNotificationLogic {
    private String FCM_API = "";
    private String serverKey = "";
    private String contentType = "";
    final String TAG = "NOTIFICATION TAG";
    private Context mContext;
    String NOTIFICATION_TITLE;
    String NOTIFICATION_MESSAGE;
    String TOPIC;

    public SendNotificationLogic(Context context) {
        mContext = context;
    }

    public void setData(String user, String title, String message) {
        TOPIC = "/topics/" + user; //topic must match with what the receiver subscribed to
        NOTIFICATION_TITLE = title;
        NOTIFICATION_MESSAGE = message;

    }

    public void send() {

        FCM_API = mContext.getString(R.string.FCM_API);
        contentType = mContext.getString(R.string.notif_contentType);
        serverKey = "key=" + mContext.getString(R.string.server_key);
        JSONObject notification = new JSONObject();
        JSONObject notifcationBody = new JSONObject();
        try {
            notifcationBody.put(mContext.getString(R.string.notif_title), NOTIFICATION_TITLE);
            notifcationBody.put(mContext.getString(R.string.notif_message), NOTIFICATION_MESSAGE);

            notification.put(mContext.getString(R.string.notif_to), TOPIC);
            notification.put(mContext.getString(R.string.notif_data), notifcationBody);

        } catch (JSONException e) {
            Log.e(TAG, "onCreate: " + e.getMessage());
        }
        sendNotification(notification);
    }

    private void sendNotification(JSONObject notification) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(SignIn.this, "Request error", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "onErrorResponse: Didn't work");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put(mContext.getString(R.string.notif_Authorization), serverKey);
                params.put(mContext.getString(R.string.notif_Content_Type), contentType);
                return params;
            }
        };
        MySingleton.getInstance(mContext).addToRequestQueue(jsonObjectRequest);
    }

}
