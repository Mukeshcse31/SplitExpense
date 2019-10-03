package com.google.app.splitwise_clone;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager.widget.ViewPager;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.app.splitwise_clone.expense.ExpenseList;
import com.google.app.splitwise_clone.groups.AddGroup;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.List;

public class SummaryActivity extends AppCompatActivity implements GroupsFragment.onGroupClickListener, FriendsFragment.onFriendClickListener{

    private FirebaseStorage mFirebaseStorage;
    private DatabaseReference mDatabaseReference;
    private String TAG = "Friendslist_Page";
    private StorageReference mPhotosStorageReference;
    private ImageView profilePicture;
    private CollapsingToolbarLayout toolbar_container;
    private static final int RC_PHOTO_PICKER = 2;
    private String balanceSummaryTxt, userName = "anonymous";
    private TextView balance_summary_tv;
    private byte[] userImageByte;
    ViewPager viewPager;
    TabLayout tabLayout;
    public static String POSITION = "POSITION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        https://android.jlelse.eu/tablayout-and-viewpager-in-your-android-app-738b8840c38a
        // Set the content of the activity to use the  activity_main.xml layout file
        setContentView(R.layout.activity_summary);
        userName = FirebaseUtils.getUserName();
        toolbar_container = findViewById(R.id.toolbar_container);
        toolbar_container.setTitleEnabled(false);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(userName);
        setSupportActionBar(myToolbar);

//        getSupportActionBar().setDisplayShowTitleEnabled(true);
//        getSupportActionBar().setTitle(userName);

        mFirebaseStorage = AppUtils.getDBStorage();
        mDatabaseReference = AppUtils.getDBReference();

        profilePicture = findViewById(R.id.profilePicture);
        balance_summary_tv = findViewById(R.id.balance_summary_tv);

        // Find the view pager that will allow the user to swipe between fragments
        viewPager = (ViewPager) findViewById(R.id.viewpager);

        // Create an adapter that knows which fragment should be shown on each page
        SummaryPagerAdapter adapter = new SummaryPagerAdapter(this, getSupportFragmentManager());

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

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

//    private void setTransition() {
//        /* Set the ExitTransition, as well as the ExitSharedElementCallback */
//        getWindow().setExitTransition(TransitionInflater.from(this).inflateTransition(R.transition.transition_grid_exit));
//        setExitSharedElementCallback(new SharedElementCallback() {
//            @Override
//            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
//                RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(id);
//                if (viewHolder == null) {
//                    return;
//                }
//                sharedElements.put(names.get(0), viewHolder.itemView.findViewById(R.id.groupImage));
//            }
//        });
//    }

    @Override
    public void gotoClickedGroup(int index, String name, View ivProfile) {
        Intent intent = new Intent(this, ExpenseList.class);
        intent.putExtra(AddGroup.GROUP_NAME, name);

//        Intent intent = new Intent(/this, DetailsActivity.class);
// Pass data object in the bundle and populate details activity.
//        intent.putExtra(DetailsActivity.EXTRA_CONTACT, contact);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, (View)ivProfile, "profile");
        startActivity(intent, options.toBundle());

//        startActivity(intent);
    }

    @Override
    public void gotoEditGroup(int index, String groupName, List<DataSnapshot> dataSnapshotGroupList) {
        Intent intent = new Intent(this, AddGroup.class);
        Group group = dataSnapshotGroupList.get(index).getValue(Group.class);
        intent.putExtra(AddGroup.GROUP_NAME, groupName);
        intent.putExtra(AddGroup.GROUP_EDIt, group);
        startActivity(intent);
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
                intent = new Intent(SummaryActivity.this, AddFriend.class);
                startActivity(intent);
                break;

            case R.id.gotoAddGroup:
                intent = new Intent(SummaryActivity.this, AddGroup.class);
                startActivity(intent);
                break;

            case R.id.invite_friend:
                inviteAFriend();
                break;

            //Sign Out
            case R.id.signout:
                FirebaseUtils.signOut(this);
                intent = new Intent(SummaryActivity.this, SignIn.class);
                startActivity(intent);
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
                            mDatabaseReference.child("users/" + userName + "/imageUrl").setValue(photoRef.getPath());

                            final StorageReference userImgRef = mPhotosStorageReference.child(userName);
                            final long ONE_MEGABYTE = 1024 * 1024;
                            userImgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    userImageByte = bytes;
//                                           https://stackoverflow.com/questions/46619510/how-can-i-download-image-on-firebase-storage
                                    //            https://github.com/bumptech/glide/issues/458

                                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                                    profilePicture.setImageBitmap(bmp);

                                    //TODO
                                    Glide.with(SummaryActivity.this)
                                            .load(bytes)
                                            .asBitmap()
                                            .placeholder(R.drawable.people_unselected)
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
//                            Drawable image = new BitmapDrawable(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
//                            toolbar_container.setContentScrim(image);
                            Glide.with(SummaryActivity.this)
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

    public void inviteAFriend() {

        String invite_text = getString(R.string.invite_text) + getPackageName();
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.invite_friend));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, invite_text);

        //verify that this intent can be launched before starting
        if (sharingIntent.resolveActivity(getPackageManager()) != null)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.invite_friend)));
    }

    @Override
    public void gotoGroupsList() {
        viewPager.setCurrentItem(1);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(POSITION, tabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        viewPager.setCurrentItem(savedInstanceState.getInt(POSITION));
    }

    @Override
    public void updateUserSummary(String balanceSummaryTxt){
        balance_summary_tv.setText(balanceSummaryTxt);
    }

    @Override
    public void onResume(){
        super.onResume();

        Log.i(TAG, "Activity on start");

    }

    @Override
    public void onBackPressed(){
//navigate from groups list to friends list
        if(viewPager.getCurrentItem() == 1)
        viewPager.setCurrentItem(0);
    }
}