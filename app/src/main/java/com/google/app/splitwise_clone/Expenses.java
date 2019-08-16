package com.google.app.splitwise_clone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.app.splitwise_clone.model.Friend;
import com.google.app.splitwise_clone.model.InstantMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Expenses extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private Button msendMessage, addaFriend_bt;
    private String TAG = "Expense";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        msendMessage = findViewById(R.id.sendMessage);
//        addaFriend_bt = findViewById(R.id.addFriend);
//        addaFriend_bt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(Expenses.this, AddFriend.class);
//                startActivity(intent);
//            }
//        });
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }


    public void sendMessage(View v) {

        Log.i(TAG, "DB");
        InstantMessage msg = new InstantMessage("a", "b");

        try {
//            mDatabaseReference.child("messages").setValue(msg);

            //https://stackoverflow.com/questions/37403747/firebase-permission-denied
            mDatabaseReference.child("messages").push().setValue(msg, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {

                    Log.i(TAG, databaseError.getDetails());

                }
            });
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    public void addAGroup(View v){


    }


    public void addAFriend(View v) {


    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.navigation, menu);
//        return true;
//    }

}
