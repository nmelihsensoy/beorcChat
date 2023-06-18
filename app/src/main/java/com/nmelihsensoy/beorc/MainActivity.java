package com.nmelihsensoy.beorc;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BeorcDEBUG";
    private static final UUID BEORC_UUID = UUID.fromString("A512FD83-E493-4D41-BB12-EF747B192056");
    Menu topBarMenu;
    private BluetoothSocket availableSocket;
    private int connectionState = -1;
    private CustomAdapter messageAdapter;
    private ArrayList<MessageData> messageStoreList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAllRuntimePerms();
        setContentView(R.layout.activity_main);

        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar1);
        toolbar1.setSubtitle("not connected");
        initMessageList();
        //new MessageData("hello pong", 1)
    }

    private void initMessageList(){
        RecyclerView recyclerView = findViewById(R.id.message_list);
        messageStoreList = new ArrayList<>();
        messageAdapter = new CustomAdapter(messageStoreList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
    }

    private void newMessage(MessageData msg){
        if(!msg.getMessage().isEmpty()){
            messageStoreList.add(msg);
            messageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        topBarMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_connect) {
            Log.d(TAG, "Menu Connect");
            MenuItem item1 = topBarMenu.getItem(2);
            item1.setEnabled(false);
            return true;
        }
        if (id == R.id.menu_discoverable) {
            Log.d(TAG, "Menu Discoverable");
            makeDiscoverable();
            return true;
        }
        if (id == R.id.menu_disconnect) {
            Log.d(TAG, "Menu Disconnect");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        enableBt();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void getAllRuntimePerms(){

        ActivityCompat.requestPermissions(MainActivity.this, new String[]
            {
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);

        if(
            (ActivityCompat. checkSelfPermission(MainActivity.this, android. Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager. PERMISSION_GRANTED) ||
            (ActivityCompat. checkSelfPermission(MainActivity.this, android. Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager. PERMISSION_GRANTED) ||
            (ActivityCompat. checkSelfPermission(MainActivity.this, android. Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager. PERMISSION_GRANTED) ||
            (ActivityCompat. checkSelfPermission(MainActivity.this, android. Manifest.permission.BLUETOOTH_CONNECT) != PackageManager. PERMISSION_GRANTED)
        ){
            Toast.makeText(this, "Accept Permissions!", Toast.LENGTH_LONG).show();
            if (android.os.Build.VERSION.SDK_INT > 31){
                MainActivity.this.finish();
                System.exit(0);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(discoverableIntent);
    }

    @SuppressLint("MissingPermission")
    private void enableBt(){
        if(BluetoothAdapter.getDefaultAdapter() == null){
            MainActivity.this.finish();
            System.exit(0);
        }

        if(BluetoothAdapter.getDefaultAdapter().isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
    }

}