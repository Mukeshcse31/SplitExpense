package com.google.app.splitwise_clone.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.SignIn;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.ExpenseCategory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AppUtils {

    private static DatabaseReference mDatabaseReference;
    private static FirebaseStorage firebaseStorage;

    public static int findImage(Context context, String desc) {

        String matchImage = getExpenseCategory(context, desc);
        int result;
        switch (matchImage) {

            case "food":
                result = R.drawable.food;
                break;

            case "bulb":
                result = R.drawable.bulb;
                break;

            case "gas":
                result = R.drawable.gas;
                break;

            case "cart":
                result = R.drawable.cart;
                break;

            case "house":
                result = R.drawable.house;
                break;

            default:
                result = R.drawable.other;
        }
        return result;
    }

    public static String getExpenseCategory(Context context, String desc) {

        String matchImage = ExpenseCategory.OTHER.getValue();
        Map<String, String> allImages = new HashMap<>();
        Resources res = context.getResources();

        String[] food = res.getStringArray(R.array.food);
        String[] bulb = res.getStringArray(R.array.bulb);
        String[] gas = res.getStringArray(R.array.gas);
        String[] cart = res.getStringArray(R.array.cart);
        String[] house = res.getStringArray(R.array.house);

        for (String i : food) allImages.put(i.toLowerCase(), ExpenseCategory.FOOD.getValue());
        for (String i : bulb) allImages.put(i.toLowerCase(), ExpenseCategory.BULB.getValue());
        for (String i : gas) allImages.put(i.toLowerCase(), ExpenseCategory.GAS.getValue());
        for (String i : cart) allImages.put(i.toLowerCase(), ExpenseCategory.CART.getValue());
        for (String i : house) allImages.put(i.toLowerCase(), ExpenseCategory.HOUSE.getValue());

        Iterator it;
        for (String exp_desc : desc.toLowerCase().split(" ")) {
            it = allImages.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String imageStr = (String) pair.getKey();
                if (TextUtils.indexOf(exp_desc, imageStr) > -1 || TextUtils.indexOf(imageStr, exp_desc) > -1) {
                    matchImage = (String) pair.getValue();
                    break;
                }
            }
            if (!TextUtils.equals(matchImage, ExpenseCategory.OTHER.getValue())) break;
        }
        return matchImage;
    }


    public static DatabaseReference getDBReference() {

        if (mDatabaseReference == null)
            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        return mDatabaseReference;
    }

    public static void closeDBReference() {

        mDatabaseReference = null;
    }

    public static FirebaseStorage getDBStorage() {

        if (firebaseStorage == null)
            firebaseStorage = FirebaseStorage.getInstance();
        return firebaseStorage;
    }

    public static LinkedHashMap reverseExpense(LinkedHashMap map) {

        LinkedHashMap<String, Expense> reverseMap = new LinkedHashMap<>();
        List<String> reverseOrderedKeys = new ArrayList<String>(map.keySet());
        Collections.reverse(reverseOrderedKeys);
        for (String key : reverseOrderedKeys) {
            reverseMap.put(key, (Expense) map.get(key));
        }
        return reverseMap;
    }


    /*
  https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out
   */
    public static boolean isOnline(Context context) {
//Complete move this to Network Utils class
        boolean status = false;

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        status = netInfo != null && netInfo.isConnectedOrConnecting();

        return status;
    }


    public static boolean checkGroupName(String groupName) {

        for(String name : groupName.split(" ")){

            Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");
            if (TextUtils.isEmpty(name) || !pattern.matcher(name).matches()) {
                return false;
            }

        }
        return true;
    }

    //verifies the userName, groupName
    public static boolean checkUserName(String name) {

        Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");
        if (TextUtils.isEmpty(name) || !pattern.matcher(name).matches()) {
            return false;
        }
        return true;
    }

    //verifies the user's and friends's email id
    public static boolean checkEmail(String email) {
        // You can add more checking logic here.
        if(email == null || TextUtils.isEmpty(email))
            return false;

        return email.contains("@");
    }


    public static void showOption(Menu mMenu, int[] ids) {

        if (mMenu == null) return;
        for (int id : ids) {
            MenuItem item = mMenu.findItem(id);
            item.setVisible(true);
        }
    }


    public static void hideOption(Menu mMenu, int[] ids) {

        if (mMenu == null) return;
        for (int id : ids) {
            MenuItem item = mMenu.findItem(id);
            item.setVisible(false);
        }
    }

    public static void showSnackBar(Context context, View view, String message){
        //https://stackoverflow.com/questions/30729312/how-to-dismiss-a-snackbar-using-its-own-action-button

        if(message == null || TextUtils.isEmpty(message)) return;

        final Snackbar snackBar = Snackbar.make(view,
                message, Snackbar.LENGTH_LONG);

        snackBar.setAction(context.getString(R.string.close), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call your action method here
                snackBar.dismiss();
            }
        });
        snackBar.show();
    }

    //        https://stackoverflow.com/questions/6094315/single-textview-with-multiple-colored-text
    public static String getColoredSpanned(String text, String color) {
        String input = "<font color=" + color + ">" + text + "</font>";
        return input;
    }

}
