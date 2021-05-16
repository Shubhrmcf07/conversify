package com.example.coversify.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.coversify.Fragments.CallFragment;
import com.example.coversify.Fragments.ChatFragment;
import com.example.coversify.Fragments.StatusFragment;
import com.example.coversify.R;
import com.example.coversify.Models.User;
import com.example.coversify.Adaptors.UsersAdaptor;
import com.example.coversify.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {



    private Toolbar toolbar;
    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdaptor usersAdaptor;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        BottomNavigationView view = binding.navBar;
        view.setOnNavigationItemSelectedListener(itemSelected);
        view.setItemIconTintList(null);
        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, new ChatFragment()).commit();

        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    BottomNavigationView.OnNavigationItemSelectedListener itemSelected = new BottomNavigationView.OnNavigationItemSelectedListener(){
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment = null;
            switch (item.getItemId()){
                case R.id.chats:
                    fragment = new ChatFragment();
                    break;

                case R.id.status_row:
                    fragment = new StatusFragment();
                    break;

                case R.id.call :
                    fragment = new CallFragment();
                    break;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout, fragment).commit();
            return true;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        String userId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("status").child(userId).setValue("Online");
    }

    @Override
    protected void onPause() {

        String userId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("status").child(userId).setValue("Offline");
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String userId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("status").child(userId).setValue("Offline");
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings:
                Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show();
                break;

            case R.id.search:
                Toast.makeText(this, "Search Clicked", Toast.LENGTH_SHORT).show();
                break;

            case  R.id.groups:
                startActivity(new Intent(MainActivity.this, GroupChatActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }


}