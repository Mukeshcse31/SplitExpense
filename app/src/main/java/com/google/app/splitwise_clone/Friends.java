package com.google.app.splitwise_clone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.app.splitwise_clone.groups.GroupsList;
import com.google.app.splitwise_clone.model.Friend;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.FriendsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class Friends extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth.IdTokenListener mIdTokenListener;
    private String displayName = "anonymous";
    private String TAG = "Friends_Page";
    private RecyclerView friends_rv;
    private FriendsAdapter mFriendsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            displayName = user.getDisplayName();
            // User is signed in
        } else {
            // No user is signed in
        }

        getSupportActionBar().setTitle(displayName);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        friends_rv = (RecyclerView) findViewById(R.id.friends_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        friends_rv.setLayoutManager(layoutManager);

        //Get the amount owed in all the groups by the user
        Query query = mDatabaseReference.child("users").orderByChild("mukesh").limitToLast(1);//TODO parameterize name
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int id = 0;
                if (dataSnapshot.exists()) {
                    Map<String, SingleBalance> allbalances = null;
                    for (DataSnapshot i : dataSnapshot.getChildren()) {

                        Log.i(TAG, i.getKey());
                        Friend f = i.getValue(Friend.class);
//                        Map<String, SingleBalance> balances= f.getAllBalance();
                        id = f.getId();
                        allbalances = f.getBalances();
                        SingleBalance sb1 = new SingleBalance(1212, "test");
//                        f.addToBalance("new_fri", sb1);
//                        f.addToGroup("G3");
//                        mDatabaseReference.child("friends/" + id).setValue(f);
                    }

                    mFriendsAdapter = new FriendsAdapter(allbalances, Friends.this);
                    friends_rv.setAdapter(mFriendsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.saveFriend:

                Query query = mDatabaseReference.child("users").orderByChild("mukesh").limitToLast(1);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int id = 0;
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                Log.i(TAG, i.getKey());
                                Friend f = i.getValue(Friend.class);
                                id = f.getId();
                                //Add a user to a group
//                        f.addToGroup("G1");
//                        f.addToGroup("G2");
//                        mDatabaseReference.child("friends/" + id).setValue(f);
//                        f.removeFromGroup("G2");
//                        mDatabaseReference.child("friends/" + id).setValue(f);
                            }
                        }
                        Intent intent = new Intent(Friends.this, GroupsList.class);
                        intent.putExtra("friendId", id);
                        startActivity(intent);

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

                break;

            case R.id.gotoGroups:
                Intent intent = new Intent(Friends.this, GroupsList.class);
                startActivity(intent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

}