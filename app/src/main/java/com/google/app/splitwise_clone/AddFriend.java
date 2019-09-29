package com.google.app.splitwise_clone;

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
import com.google.app.splitwise_clone.model.User;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class AddFriend extends AppCompatActivity {

    private AutoCompleteTextView mFriendEmail;
    private AutoCompleteTextView mFriendName;
    private DatabaseReference mDatabaseReference;
    private String TAG = "ADDAFriend";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.add_friend));

        mFriendName = findViewById(R.id.friend_name);
        mFriendEmail = findViewById(R.id.friend_email);
        mDatabaseReference = AppUtils.getDBReference();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mnu_add_friend, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String userName = FirebaseUtils.getUserName();
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

//                https://stackoverflow.com/questions/51607449/what-is-the-different-betwen-equalto-and-startat-endat-in-firebase-and-whe/51610286
                Query query = mDatabaseReference.child("users").orderByChild("email").startAt(friendEmail).endAt(friendEmail);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        int id = 1;
                        if (dataSnapshot.exists()) {
                            mFriendEmail.setError(getString(R.string.email_exists));
                            Toast.makeText(AddFriend.this, "Email already exists", Toast.LENGTH_LONG).show();

                        } else {
                            //a new user to be added
                            User user = new User(friendName, friendEmail);
                            user.addAsFriend(userName);
                            mDatabaseReference.child("users/" + friendName).setValue(user, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
//https://stackoverflow.com/questions/30729312/how-to-dismiss-a-snackbar-using-its-own-action-button
                                    final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content),
                                            getString(R.string.friend_added), Snackbar.LENGTH_LONG);

                                    snackBar.setAction(getString(R.string.close), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // Call your action method here
                                            snackBar.dismiss();
                                        }
                                    });
                                    snackBar.show();
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
        }
                break;

            case R.id.cancel:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
