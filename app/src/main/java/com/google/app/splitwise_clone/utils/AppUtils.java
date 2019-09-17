package com.google.app.splitwise_clone.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import com.google.app.splitwise_clone.R;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AppUtils {

    public static int findImage(Context context, String desc) {

        String matchImage = "";
        int result;
        Map<String, String> allImages = new HashMap<>();
        Resources res = context.getResources();

        String[] food = res.getStringArray(R.array.food);
        String[] bulb = res.getStringArray(R.array.bulb);
        String[] gas = res.getStringArray(R.array.gas);
        String[] cart = res.getStringArray(R.array.cart);
        String[] house = res.getStringArray(R.array.house);

        for (String i : food) allImages.put(i.toLowerCase(), "food");
        for (String i : bulb) allImages.put(i.toLowerCase(), "bulb");
        for (String i : gas) allImages.put(i.toLowerCase(), "gas");
        for (String i : cart) allImages.put(i.toLowerCase(), "cart");
        for (String i : house) allImages.put(i.toLowerCase(), "house");

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
            if(!TextUtils.isEmpty(matchImage)) break;
        }

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
                result = R.drawable.list;
        }
        return result;
    }

}
