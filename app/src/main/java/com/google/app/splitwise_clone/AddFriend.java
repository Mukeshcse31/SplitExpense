package com.google.app.splitwise_clone;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.groups.AddGroup;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.model.User;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddFriend extends AppCompatActivity {

    private AutoCompleteTextView mFriendEmail;
    private AutoCompleteTextView mFriendName;
    private DatabaseReference mDatabaseReference;
    private String TAG = "ADDAFriend";
    public static final String FRIEND_ADDED = "friend_added";
    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.add_friend));
        userName = FirebaseUtils.getUserName();
        mFriendName = findViewById(R.id.friend_name);
        mFriendEmail = findViewById(R.id.friend_email);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mnu_add_friend, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.addFriend:
                boolean cancel = false;
                View focusView = null;
                final String friendName = mFriendName.getText().toString().trim();
                final String friendEmail = mFriendEmail.getText().toString().toLowerCase();//convert email to lowercase

                //check the fields

                if(!AppUtils.checkUserName(friendName)){
                    focusView = mFriendName;
                    mFriendName.setError(getString(R.string.error_username));
                    cancel = true;
                }

                if (TextUtils.isEmpty(friendEmail)) {
                    mFriendEmail.setError(getString(R.string.error_field_required));
                    focusView = mFriendEmail;
                    cancel = true;
                } else if (!AppUtils.checkEmail(friendEmail)) {
                    mFriendEmail.setError(getString(R.string.error_invalid_email));
                    focusView = mFriendEmail;
                    cancel = true;
                }

                if (cancel) {
                    // There was an error; don't attempt login and focus the first form field with an error.
                    focusView.requestFocus();
                } else {
//Check for existing email id
//                https://stackoverflow.com/questions/51607449/what-is-the-different-betwen-equalto-and-startat-endat-in-firebase-and-whe/51610286
                    Query query = mDatabaseReference.child("users/" + userName + "/friends/" + friendName);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
AppUtils.showSnackBar(AddFriend.this, findViewById(android.R.id.content), " is already a friend");
                            }
                            else{

                                Query query = mDatabaseReference.child("users/").orderByChild("email").startAt(friendEmail).endAt(friendEmail);
                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                                User user = i.getValue(User.class);
                                                String name = user.getName();
                                                if (friendName.compareToIgnoreCase(name) == 0) {
                                                    mDatabaseReference.child("users/" + userName + "/friends/" + friendName).setValue(true);
                                                    mDatabaseReference.child("users/" + friendName + "/friends/" + userName).setValue(true);
                                                    addFriendsToGroups(friendName, friendEmail);
                                                } else {
                                                    mFriendName.setError(getString(R.string.username_exists));
                                                }
                                            }
                                        } else {
                                            mFriendEmail.setError(getString(R.string.email_exists));
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                           /* //check for existing name
                            Query query = mDatabaseReference.child("users").orderByChild("name").startAt(friendName).endAt(friendName);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        mFriendName.setError(getString(R.string.friendname_exists));
                                    }
                                    else{
                                        //a new user to be added
                                        User user = new User(friendName, friendEmail);
                                        user.addAsFriend(userName);
                                        mDatabaseReference.child("users/" + friendName).setValue(user, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {

                                                Intent intent = new Intent(AddFriend.this, SummaryActivity.class);
                                                intent.putExtra(FRIEND_ADDED, getString(R.string.friend_added));
                                                startActivity(intent);

                                                if (databaseError != null)
                                                    Log.i(TAG, databaseError.getDetails());

                                                finish();
                                            }
                                        });
                                        //the new friend is to be added to the userName
                                        mDatabaseReference.child("users/" + userName + "/friends/" + friendName).setValue(true);
                                    }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                        }*/

        }
                break;

            case R.id.cancel:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

private void addFriendsToGroups(final String friend_name, final String email){


        Query query = mDatabaseReference.child("groups/").orderByChild("members/" + userName + "/name").equalTo(userName);

    query.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            if (dataSnapshot.exists()) {
                SingleBalance sb1 = new SingleBalance(friend_name);
                sb1.setEmail(email);
                //loop through the groups
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    String group_name = i.getKey();
                    mDatabaseReference.child("groups/" + group_name + "/nonMembers/" + friend_name).setValue(sb1);
                }
            }

            gotoSummaryActivity(friend_name);
        }
        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    });
}
    private void gotoSummaryActivity(String friendname){

        final Intent intent = new Intent(AddFriend.this, SummaryActivity.class);
        intent.putExtra(FRIEND_ADDED, friendname + " added");
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume(){
        super.onResume();
        mDatabaseReference = AppUtils.getDBReference();
    }

    @Override
    public void onPause(){
        super.onPause();
        AppUtils.closeDBReference(mDatabaseReference);
        Log.i(TAG, "listener cleared");
    }
}
