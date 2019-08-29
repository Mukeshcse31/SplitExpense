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
import com.google.app.splitwise_clone.model.SingleBalance;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ReviewViewHolder> {

    private static final String TAG = FriendsAdapter.class.getSimpleName();
private String userName;
private Context mContext;
    private static int viewHolderCount;
//    List<String> friends;
    private Set friendsSet;
    Iterator it;
    private Map<String, Map<String, Float>> expenseMatrix;

    public FriendsAdapter(Map<String, Map<String, Float>> mBalance, Context context) {
        userName = FirebaseUtils.getUserName();
        this.expenseMatrix = mBalance;
        mContext = context;
        it = expenseMatrix.entrySet().iterator();
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

//        viewHolder.tv_review.setText("ViewHolder index: " + viewHolderCount);

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

        public ReviewViewHolder(View itemView) {
            super(itemView);

            tv_friend_name = (TextView) itemView.findViewById(R.id.tv_friend_name);
            tv_status = (TextView) itemView.findViewById(R.id.tv_status);
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

    }
}
