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
import android.graphics.Movie;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.app.splitwise_clone.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder> {

    private static final String TAG = FriendsAdapter.class.getSimpleName();
    private String userName;
    private StorageReference mPhotosStorageReference;
    private FirebaseStorage mFirebaseStorage;
    private FriendsAdapter.OnClickListener mOnClickListener;
    private Context mContext;
    private static int viewHolderCount;
    Iterator it;
    private Map<String, Map<String, Float>> expenseMatrix;

    public FriendsAdapter(Map<String, Map<String, Float>> mBalance, OnClickListener mOnClickListener) {
        userName = FirebaseUtils.getUserName();
        this.expenseMatrix = mBalance;
        this.mOnClickListener = mOnClickListener;
        it = expenseMatrix.entrySet().iterator();
        mFirebaseStorage = AppUtils.getDBStorage();
        mPhotosStorageReference = mFirebaseStorage.getReference();
        viewHolderCount = 0;

    }


    public interface OnClickListener {
        void gotoGroup();
    }

    @Override
    public FriendsViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        mContext = viewGroup.getContext();
        int layoutIdForListItem = R.layout.friends_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        FriendsViewHolder viewHolder = new FriendsViewHolder(view);
        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: "
                + viewHolderCount);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(FriendsViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.bind(position);
}

    @Override
    public int getItemCount() {
        return expenseMatrix.size();
    }

    class FriendsViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the other, ie 0 through getItemCount() - 1
        TextView tv_friend_name;
        // Will display which ViewHolder is displaying this data
        TextView tv_status;
        ImageView friendImage;

        public FriendsViewHolder(View itemView) {
            super(itemView);

            itemView.animate();
            friendImage = itemView.findViewById(R.id.friendImage);
            tv_friend_name = itemView.findViewById(R.id.tv_friend_name);
            tv_status = itemView.findViewById(R.id.tv_status);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoGroup();
                }
            });

        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a other item.
         *
         * @param listIndex Position of the item in the other
         */
        void bind(int listIndex) {
            String stat = "";
            Map.Entry pair = (Map.Entry) it.next();
            String friendName = (String) pair.getKey();
            loadImage(friendName);
            Map<String, Float> allGroups = new HashMap<>();
            allGroups = (Map<String, Float>) pair.getValue();

//create text view for each group
            Iterator it2 = allGroups.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry pair2 = (Map.Entry) it2.next();
                String groupName = (String) pair2.getKey();
                Float amount = (float) pair2.getValue();
                String status = String.format("%s %s", mContext.getString(R.string.you_owe), AppUtils.getColoredSpanned("$" + Math.abs(amount), mContext.getString(R.string.orange)));

                if (amount > 0){
                    status = String.format("%s %s", mContext.getString(R.string.owes_you), AppUtils.getColoredSpanned("$" + Math.abs(amount), mContext.getString(R.string.green)));

                }
                stat += String.format("%s %s %s <br>",status, mContext.getString(R.string.from_group), groupName);

            }

            tv_friend_name.setText(friendName);
            tv_status.setText(HtmlCompat.fromHtml(stat, HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);

        }

        private void loadImage(String friendName) {

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
