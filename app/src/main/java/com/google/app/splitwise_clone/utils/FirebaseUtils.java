package com.google.app.splitwise_clone.utils;

import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.google.app.splitwise_clone.expense.AddExpense;
import com.google.app.splitwise_clone.expense.ExpenseList;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {

    private static String TAG="TOTAL";

    public static String getUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = "";
        if (user != null) {
            displayName = user.getDisplayName();
        }
        return displayName;
    }

public static void updateDB(final String group_name){

    //update the participant's total amount
    final DatabaseReference mDatabaseReference;
    mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    Query query = mDatabaseReference.child("groups/" + group_name + "/members");

    query.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            List<String> members = new ArrayList<>();
            if (dataSnapshot.exists()) {
                for (DataSnapshot i : dataSnapshot.getChildren()) {
                    members.add(i.getKey());
                }
//                groupMembers = members.toArray(new String[0]);
            }
            Query query = mDatabaseReference.child("groups/" + group_name + "/expenses");

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
//                        List<DataSnapshot> expenseSnapshotList = new ArrayList<>();
                        Map<String, Float> amountSpentByMember = new HashMap<>();
                        Map<String, Float> amountDueByMember = new HashMap<>();
                        float totalGroupExpense = 0.2f;
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
//                            expenseSnapshotList.add(i);
                            Expense expense = i.getValue(Expense.class);

//                            String date = expense.getDateSpent();
//                            final String expense_id = i.getKey();
                            String spender = expense.getMemberSpent();
                            Map<String, SingleBalance> splitExpense = expense.getSplitExpense();

                            //amount due by individuals
                            Iterator it = splitExpense.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry pair = (Map.Entry)it.next();
                                String name = (String) pair.getKey();
                                SingleBalance balance = (SingleBalance) pair.getValue();
                                float amount = balance.getAmount();

                                if(amountDueByMember.containsKey(name)){
                                    amountDueByMember.put(name, amountDueByMember.get(name) + amount);
                                }
                                else amountDueByMember.put(name, amount);
                                it.remove(); // avoids a ConcurrentModificationException
                            }

                            //total amount by the group members
                            if(amountSpentByMember.containsKey(spender))
                                amountSpentByMember.put(spender, amountSpentByMember.get(spender) + expense.getTotal());
                            else
                                amountSpentByMember.put(spender, expense.getTotal());

                            //total expense in the group
                            totalGroupExpense += expense.getTotal();
                        }
                        Log.i(TAG, "" + amountSpentByMember.size());
                        Log.i(TAG, " group expense " + totalGroupExpense);
                        Log.i(TAG, "individual amount due " + amountDueByMember.size());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    });


}

}
