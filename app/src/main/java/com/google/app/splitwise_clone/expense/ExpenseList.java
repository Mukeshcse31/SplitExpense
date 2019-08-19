package com.google.app.splitwise_clone.expense;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.groups.Groups;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.DatePickerFragment;
import com.google.app.splitwise_clone.utils.ExpenseAdapter;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpenseList extends AppCompatActivity implements ExpenseAdapter.OnClickListener{

    private DatabaseReference mDatabaseReference;
    private String TAG = ExpenseList.class.getSimpleName();

    private ExpenseAdapter mExpenseAdapter;
    private RecyclerView expenses_rv;
    private FloatingActionButton mFloatingActionButton;

    ListView listView;
    Spinner spinner2;
    private static String[] groupMembers;
    private AutoCompleteTextView mDescription, mAmount;
    private Button date_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        expenses_rv = (RecyclerView) findViewById(R.id.expenses_rv);
        mFloatingActionButton = findViewById(R.id.add_expense_fab);

        Intent intent = getIntent();
        if(intent.hasExtra("group_name")){
            String group_name = intent.getStringExtra("group_name");
            Toast.makeText(this, "Expense list - " + group_name, Toast.LENGTH_LONG).show();

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            expenses_rv.setLayoutManager(layoutManager);
            Query query = mDatabaseReference.child("groups/" + group_name + "/expenses");

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<DataSnapshot> dataSnapshotList = new ArrayList<>();
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            dataSnapshotList.add(i);
                        }

                        mExpenseAdapter = new ExpenseAdapter(dataSnapshotList, ExpenseList.this);
                        expenses_rv.setAdapter(mExpenseAdapter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ExpenseList.this, AddExpense.class);
                    startActivity(intent);
                }
            });

        }

    }

    @Override
    public void gotoExpenseDetails(String name) {

    }
}