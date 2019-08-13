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
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.database.DataSnapshot;
import java.util.List;
import java.util.Map;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ReviewViewHolder> {

    private static final String TAG = GroupsAdapter.class.getSimpleName();

    private static int viewHolderCount;
    List<DataSnapshot>  mDataSnapshotList;

    public GroupsAdapter(List<DataSnapshot> dataSnapshotList, Context context) {
        mDataSnapshotList = dataSnapshotList;
        viewHolderCount = 0;
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

        public ReviewViewHolder(View itemView) {
            super(itemView);

            tv_group_name = (TextView) itemView.findViewById(R.id.tv_group_name);
            tv_member_status = (TextView) itemView.findViewById(R.id.tv_member_status);
        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         * @param listIndex Position of the item in the list
         */
        void bind(int listIndex) {

            DataSnapshot d = mDataSnapshotList.get(listIndex);

            Group group = d.getValue(Group.class);
            Map<String, SingleBalance> members = group.getMembers();
            String group_name = group.getName();
            String member_status = "";

            for (Map.Entry<String, SingleBalance> entrySet : members.entrySet()){
                String name = entrySet.getKey();
                SingleBalance sb = entrySet.getValue();
                member_status += String.format("%s %s %s\n", name, sb.getStatus(),sb.getAmount());
            }

            tv_group_name.setText(group_name);
            tv_member_status.setText(member_status);

        }

    }
}
