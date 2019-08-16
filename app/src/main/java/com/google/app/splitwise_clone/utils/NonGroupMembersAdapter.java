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

import androidx.recyclerview.widget.RecyclerView;

import com.google.app.splitwise_clone.R;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NonGroupMembersAdapter extends RecyclerView.Adapter<NonGroupMembersAdapter.ReviewViewHolder> {

    private static final String TAG = NonGroupMembersAdapter.class.getSimpleName();

    private static int viewHolderCount;
    private Set nonMembersSet;
    Iterator<String> it;
    private OnClickListener mOnClickListener;
    private Map<String, String> nonGroup_members;

    public NonGroupMembersAdapter(Map<String, String> nongroup_members, OnClickListener context) {
        mOnClickListener = (OnClickListener) context;
        this.nonGroup_members = nongroup_members;
        nonMembersSet = nonGroup_members.keySet();
        it = nonMembersSet.iterator();
        Set<Map.Entry<String, String>> ee = nonGroup_members.entrySet();
        ee.size();
        viewHolderCount = 0;
    }

    public interface OnClickListener {
        void addFriendToGroup(String name);
    }

    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.groupmember_list_item;
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
        return nonGroup_members.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the list, ie 0 through getItemCount() - 1
        TextView tv_friend_name;
        ImageView member_iv;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            tv_friend_name = (TextView) itemView.findViewById(R.id.tv_friend_name);
            member_iv = itemView.findViewById(R.id.member_iv);

            member_iv.setImageDrawable(member_iv.getContext().getDrawable(R.drawable.plus));

        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         *
         * @param listIndex Position of the item in the list
         */
        void bind(int listIndex) {

            final String member_name = it.next();
            String email = nonGroup_members.get(member_name);
            String temp = member_name + tv_friend_name.getContext().getString(R.string.new_line) + email;
            tv_friend_name.setText(temp);
            member_iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.addFriendToGroup(member_name);
                }
            });

        }

    }
}
