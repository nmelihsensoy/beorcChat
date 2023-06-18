package com.nmelihsensoy.beorc;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BeorcDEBUG";
    private static final UUID BEORC_UUID = UUID.fromString("A512FD83-E493-4D41-BB12-EF747B192056");
    private static final String BEORC_NAME = "BEORC";
    Menu topBarMenu;
    private BluetoothSocket availableSocket;
    private int connectionState = -1;
    private CustomAdapter messageAdapter;
    private ArrayList<MessageData> messageStoreList;
    private PeerListener chatPeer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAllRuntimePerms();
        setContentView(R.layout.activity_main);
        setConnectedDeviceTitle();
        initMessageList();
        initMessageBox();
    }

    private void initMessageBox(){
        EditText editText = findViewById(R.id.msgText);
        editText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE){
                sendMessage(textView.getText().toString());
                textView.setText("");
                return true;
            }
            return false;
        });
    }

    private void initMessageList(){
        RecyclerView recyclerView = findViewById(R.id.message_list);
        messageStoreList = new ArrayList<>();
        messageAdapter = new CustomAdapter(messageStoreList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
    }

    private void clearMessageList(){
        messageStoreList.clear();
        messageAdapter.notifyDataSetChanged();
    }

    private void setConnectedDeviceTitle(String deviceName){
        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar1);
        toolbar1.setSubtitle("connected: "+deviceName);
    }

    private void setConnectedDeviceTitle(){
        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar1);
        toolbar1.setSubtitle("not connected");
    }

    private void newMessage(MessageData msg){
        if(!msg.getMessage().isEmpty()){
            messageStoreList.add(msg);
            messageAdapter.notifyDataSetChanged();
        }
    }

    private void enableMessageBox(){
        EditText messageBox = (EditText) findViewById(R.id.msgText);
        messageBox.setEnabled(true);
    }

    private void disableMessageBox(){
        EditText messageBox = (EditText) findViewById(R.id.msgText);
        messageBox.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        topBarMenu = menu;
        setDisconnectMenuEnabled(menu.getItem(2));
        return true;
    }

    private void setDisconnectMenuEnabled(MenuItem item){
        if(availableSocket != null && availableSocket.isConnected()){
            item.setEnabled(true);
        }else{
            item.setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_connect) {
            Log.d(TAG, "Menu Connect");
            openNativeDevicePicker();
            return true;
        }
        if (id == R.id.menu_discoverable) {
            Log.d(TAG, "Menu Discoverable");
            makeDiscoverable();
            return true;
        }
        if (id == R.id.menu_disconnect) {
            Log.d(TAG, "Menu Disconnect");
            disconnectPeer();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(availableSocket == null){
            chatPeer = new PeerListener();
            chatPeer.start();
        }else if(availableSocket != null && !availableSocket.isConnected()){
            chatPeer = new PeerListener();
            chatPeer.start();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        enableBt();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(availableSocket != null){
            try {
                availableSocket.close();
                chatPeer = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
            (ActivityCompat.checkSelfPermission(MainActivity.this, android. Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager. PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(MainActivity.this, android. Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager. PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(MainActivity.this, android. Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager. PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(MainActivity.this, android. Manifest.permission.BLUETOOTH_CONNECT) != PackageManager. PERMISSION_GRANTED)
        ){
            if (android.os.Build.VERSION.SDK_INT > 31){
                Toast.makeText(this, "Accept Permissions!", Toast.LENGTH_LONG).show();
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

    private void openNativeDevicePicker(){
        IntentFilter filter = new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        registerReceiver(nativeDevicePickerReceiver, filter);

        Intent devicePickerIntent = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
        startActivity(devicePickerIntent);
    }

    private void setUiConnected(String devName){
        runOnUiThread(() -> {
            MainActivity.this.invalidateOptionsMenu();
            setConnectedDeviceTitle(devName);
            enableMessageBox();
        });
    }

    private void setUiDisconnected(){
        runOnUiThread(() -> {
            MainActivity.this.invalidateOptionsMenu();
            setConnectedDeviceTitle();
            disableMessageBox();
            clearMessageList();
        });
    }

    private void sendMessage(String messageLine){
        if(!messageLine.isEmpty()){
            Log.d(TAG, messageLine);
        }
    }

    private void disconnectPeer(){
        if(availableSocket != null){
            try {
                availableSocket.close();
                chatPeer = null;
                setUiDisconnected();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        chatPeer = new PeerListener();
        chatPeer.start();
    }

    @SuppressLint("MissingPermission")
    private void connectPeer(BluetoothDevice device){
        BluetoothSocket tmp = null;

        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(BEORC_UUID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            tmp.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        availableSocket = tmp;
        setUiConnected(device.getName());
    }

    private final BroadcastReceiver nativeDevicePickerReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "Pick Device: " + dev.getName() + "\n" + dev.getAddress());

                if(dev.getBondState() == BluetoothDevice.BOND_NONE){
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(devicePairReceiver, filter);
                    dev.createBond();
                    unregisterReceiver(devicePairReceiver);
                }else{
                    connectPeer(dev);
                    unregisterReceiver(nativeDevicePickerReceiver);
                }
            }
        }
    };

    private final BroadcastReceiver devicePairReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    connectPeer(mDevice);
                    unregisterReceiver(devicePairReceiver);
                }
            }
        }
    };

    private class PeerListener extends Thread {
        @SuppressLint("MissingPermission")
        public void run() {

            BluetoothServerSocket tmp;
            try {
                tmp = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(
                        BEORC_NAME, BEORC_UUID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (connectionState != 1) {
                try {
                    availableSocket = tmp.accept();
                    Log.d(TAG, "Socket: "+ availableSocket.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + "accept() failed", e);
                    break;
                }

                if (availableSocket != null) {
                    Log.e(TAG, "Socket accepted "+ availableSocket.getRemoteDevice().getName());

                    connectionState = 1;
                    setUiConnected(availableSocket.getRemoteDevice().getName());
                }
            }
        }
    }

}