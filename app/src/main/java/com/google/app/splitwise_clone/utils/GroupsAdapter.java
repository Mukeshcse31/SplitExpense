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
import android.text.TextUtils;
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
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ReviewViewHolder> {

    private static final String TAG = GroupsAdapter.class.getSimpleName();
    private StorageReference mPhotosStorageReference;
    private FirebaseStorage mFirebaseStorage;
    private static int viewHolderCount;
    List<DataSnapshot> mDataSnapshotList;
    private OnClickListener mOnClickListener;
    private String userName;
    private Context mContext;

    public GroupsAdapter(List<DataSnapshot> dataSnapshotList, OnClickListener listener) {
        mDataSnapshotList = dataSnapshotList;
        mOnClickListener = listener;
        mFirebaseStorage = FirebaseStorage.getInstance();
        mPhotosStorageReference = mFirebaseStorage.getReference();
        userName = FirebaseUtils.getUserName();
        viewHolderCount = 0;
    }

    public interface OnClickListener {
        void gotoSharedGroup(int index, String name);

        void gotoEditGroup(int index, String name);
    }

    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        mContext = viewGroup.getContext();
        int layoutIdForListItem = R.layout.group_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        ReviewViewHolder viewHolder = new ReviewViewHolder(view);

//        viewHolder.tv_review.setText("ViewHolder index: " + viewHolderCount);

        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: "
                + viewHolderCount);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ReviewViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mDataSnapshotList.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the list, ie 0 through getItemCount() - 1
        TextView tv_group_name;
        // Will display which ViewHolder is displaying this data
        TextView tv_member_status, tv_status;
        ImageView group_image;

        public ReviewViewHolder(View itemView) {
            super(itemView);
            group_image = itemView.findViewById(R.id.group_image);
            tv_group_name = itemView.findViewById(R.id.tv_group_name);
            tv_member_status = itemView.findViewById(R.id.tv_member_status);
            tv_status = itemView.findViewById(R.id.tv_status);

        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         *
         * @param listIndex Position of the item in the list
         */
        void bind(final int listIndex) {

            DataSnapshot d = mDataSnapshotList.get(listIndex);

            Group group = d.getValue(Group.class);
            SingleBalance sb = group.getMembers().get(userName);

            //update group name
            final String group_name = d.getKey();
            tv_group_name.setText(group_name);

            loadImage(group_name);

            //member balance
            float balanceAmount = 0.0f;
            String member_status = "";

            Map<String, Float> splitDues = sb.getSplitDues();
            Iterator it = splitDues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String friendName = (String) pair.getKey();
                float amount = Float.parseFloat(String.valueOf(pair.getValue()));
                balanceAmount += amount;
                if (!TextUtils.equals(userName, friendName))
                    member_status += String.format("%s %s you $%.2f\n", friendName, amount > 0 ? "owes" : "lent", amount);

            }
            tv_member_status.setText(member_status);

            //aggregate status
            if (balanceAmount == 0) {
                Log.i(TAG, "N/A");
            } else {
                String aggr_status = mContext.getString(R.string.you_lent);
                tv_status.setTextColor(mContext.getColor(R.color.green));
                if (balanceAmount < 0) {
                    tv_status.setTextColor(mContext.getColor(R.color.orange));
                    aggr_status = mContext.getString(R.string.you_borrowed);
                }
                tv_status.setText(String.format("%s %s $%.2f", aggr_status, mContext.getString(R.string.group_debt_separator), balanceAmount));
            }

            //set listeners
            group_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoEditGroup(listIndex, group_name);
                }
            });
            tv_member_status.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoSharedGroup(listIndex, group_name);
                }
            });
            tv_group_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoSharedGroup(listIndex, group_name);
                }
            });
            tv_status.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoSharedGroup(listIndex, group_name);
                }
            });
        }

        private void loadImage(String groupName) {

            final StorageReference imageRef = mPhotosStorageReference.child("images/groups/" + groupName);
            final long ONE_MEGABYTE = 1024 * 1024;
            imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Glide.with(group_image.getContext())
                            .load(bytes)
                            .asBitmap()
                            .into(group_image);
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
