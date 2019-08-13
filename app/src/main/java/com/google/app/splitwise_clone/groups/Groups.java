package com.google.app.splitwise_clone.groups;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class Groups extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private String TAG = Groups.class.getSimpleName();
    private GroupsAdapter mGroupsAdapter;
    private RecyclerView groups_rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);


        getSupportActionBar().setTitle("");
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        groups_rv = (RecyclerView) findViewById(R.id.groups_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        groups_rv.setLayoutManager(layoutManager);
        Query query = mDatabaseReference.child("groups");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<DataSnapshot> dataSnapshotList = new ArrayList<>();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        dataSnapshotList.add(i);
                    }

                    mGroupsAdapter = new GroupsAdapter(dataSnapshotList, Groups.this);
                    groups_rv.setAdapter(mGroupsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.group_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gotoAddGroup:
                Intent intent = new Intent(Groups.this, AddGroup.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
