package com.google.app.splitwise_clone.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import java.util.ArrayList;
import java.util.List;

//https://javamidpoint.wordpress.com/2017/02/21/android-multiselect-spinner-dialog-with-select-all-option/
public class MultiSpinner extends AppCompatSpinner implements
        DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

    private List items = new ArrayList<>();
    private boolean[] selected;
    private static String defaultText;
    private MultiSpinnerListener listener;
    private boolean allselected = false;
    int listsize;
    int totalItems;

    public MultiSpinner(Context context, MultiSpinnerListener listener) {
        super(context);
        this.listener = listener;
    }

    public MultiSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

        if (which == 0) {

            if (isChecked) {
                for (int i = 0; i < selected.length; i++) {
                    selected[i] = true;
                    listsize = totalItems - 1;
                    ((AlertDialog) dialog).getListView().setItemChecked(i, true);
                }

            } else {

                for (int i = 0; i < selected.length; i++) {
                    selected[i] = false;
                    listsize = 0;
                    ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                }
            }

        } else {

            if (isChecked) {
                selected[which] = true;
                ++listsize;

            } else {
                selected[which] = false;
                --listsize;
            }
        }

        if (listsize == totalItems - 1) {
            selected[0] = true;
            ((AlertDialog) dialog).getListView().setItemChecked(0, true);
        } else {
            selected[0] = false;
            ((AlertDialog) dialog).getListView().setItemChecked(0, false);
        }

//Log.e(“listsize”, String.valueOf(listsize));
//Log.e(“total items”, “” + totalItems);

    }

    @Override
    public void onCancel(DialogInterface dialog) {
// refresh text on spinner
        StringBuffer spinnerBuffer = new StringBuffer();
        boolean someUnselected = false;
        for (int i = 0; i < items.size(); i++) {
            if (selected[i] == true) {
                spinnerBuffer.append(items.get(i));
                spinnerBuffer.append(",");
            } else {
                someUnselected = true;
            }
        }
        String spinnerText;
        if (someUnselected) {
            spinnerText = spinnerBuffer.toString();
            if (spinnerText.length() > 2)
                spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
        } else {
            spinnerText = defaultText;
        }
        ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1,new String[]{"er","opp"});
        setAdapter(adapter);
        listener.onItemsSelected(selected);
    }

    @Override
    public boolean performClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems((CharSequence[]) items.toArray(new CharSequence[items.size()]), selected, this);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.setOnCancelListener(this);
        builder.show();
        return true;
    }

    public void setItems(List items, String allText,
                         MultiSpinnerListener listener) {
        this.items.clear();
        this.items.addAll(items);
//        items.add(0, "Select All");
//        this.items.addAll(items);
        listsize = 0;
        totalItems = items.size();
        selected = new boolean[items.size()];
        this.defaultText = allText;
        this.listener = listener;

        deSelectAll();
// all text on the spinner
        setDefaultText(allText);

    }

    public interface MultiSpinnerListener {
        void onItemsSelected(boolean[] selected);
    }

    public void setDefaultText(String text) {
        defaultText = text;
        ArrayAdapter adapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_1, new String[]{text});
        setAdapter(adapter);
    }

    public void deSelectAll() {

        for (int i = 0; i < selected.length; i++)
            selected[i] = false;
    }
}