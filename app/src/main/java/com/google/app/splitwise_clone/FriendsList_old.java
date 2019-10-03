package com.google.app.splitwise_clone;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.FriendsAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FriendsList_old extends AppCompatActivity implements FriendsAdapter.OnClickListener {

    private FirebaseStorage mFirebaseStorage;
    private DatabaseReference mDatabaseReference;
    private StorageReference mPhotosStorageReference;
    private String userName = "anonymous";
    public static String USER_IMAGE = "user_image";
    public static String BALANCE_SUMMARY = "balance_summary";

    private byte[] userImageByte;
    private String balanceSummaryTxt;

    private ImageView profilePicture;
    private CollapsingToolbarLayout toolbar_container;
    private static final int RC_PHOTO_PICKER = 2;
    private TextView balance_summary_tv;
    Map<String, Float> amountSpentByMember = null;
    Map<String, Float> amountDueByMember = null;
    Map<String, Map<String, Float>> expenseMatrix = null;
    List<String> friends = new ArrayList<>();
    private String TAG = "Friendslist_Page";
    private RecyclerView friends_rv;
    private FriendsAdapter mFriendsAdapter;
    float amountSpentByUser = 0.0f, balanceAmount = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        toolbar_container = findViewById(R.id.toolbar_container);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mFirebaseStorage = AppUtils.getDBStorage();
        userName = FirebaseUtils.getUserName();
        profilePicture = findViewById(R.id.profilePicture);
        balance_summary_tv = findViewById(R.id.balance_summary_tv);

//        imageCard = findViewById(R.id.roundCardView);
        getSupportActionBar().setTitle(userName);
        mDatabaseReference = AppUtils.getDBReference();
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

        //update the participant's total amount
        Query query = mDatabaseReference.child("groups/").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    balanceAmount = 0f;
                    amountSpentByUser = 0.0f;
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

                    balanceSummaryTxt = String.format("%s $%.2f\n%s %.2f", "total amount spent by you", amountSpentByUser, "others owe", balanceAmount);
                    balance_summary_tv.setText(balanceSummaryTxt);

                    mFriendsAdapter = new FriendsAdapter(amountGroup, FriendsList_old.this);
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
        getMenuInflater().inflate(R.menu.mnu_summary, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.saveFriend:
                intent = new Intent(FriendsList_old.this, AddFriend.class);
                startActivity(intent);
                break;

//            case R.id.gotoGroups:
//                gotoGroupsList();
//                break;

            case R.id.invite_friend:
                inviteAFriend();
                break;

            //Sign Out
            case R.id.signout:
                FirebaseUtils.signOut(this);
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoGroupsList() {

//        Intent intent = new Intent(FriendsList_old.this, GroupsList.class);
//        if(userImageByte != null) intent.putExtra(USER_IMAGE, userImageByte);
//        intent.putExtra(BALANCE_SUMMARY, balanceSummaryTxt);
//        startActivity(intent);
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
                            mDatabaseReference.child("users/" + userName + "/imageUrl").setValue(photoRef.getPath());

                            final StorageReference group1 = mPhotosStorageReference.child("images/users/" + userName);
                            final long ONE_MEGABYTE = 1024 * 1024;
                            group1.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    userImageByte = bytes;
//                                           https://stackoverflow.com/questions/46619510/how-can-i-download-image-on-firebase-storage
                                    //            https://github.com/bumptech/glide/issues/458

                                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    profilePicture.setImageBitmap(bmp);

//                                    Glide.with(FriendsList.this)
//                                            .load(bytes)
//                                            .asBitmap()
//                                            .into(profilePicture);

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

    private void loadUserImage() {

        Query query = mDatabaseReference.child("users/" + userName + "/imageUrl");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    String imagePath = (String) dataSnapshot.getValue();
                    if (TextUtils.isEmpty(imagePath)) return;

//                    https://firebase.google.com/docs/storage/android/download-files
                    mPhotosStorageReference = mFirebaseStorage.getReference();
                    StorageReference islandRef = mPhotosStorageReference.child(imagePath);

                    final long ONE_MEGABYTE = 1024 * 1024;
                    islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            userImageByte= bytes;
                            // Data for "images/island.jpg" is returns, use this as needed
//                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                            profilePicture.setImageBitmap(bmp);
//                            Drawable image1 = new BitmapDrawable(FriendsList.getResources(), canvasBitmap);
                            Drawable image = new BitmapDrawable(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                            toolbar_container.setContentScrim(image);
//                            toolbar_container.setForeground();

                            Glide.with(FriendsList_old.this)
                                    .load(bytes)
                                    .asBitmap()
                                    .placeholder(R.drawable.person)
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

    public void inviteAFriend() {

//        Log.i(TAG, "package name" + getPackageName());
        String invite_text = getString(R.string.invite_text) + getPackageName();
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invite_friend));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, invite_text);

        //verify that this intent can be launched before starting
        if (sharingIntent.resolveActivity(getPackageManager()) != null)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.invite_friend)));
    }

    @Override
    public void gotoGroup() {
        gotoGroupsList();
    }
}