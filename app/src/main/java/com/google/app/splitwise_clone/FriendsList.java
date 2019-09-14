package com.google.app.splitwise_clone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.app.splitwise_clone.groups.AddGroup;
import com.google.app.splitwise_clone.groups.GroupsList;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.notification.SendNotificationLogic;
import com.google.app.splitwise_clone.utils.ExpenseAdapter;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.FriendsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FriendsList extends AppCompatActivity {

    private FirebaseStorage mFirebaseStorage;
    private DatabaseReference mDatabaseReference;
    private StorageReference mPhotosStorageReference;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth.IdTokenListener mIdTokenListener;
    private String userName = "anonymous";
    private ImageView profilePicture;
    private static final int RC_PHOTO_PICKER = 2;
private TextView balance_summary_tv;
    List<String> groupMember = new ArrayList<>();
    private Map<String, SingleBalance> members;
    private Map<String, Float> splitDues = new HashMap<>();
    Map<String, Float> balances;

    private Map<String, String> friendsImageMap = new HashMap<>();
    Map<String, Float> amountSpentByMember = null;
    Map<String, Float> amountDueByMember = null;
    Map<String, Map<String, Float>> expenseMatrix = null;
    List<String> friends = new ArrayList<>();

    private String TAG = "Friendslist_Page";
    private RecyclerView friends_rv;
    private FriendsAdapter mFriendsAdapter;
    float amountSpentByUser = 0.2f, balanceAmount = 0.2f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);
        mFirebaseStorage = FirebaseStorage.getInstance();

        userName = FirebaseUtils.getUserName();
        profilePicture = findViewById(R.id.profilePicture);
        balance_summary_tv = findViewById(R.id.balance_summary_tv);

//        imageCard = findViewById(R.id.roundCardView);
        getSupportActionBar().setTitle(userName);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        friends_rv = findViewById(R.id.friends_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        friends_rv.setLayoutManager(layoutManager);
        profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });
        loadUserImage();
    }

    public void updateUsersAmount() {

        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();
        friends = new ArrayList<>();
        members = new HashMap<>();

        //update the participant's total amount
        final DatabaseReference mDatabaseReference;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        Query query = mDatabaseReference.child("groups/").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    balanceAmount = 0f;
                    Balance balance = new Balance();
                    Map<String, Map<String, Float>> amountGroup = new HashMap<>();

                    //loop through the groups
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Group group = i.getValue(Group.class);
                        String groupName = i.getKey();
                        Map<String, SingleBalance> sb = group.getMembers();
                        SingleBalance sb1 = sb.get(userName);
                        Float amountSpentForGroup = sb1.getAmount();
                        Map<String, Float> dues = sb1.getSplitDues();

                        Iterator it = dues.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String expenseParticipant = (String) pair.getKey();
                            Float amount = (Float) pair.getValue();

                            //update user Balance
                            balanceAmount += amount;

                            //group-wise balance
                            Map<String, Float> eachGroup = new HashMap<>();
                            eachGroup.put(groupName, amount);
                            if (amountGroup.containsKey(expenseParticipant)) {
                                amountGroup.get(expenseParticipant).put(groupName, amount);
                            } else
                                amountGroup.put(expenseParticipant, eachGroup);

                        }
                        amountSpentByUser += amountSpentForGroup; //TODO put it in the APP BAR
                    }
                    balance.setAmount(amountSpentByUser);
                    amountGroup.remove(userName);
                    balance.setGroups(amountGroup);

                    balance_summary_tv.setText(String.format("%s $%.2f\n%s %.2f","total amount spent by you", amountSpentByUser, "others owe", balanceAmount));

                    mFriendsAdapter = new FriendsAdapter(amountGroup,friendsImageMap, FriendsList.this);
                    friends_rv.setAdapter(mFriendsAdapter);
                    mDatabaseReference.child("users/" + userName + "/balances/").setValue(balance);
                    Log.i(TAG, "total calculation");
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
        getMenuInflater().inflate(R.menu.friends_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.saveFriend:
                intent = new Intent(FriendsList.this, AddFriend.class);
                startActivity(intent);
                break;

            case R.id.gotoGroups:
                intent = new Intent(FriendsList.this, GroupsList.class);
                startActivity(intent);
                break;

            case R.id.invite_friend:
                inviteAFriend();
                break;

            //Sign Out
            case R.id.signout:
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                SharedPreferences prefs = getSharedPreferences(MainActivity.SPLIT_PREFS, 0);
                prefs.edit().remove(MainActivity.USERNAME_KEY).commit();
                prefs.edit().remove(MainActivity.PASSWORD_KEY).commit();
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //update users' profile picture
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            // Get a reference to store file at chat_photos/<FILENAME>
            mPhotosStorageReference = mFirebaseStorage.getReference().child("images/users");
            final StorageReference photoRef = mPhotosStorageReference.child(userName);

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            //update the path of image in the DB users'
                            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
                            mDatabaseReference.child("users/" + userName + "/imageUrl").setValue(photoRef.getPath());

                            final StorageReference group1 = mPhotosStorageReference.child("images/users/" + userName);
                            final long ONE_MEGABYTE = 1024 * 1024;
                            group1.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
//                                           https://stackoverflow.com/questions/46619510/how-can-i-download-image-on-firebase-storage
                                    //            https://github.com/bumptech/glide/issues/458
                                    Glide.with(FriendsList.this)
                                            .load(bytes)
                                            .asBitmap()
                                            .into(profilePicture);

//                                    Log.i(TAG, "photo download " + mPhotosStorageReference.getPath());
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {

                                    Log.i(TAG, exception.getMessage());
                                    // Handle any errors
                                }
                            });
                        }
                    });
        }
    }

    private void loadUserImage(){

        Query query = mDatabaseReference.child("users/" + userName + "/imageUrl");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    String imagePath = (String) dataSnapshot.getValue();
                    if(TextUtils.isEmpty(imagePath)) return;

//                    https://firebase.google.com/docs/storage/android/download-files
                    mPhotosStorageReference = mFirebaseStorage.getReference();
                    StorageReference islandRef = mPhotosStorageReference.child(imagePath);

                    final long ONE_MEGABYTE = 1024 * 1024;
                    islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            // Data for "images/island.jpg" is returns, use this as needed
                            Glide.with(FriendsList.this)
                                    .load(bytes)
                                    .asBitmap()
                                    .into(profilePicture);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                            Log.i(TAG, exception.toString());
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        updateUsersAmount();
    }

    public void inviteAFriend(){

//        Log.i(TAG, "package name" + getPackageName());
        String invite_text = getString(R.string.invite_text) + getPackageName();
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.invite_friend));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, invite_text);

        //verify that this intent can be launched before starting
        if (sharingIntent.resolveActivity(getPackageManager()) != null)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.invite_friend)));
    }
}