/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.app.splitwise_clone.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.app.splitwise_clone.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ReviewViewHolder> {

    private static final String TAG = FriendsAdapter.class.getSimpleName();
private String userName;
    private StorageReference mPhotosStorageReference;
    private FirebaseStorage mFirebaseStorage;
private Context mContext;
    private static int viewHolderCount;
    private Set friendsSet;
    private Map<String, String> friendsImageMap = new HashMap<>();
    Iterator it;
    private Map<String, Map<String, Float>> expenseMatrix;

    public FriendsAdapter(Map<String, Map<String, Float>> mBalance, Map<String, String> images, Context context) {
        userName = FirebaseUtils.getUserName();
        this.expenseMatrix = mBalance;
        friendsImageMap = images;
        mContext = context;
        it = expenseMatrix.entrySet().iterator();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mPhotosStorageReference= mFirebaseStorage.getReference();
        viewHolderCount = 0;
    }
    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.friends_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        ReviewViewHolder viewHolder = new ReviewViewHolder(view);
        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: "
                + viewHolderCount);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ReviewViewHolder holder, int position) {
        Log.d(TAG, "#" + position);

//        holder.setIsRecyclable(false);
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return expenseMatrix.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the list, ie 0 through getItemCount() - 1
        TextView tv_friend_name;
        // Will display which ViewHolder is displaying this data
        TextView tv_status;
        ImageView friendImage;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            friendImage = itemView.findViewById(R.id.friendImage);
            tv_friend_name = itemView.findViewById(R.id.tv_friend_name);
            tv_status = itemView.findViewById(R.id.tv_status);
        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         * @param listIndex Position of the item in the list
         */
        void bind(int listIndex) {
            String stat="";
            Map.Entry pair = (Map.Entry) it.next();
            String friendName = (String) pair.getKey();
            loadImage(friendName);
            Map<String, Float> allGroups = new HashMap<>();
            allGroups = (Map<String, Float>) pair.getValue();

//create text view for each group
            Iterator it2 = allGroups.entrySet().iterator();
            while (it2.hasNext()){
                Map.Entry pair2 = (Map.Entry) it2.next();
                String groupName = (String) pair2.getKey();
                Float amount = (float) pair2.getValue();
                String stat1 = "you owe ";
                if(amount > 0)
                    stat1 = "owes you ";
                stat += stat1 + amount + " from group " + groupName + "\n";

            }

            tv_friend_name.setText(friendName);
            tv_status.setText(stat);
        }

        private void loadImage(String friendName){

            final StorageReference imageRef = mPhotosStorageReference.child("images/users/" + friendName);
            final long ONE_MEGABYTE = 1024 * 1024;
            imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Glide.with(mContext)
                            .load(bytes)
                            .asBitmap()
                            .into(friendImage);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                    Log.i(TAG, exception.getMessage());
                    // Handle any errors
                }
            });
        }
    }
}
