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
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
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
import java.util.Random;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private static final String TAG = ExpenseAdapter.class.getSimpleName();
    public static String expensePayload;
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
    public ExpenseViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        mContext = viewGroup.getContext();
        int layoutIdForListItem = R.layout.expense_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(layoutIdForListItem, viewGroup, false);
        ExpenseViewHolder viewHolder = new ExpenseViewHolder(view);

//        viewHolder.tv_review.setText("ViewHolder index: " + viewHolderCount);

        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: " + viewHolderCount);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.bind(position);
        // call Animation function
        setAnimation(holder.itemView, position);
    }


    private int lastPosition = -1;

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            ScaleAnimation anim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//            anim.setDuration(new Random().nextInt(501));//to make duration random number between [0,501)
            anim.setDuration(1000);
            viewToAnimate.startAnimation(anim);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return mDataSnapshotList.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the list, ie 0 through getItemCount() - 1
        TextView dateSpent_tv, tv_expenseDescription, tv_debt_credit, tv_amount, tv_paidBy;
        ImageView expense_img;

        public ExpenseViewHolder(View itemView) {
            super(itemView);

            expense_img = itemView.findViewById(R.id.expense_img);
            dateSpent_tv = itemView.findViewById(R.id.dateSpent_tv);
            tv_expenseDescription = itemView.findViewById(R.id.tv_expenseDescription);
            tv_paidBy = itemView.findViewById(R.id.tv_paidBy);

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

            //the latest expense should be on top
            DataSnapshot d = mDataSnapshotList.get(mDataSnapshotList.size() -1 - listIndex);

            Expense expense = d.getValue(Expense.class);

            String date = expense.getDateSpent();
            String payer = expense.getPayer();
            String description = expense.getDescription();
            float totalAmount = expense.getTotal();
            String debtCredit = mContext.getString(R.string.you_borrowed);
            String amountDueStr = mContext.getString(R.string.default_amount);
            //1. date
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat df1 = new SimpleDateFormat("MMM");
            SimpleDateFormat df2 = new SimpleDateFormat("dd");
            String dateString = "";
            try {
                Date date1 = df.parse(date);
                dateString = df1.format(date1) + "\n" + df2.format(date1);
            } catch (Exception e) {
            }
            dateSpent_tv.setText(dateString);

            int imageName = AppUtils.findImage(mContext, description);
            expense_img.setImageDrawable(mContext.getDrawable(imageName));

            //2. description
            tv_expenseDescription.setText(description);

            if (TextUtils.equals(payer, userName)) {
                payer = mContext.getString(R.string.you);
                debtCredit = mContext.getString(R.string.you_lent);
            }

            //3.paid By
            String paidBy = String.format("%s %s $%.2f",
                    payer, mContext.getString(R.string.paid), totalAmount);
            tv_paidBy.setText(paidBy);

            //TODO if the userName is not available, the expense is not shared with him
            // so checking if the username is available
            Map<String, SingleBalance> splitExpense = expense.getSplitExpense();

            if (splitExpense.containsKey(userName)) {
                SingleBalance sb = splitExpense.get(userName);
                float amountDue = sb.getAmount();


                if (amountDue < 0) {
                    tv_debt_credit.setTextColor(mContext.getColor(R.color.orange));
                    tv_amount.setTextColor(mContext.getColor(R.color.orange));
                }
                //4. your credit
                tv_debt_credit.setText(debtCredit);

                //5. amount due
                amountDueStr = String.format("$%.2f", Math.abs(amountDue));
                tv_amount.setText(amountDueStr);
            }

            //click event handler to edit
            final String expense_id = d.getKey();
            tv_expenseDescription.getRootView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoExpenseDetails(expense_id, listIndex);
                }
            });

            //update the payload
            expensePayload += String.format("\n%s,%s,%s,%s,%s",
                    dateString.replace("\n", ""), description, paidBy, debtCredit, amountDueStr);
        }
    }
}
