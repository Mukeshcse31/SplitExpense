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
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ReviewViewHolder> {

    private static final String TAG = ExpenseAdapter.class.getSimpleName();

    private static int viewHolderCount;
    String userName;
    List<DataSnapshot> mDataSnapshotList;
    Context mContext;
    private OnClickListener mOnClickListener;

    public ExpenseAdapter(List<DataSnapshot> dataSnapshotList, OnClickListener listener) {
        mDataSnapshotList = dataSnapshotList;
        mOnClickListener = listener;
        userName = FirebaseUtils.getUserName();
        viewHolderCount = 0;
    }

    public interface OnClickListener {
        void gotoExpenseDetails(String expenseId, int index);
    }

    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        mContext = viewGroup.getContext();
        int layoutIdForListItem = R.layout.expense_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        ReviewViewHolder viewHolder = new ReviewViewHolder(view);

//        viewHolder.tv_review.setText("ViewHolder index: " + viewHolderCount);

        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: " + viewHolderCount);
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
        TextView dateSpent_tv, tv_expenseDescription, tv_debt_credit, tv_amount;
        // Will display which ViewHolder is displaying this data
        TextView tv_paidBy;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            dateSpent_tv = itemView.findViewById(R.id.dateSpent_tv);
            tv_expenseDescription = (TextView) itemView.findViewById(R.id.tv_expenseDescription);
            tv_paidBy = (TextView) itemView.findViewById(R.id.tv_paidBy);

            tv_debt_credit = itemView.findViewById(R.id.tv_debt_credit);
            tv_amount = itemView.findViewById(R.id.tv_amount);
        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         *
         * @param listIndex Position of the item in the list
         */
        void bind(final int listIndex) {

            DataSnapshot d = mDataSnapshotList.get(listIndex);

            Expense expense = d.getValue(Expense.class);

            String date = expense.getDateSpent();
            String payer = expense.getPayer();
            float totalAmount = expense.getTotal();
            String debtCredit = "you borrowed";

            //1. date
            SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");
            SimpleDateFormat df1 = new SimpleDateFormat("MMM");
            SimpleDateFormat df2 = new SimpleDateFormat("DD");
            String dateString = "";
            try {
                Date date1 = df.parse(date);
                dateString = df1.format(date1) + "\n" + df2.format(date1);
            } catch (Exception e) {
            }
            dateSpent_tv.setText(dateString);

            //2. description
            tv_expenseDescription.setText(expense.getDescription());

            if (TextUtils.equals(payer, userName)) {
                payer = "You";
                debtCredit = "you lent";
            }

            //3.paid By
            tv_paidBy.setText(String.format("%s paid $%.2f", payer, totalAmount));

            //TODO if the userName is not available, the expense is not shared with him
            // so checking if the username is available, otherwiser app crashed
            Map<String, SingleBalance> splitExpense = expense.getSplitExpense();

            if(splitExpense.containsKey(userName)) {
                SingleBalance sb = splitExpense.get(userName);
                float amountDue = sb.getAmount();


                if (amountDue < 0) {
                    tv_debt_credit.setTextColor(mContext.getColor(R.color.red));
                    tv_amount.setTextColor(mContext.getColor(R.color.red));
                }
                //4. your credit
                tv_debt_credit.setText(debtCredit);

                //5. amount due
                tv_amount.setText(String.format("$%.2f", amountDue));
            }

            //click event handler to edit
            final String expense_id = d.getKey();
            tv_expenseDescription.getRootView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoExpenseDetails(expense_id, listIndex);
                }
            });
        }
    }
}
