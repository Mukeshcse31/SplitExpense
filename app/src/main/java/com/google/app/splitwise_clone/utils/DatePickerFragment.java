package com.google.app.splitwise_clone.utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import java.util.Calendar;
import androidx.fragment.app.DialogFragment;

import com.google.app.splitwise_clone.R;
//https://android--examples.blogspot.com/2015/05/how-to-use-datepickerdialog-in-android.html
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //Use the current date as the default date in the date picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        //Create a new DatePickerDialog instance and return it
        /*
            DatePickerDialog Public Constructors - Here we uses first one
            public DatePickerDialog (Context context, DatePickerDialog.OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth)
            public DatePickerDialog (Context context, int theme, DatePickerDialog.OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth)
         */
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }
    public void onDateSet(DatePicker view, int year, int month, int day) {
        //Do something with the date chosen by the user
        Button tv = getActivity().findViewById(R.id.date_btn);
//        tv.setText("Date changed...");
//        tv.setText(tv.getText() + "\nYear: " + year);
//        tv.setText(tv.getText() + "\nMonth: " + month);
//        tv.setText(tv.getText() + "\nDay of Month: " + day);

        String stringOfDate = String.format("%d-%02d-%02d",year, month, day);
        tv.setText(stringOfDate);
    }
}
