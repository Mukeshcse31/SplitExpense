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
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Map;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ReviewViewHolder> {

    private static final String TAG = GroupsAdapter.class.getSimpleName();
    private StorageReference mPhotosStorageReference;
    private FirebaseStorage mFirebaseStorage;
    private static int viewHolderCount;
    List<DataSnapshot>  mDataSnapshotList;
    private OnClickListener mOnClickListener;

    public GroupsAdapter(List<DataSnapshot> dataSnapshotList, OnClickListener listener) {
        mDataSnapshotList = dataSnapshotList;
        mOnClickListener = listener;
        mFirebaseStorage = FirebaseStorage.getInstance();
        mPhotosStorageReference= mFirebaseStorage.getReference();
        viewHolderCount = 0;
    }

    public interface OnClickListener{
        void gotoSharedGroup(int index, String name);
        void gotoEditGroup(int index, String name);
    }

    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.group_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
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
        TextView tv_member_status;
        ImageView group_image;

        public ReviewViewHolder(View itemView) {
            super(itemView);
            group_image = itemView.findViewById(R.id.group_image);
            tv_group_name = itemView.findViewById(R.id.tv_group_name);
            tv_member_status = itemView.findViewById(R.id.tv_member_status);
        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         * @param listIndex Position of the item in the list
         */
        void bind(final int listIndex) {

            DataSnapshot d = mDataSnapshotList.get(listIndex);

            Group group = d.getValue(Group.class);
            Map<String, SingleBalance> members = group.getMembers();
            final String group_name = d.getKey();
            loadImage(group_name);
            String member_status = "";

            for (Map.Entry<String, SingleBalance> entrySet : members.entrySet()){
                String name = entrySet.getKey();
                SingleBalance sb = entrySet.getValue();
                member_status += String.format("%s %s %s\n", name, sb.getStatus(),sb.getAmount());
            }

            tv_group_name.setText(group_name);
            tv_member_status.setText(member_status);


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
        }

        private void loadImage(String groupName){

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
