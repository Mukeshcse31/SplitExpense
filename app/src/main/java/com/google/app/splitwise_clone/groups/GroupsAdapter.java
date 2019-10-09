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
package com.google.app.splitwise_clone.groups;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.CircleTransform;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
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
        mFirebaseStorage = AppUtils.getDBStorage();
        mPhotosStorageReference = mFirebaseStorage.getReference();
        userName = FirebaseUtils.getUserName();
        viewHolderCount = 0;
    }

    public interface OnClickListener {
        void gotoSharedGroup(int index, String name, View view);

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

        // Will display the position in the other, ie 0 through getItemCount() - 1
        TextView tv_group_name;
        // Will display which ViewHolder is displaying this data
        TextView tv_member_status, tv_status, tv_amount;
        ImageView group_image, edit_img;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            edit_img = itemView.findViewById(R.id.edit_img);
            group_image = itemView.findViewById(R.id.group_image);
            tv_group_name = itemView.findViewById(R.id.tv_group_name);
            tv_member_status = itemView.findViewById(R.id.tv_member_status);
            tv_status = itemView.findViewById(R.id.tv_status);
            tv_amount = itemView.findViewById(R.id.tv_amount);

        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a other item.
         *
         * @param listIndex Position of the item in the other
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
                DecimalFormat df = new DecimalFormat("#.##");

                balanceAmount += amount;
                String status = String.format("%s %s %s %s", friendName, mContext.getString(R.string.lent_you), AppUtils.getColoredSpanned("$" + df.format(Math.abs(amount)), mContext.getString(R.string.orange)),"<br>");
                if (amount > 0){
                    status = String.format("%s %s %s %s", friendName, mContext.getString(R.string.owes_you), AppUtils.getColoredSpanned("$" + df.format(Math.abs(amount)), mContext.getString(R.string.green)), "<br>");
                }

                if (!TextUtils.equals(userName, friendName))
                    member_status += status;

            }
            if(!TextUtils.isEmpty(member_status))
                tv_member_status.setText(HtmlCompat.fromHtml(member_status, HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);

            //aggregate status
            if (balanceAmount == 0) {
                Log.i(TAG, "N/A");
            } else {
                String aggr_status = mContext.getString(R.string.you_lent);
                tv_status.setTextColor(mContext.getColor(R.color.green));
                tv_amount.setTextColor(mContext.getColor(R.color.green));
                if (balanceAmount < 0) {
                    tv_status.setTextColor(mContext.getColor(R.color.orange));
                    tv_amount.setTextColor(mContext.getColor(R.color.orange));
                    aggr_status = mContext.getString(R.string.you_borrowed);
                }
                tv_status.setText(aggr_status);
                tv_amount.setText(String.format("$%.2f",Math.abs(balanceAmount)));
            }

            //set listeners

            if(TextUtils.equals(group.getOwner(), userName)){
                edit_img.setVisibility(View.VISIBLE);
                edit_img.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        mOnClickListener.gotoEditGroup(listIndex, group_name);
                    }
                });
            }
            group_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoEditGroup(listIndex, group_name);
                }
            });
            tv_member_status.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoSharedGroup(listIndex, group_name, group_image);
                }
            });
            tv_group_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoSharedGroup(listIndex, group_name, group_image);
                }
            });
            tv_status.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoSharedGroup(listIndex, group_name, group_image);
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
                            .transform(new CircleTransform(mContext))
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
