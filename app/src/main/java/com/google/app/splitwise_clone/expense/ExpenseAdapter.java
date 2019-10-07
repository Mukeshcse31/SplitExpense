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
package com.google.app.splitwise_clone.expense;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private static final String TAG = ExpenseAdapter.class.getSimpleName();
    public static String expensePayload = "";
    String userName;
    LinkedHashMap<String, Expense> mExpenseMap;
    List<String> expList;
    static Iterator it;
    Context mContext;
    Expense expense;
    String expenseId;
    Boolean enabled;

    private OnClickListener mOnClickListener;

    public ExpenseAdapter(LinkedHashMap<String, Expense> map, OnClickListener listener, boolean enabled) {

        mExpenseMap = map;
        expList = new ArrayList<>(mExpenseMap.keySet());
        mOnClickListener = listener;
        userName = FirebaseUtils.getUserName();
        this.enabled = enabled;
        expensePayload = "";
    }

    public interface OnClickListener {
        void gotoExpenseDetails(String expenseId);
    }

    @Override
    public ExpenseViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        mContext = viewGroup.getContext();
        int layoutIdForListItem = R.layout.expense_list_item;
        if (viewType == 0) layoutIdForListItem = R.layout.expense_list_item_category;

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(layoutIdForListItem, viewGroup, false);
        if (enabled == false) {
            view.setAlpha(0.5f);
            view.setEnabled(false);
        }
        ExpenseViewHolder viewHolder = new ExpenseViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.bind(position);
    }

    @Override
    public int getItemViewType(int pos) {
//        https://stackoverflow.com/questions/5300962/getviewtypecount-and-getitemviewtype-methods-of-arrayadapter

//        if (pos == 0)
//            it = mExpenseMap.entrySet().iterator();
//
//        Map.Entry pair = (Map.Entry) it.next();

//        expense = (Expense) pair.getValue();
//        expenseId = (String) pair.getKey();

        expenseId = expList.get(pos);
        expense = mExpenseMap.get(expenseId);

        if (expense == null)
            return 0;
        else
            return 1;
    }

    @Override
    public int getItemCount() {

        return mExpenseMap.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the other, ie 0 through getItemCount() - 1
        TextView dateSpent_tv, tv_expenseDescription, tv_debt_credit, tv_amount, tv_paidBy, category_tv;
        ImageView expense_img;

        public ExpenseViewHolder(View itemView) {
            super(itemView);

            if (expense == null) {
                category_tv = itemView.findViewById(R.id.category_tv);
                return;
            }
            expense_img = itemView.findViewById(R.id.expense_img);
            dateSpent_tv = itemView.findViewById(R.id.dateSpent_tv);
            tv_expenseDescription = itemView.findViewById(R.id.tv_expenseDescription);
            tv_paidBy = itemView.findViewById(R.id.tv_paidBy);

            tv_debt_credit = itemView.findViewById(R.id.tv_debt_credit);
            tv_amount = itemView.findViewById(R.id.tv_amount);

        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a other item.
         *
         * @param listIndex Position of the item in the other
         */
        void bind(final int listIndex) {

            if (expense == null) {
                category_tv.setText(expenseId.toUpperCase());
                return;
            }
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

            if (splitExpense.containsKey(userName) || TextUtils.equals(payer, mContext.getString(R.string.you))) {
                SingleBalance sb = splitExpense.get(userName);

                float amountDue;
                if (sb == null) { //if the user has paid and not sharing the expense
                    amountDue = totalAmount;
                } else amountDue = sb.getAmount();

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
            else{//if the user has not participated
                tv_debt_credit.setTextColor(mContext.getColor(R.color.black));
                tv_amount.setTextColor(mContext.getColor(R.color.black));
            }

            //click event handler to edit
            final String expense_id = expenseId;
            tv_expenseDescription.getRootView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.gotoExpenseDetails(expense_id);
                }
            });

            //update the payload
            expensePayload += String.format("\n%s,%s,%s,%s,%s",
                    dateString.replace("\n", ""), description, paidBy, debtCredit, amountDueStr);
        }
    }
}
