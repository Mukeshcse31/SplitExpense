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
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.database.DataSnapshot;

import java.util.List;
import java.util.Map;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ReviewViewHolder> {

    private static final String TAG = ExpenseAdapter.class.getSimpleName();

    private static int viewHolderCount;
    List<DataSnapshot>  mDataSnapshotList;
    private OnClickListener mOnClickListener;

    public ExpenseAdapter(List<DataSnapshot> dataSnapshotList, OnClickListener listener) {
        mDataSnapshotList = dataSnapshotList;
        mOnClickListener = listener;
        viewHolderCount = 0;
    }

    public interface OnClickListener{
        void gotoExpenseDetails(String expenseId, int index);
    }

    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.expense_list_item;
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
        TextView dateSpent_tv, tv_expenseDescription, tv_status;
        // Will display which ViewHolder is displaying this data
        TextView tv_paidBy;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            dateSpent_tv = itemView.findViewById(R.id.dateSpent_tv);
            tv_expenseDescription = (TextView) itemView.findViewById(R.id.tv_expenseDescription);
            tv_paidBy = (TextView) itemView.findViewById(R.id.tv_paidBy);
            tv_status = itemView.findViewById(R.id.tv_status);
        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         * @param listIndex Position of the item in the list
         */
        void bind(final int listIndex) {

            DataSnapshot d = mDataSnapshotList.get(listIndex);

            Expense expense = d.getValue(Expense.class);

            String date = expense.getDateSpent();
String spender = expense.getMemberSpent();

            Map<String, SingleBalance> splitExpense = expense.getSplitExpense();
            final String expense_id = d.getKey();
            SingleBalance singleBalance = splitExpense.get("Mukesh");
            tv_status.setText(singleBalance.getStatus() + "\n" + singleBalance.getAmount());

//            for (Map.Entry<String, SingleBalance> entrySet : splitExpense.entrySet()){
//                String name = entrySet.getKey();
//                SingleBalance sb = entrySet.getValue();
//                member_status += String.format("%s %s %s\n", name, sb.getStatus(),sb.getAmount());
//            }
            dateSpent_tv.setText(date);
            tv_expenseDescription.setText(expense.getDescription());
            tv_paidBy.setText(String.format("%s paid $%.2f",spender,expense.getTotal()));

            tv_expenseDescription.getRootView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoExpenseDetails(expense_id, listIndex);
                }
            });
        }

    }
}
