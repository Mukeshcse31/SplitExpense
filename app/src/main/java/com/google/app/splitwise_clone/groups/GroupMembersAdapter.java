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

public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.ReviewViewHolder> {

    private static final String TAG = GroupMembersAdapter.class.getSimpleName();

    private Set membersSet;
    Iterator<String> it;
    private Map<String, String> group_members;
    private OnClickListener mOnClickListener;

    public GroupMembersAdapter(Map<String, String> group_members, Context context) {
        mOnClickListener = (OnClickListener) context;
        this.group_members = group_members;

        membersSet = group_members.keySet();
        it = membersSet.iterator();
    }

    public interface OnClickListener {
        void removeFriendFromGroup(String name);
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

//        viewHolderCount++;
//        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: " + viewHolderCount);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ReviewViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return group_members.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the other, ie 0 through getItemCount() - 1
        TextView tv_friend_name;
        ImageView member_iv;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            tv_friend_name = itemView.findViewById(R.id.tv_friend_name);
            member_iv = itemView.findViewById(R.id.member_iv);

            member_iv.setImageDrawable(member_iv.getContext().getDrawable(R.drawable.minus));


        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a other item.
         *
         * @param listIndex Position of the item in the other
         */
        void bind(int listIndex) {

            final String member_name = it.next();
            String email = group_members.get(member_name);
            String temp = member_name + tv_friend_name.getContext().getString(R.string.new_line) + email;
            tv_friend_name.setText(temp);
            member_iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.removeFriendFromGroup(member_name);
                }
            });

        }
    }

    public void setData(Map<String, String> members) {
        group_members = members;
        notifyDataSetChanged();
    }
}
