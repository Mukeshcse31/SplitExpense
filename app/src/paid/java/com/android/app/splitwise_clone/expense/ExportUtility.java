package com.google.app.splitwise_clone.expense;

import android.content.Context;
import android.content.Intent;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.utils.ExpenseAdapter;

public class ExportUtility {

    public static void exportExpenses(Context context, String group_name){

        String expensePayload = ExpenseAdapter.expensePayload;
//        Log.i(TAG, ExpenseAdapter.expensePayload);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, group_name);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, expensePayload);

        //verify that this intent can be launched before starting
        if (sharingIntent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.export_title)));
    }
}
