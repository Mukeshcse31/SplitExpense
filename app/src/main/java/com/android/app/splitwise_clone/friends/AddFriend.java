package com.android.app.splitwise_clone.friends;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.app.splitwise_clone.R;
import com.android.app.splitwise_clone.utils.AppUtils;
import com.android.app.splitwise_clone.utils.FirebaseUtils;
import com.android.app.splitwise_clone.SummaryActivity;
import com.android.app.splitwise_clone.model.SingleBalance;
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
    String db_users, db_balances, db_groups, db_archivedExpenses, db_expenses, db_members, db_nonMembers, db_owner, db_photoUrl, db_amount, db_status, db_friends, db_email, db_name, db_imageUrl;

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
        db_users = getString(R.string.db_users);
        db_balances = getString(R.string.db_balances);
        db_groups = getString(R.string.db_groups);
        db_archivedExpenses = getString(R.string.db_archivedExpenses);
        db_expenses = getString(R.string.db_expenses);
        db_members = getString(R.string.db_members);
        db_nonMembers = getString(R.string.db_nonMembers);
        db_owner = getString(R.string.db_owner);
        db_photoUrl = getString(R.string.db_photoUrl);
        db_amount = getString(R.string.db_amount);
        db_status = getString(R.string.db_status);
        db_friends = getString(R.string.db_friends);
        db_email = getString(R.string.db_email);
        db_name = getString(R.string.db_name);
        db_imageUrl = getString(R.string.db_imageUrl);
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

                if (!AppUtils.checkUserName(friendName)) {
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
                    Query query = mDatabaseReference.child(db_users + "/" + userName + "/" + db_friends + "/" + friendName).equalTo("true");
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            //Check for existing friend
                            if (dataSnapshot.exists()) {
                                mFriendName.setError(getString(R.string.error_username_friend));
                                AppUtils.showSnackBar(AddFriend.this, findViewById(android.R.id.content), getString(R.string.error_username_friend));
                            } else {

                                Query query = mDatabaseReference.child(db_users + "/" + friendName);
                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        //Check for existing userName
                                        if (dataSnapshot.exists()) {

                                            Query query = mDatabaseReference.child(db_users + "/" + friendName + "/" + db_email);
                                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    //Check if the email matches
                                                    if (dataSnapshot.exists()) {
                                                        String email = (String) dataSnapshot.getValue();
//                                                            String name = user.getName();
                                                        if (friendEmail.compareToIgnoreCase(email) == 0) {
                                                            mDatabaseReference.child(db_users + "/" + userName + "/" + db_friends + "/" + friendName).setValue(true);
                                                            mDatabaseReference.child(db_users + "/" + friendName + "/" + db_friends + "/" + userName).setValue(true);
                                                            addFriendsToGroups(friendName, friendEmail);
                                                        } else
                                                            mFriendEmail.setError(getString(R.string.email_nomatch));
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                }
                                            });
                                        } else
                                            mFriendName.setError(getString(R.string.error_username_notfound));
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
                }
                break;

            case R.id.cancel:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addFriendsToGroups(final String friend_name, final String email) {

        //friend is added to the groups in which the user is the owner
        Query query = mDatabaseReference.child(db_groups).orderByChild(db_owner).equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    SingleBalance sb1 = new SingleBalance(friend_name);
                    sb1.setEmail(email);
                    Map<String, Float> splitDues = new HashMap<>();
                    splitDues.put(userName, 0.0f);
                    sb1.setSplitDues(splitDues);

                    //loop through the groups
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        String group_name = i.getKey();
                        mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_nonMembers + "/" + friend_name).setValue(sb1);
                    }

                    //init the user balance
                    mDatabaseReference.child(db_users + "/" + userName + "/" + db_balances + "/" + db_amount).setValue(0);
                }

                gotoSummaryActivity(friend_name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void gotoSummaryActivity(String friendname) {

        final Intent intent = new Intent(AddFriend.this, SummaryActivity.class);
        intent.putExtra(FRIEND_ADDED, getString(R.string.friend_added) + " " + friendname);
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabaseReference = AppUtils.getDBReference();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppUtils.closeDBReference(mDatabaseReference);
        Log.i(TAG, "listener cleared");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "config changed");

    }
}
