package com.nmelihsensoy.beorc;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private PeerConnWaiter chatPeer = null;
    private PeerMessageWaiter chatReceiver = null;
    private RecyclerView recyclerView = null;
    private BluetoothAdapter devAdapter = null;
    private Toolbar toolbar1 = null;
    private EditText messageBox = null;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
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

        devAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!devAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }

        while(!devAdapter.isEnabled()){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        setContentView(R.layout.activity_main);
        initToolbar();
        setConnectedDeviceTitle();
        initMessageList();
        initMessageBox();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(availableSocket == null){
            chatPeer = new PeerConnWaiter();
            chatPeer.start();
        }else if(!availableSocket.isConnected()){
            chatPeer = new PeerConnWaiter();
            chatPeer.start();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        registerReceiver(nativeDevicePickerReceiver, filter);
    }

    private void initToolbar(){
        toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar1);
    }

    private void initMessageBox(){
        messageBox = findViewById(R.id.msgText);
        messageBox.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE){
                sendMessage(textView.getText().toString());
                textView.setText("");
                return true;
            }
            return false;
        });
    }

    private void initMessageList(){
        recyclerView = findViewById(R.id.message_list);
        messageStoreList = new ArrayList<>();
        messageAdapter = new CustomAdapter(messageStoreList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
    }

    private void clearMessageList(){
        messageStoreList.clear();
        messageAdapter.notifyDataSetChanged();
    }

    private void scrollMessageListToLatest(){
        if(recyclerView != null){
            recyclerView.scrollToPosition(messageStoreList.size() - 1);
        }
    }

    private void setConnectedDeviceTitle(String deviceName){
        if(toolbar1 != null){
            toolbar1.setSubtitle("connected: "+deviceName);
        }
    }

    private void setConnectedDeviceTitle(){
        if(toolbar1 != null){
            toolbar1.setSubtitle("not connected");
        }
    }

    private void setPairingDeviceTitle(){
        if(toolbar1 != null){
            setSupportActionBar(toolbar1);
            toolbar1.setSubtitle("pairing...");
        }
    }

    private void newMessage(MessageData msg){
        if(!msg.getMessage().isEmpty()){
            messageStoreList.add(msg);
            messageAdapter.notifyDataSetChanged();
        }
    }

    private void enableMessageBox(){
        messageBox.setEnabled(true);
    }

    private void disableMessageBox(){
        messageBox.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        topBarMenu = menu;
        setItemEnabledByConnection(menu.getItem(2), true);
        setItemEnabledByConnection(menu.getItem(0), false);
        return true;
    }

    private void setItemEnabledByConnection(MenuItem item, boolean status){
        if(availableSocket != null && availableSocket.isConnected()){
            //connected
            item.setEnabled(status);
        }else{
            item.setEnabled(!status);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.menu_connect) {
            Log.d(TAG, "Menu Connect");
            openNativeDevicePicker();
            return true;
        }
        if(id == R.id.menu_discoverable) {
            Log.d(TAG, "Menu Discoverable");
            makeDiscoverable();
            return true;
        }
        if(id == R.id.menu_disconnect) {
            Log.d(TAG, "Menu Disconnect");
            disconnectPeer();
            return true;
        }
        if(id == R.id.menu_about){
            Log.d(TAG, "Menu About");
            openAboutPage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.availableSocket.close();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
        unregisterReceiver(nativeDevicePickerReceiver);
    }

    @SuppressLint("MissingPermission")
    private void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(discoverableIntent);
    }

    private void openNativeDevicePicker(){
        Intent devicePickerIntent = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
        startActivity(devicePickerIntent);
    }

    private void openAboutPage(){
        Intent switchActivityIntent = new Intent(this, AboutActivity.class);
        startActivity(switchActivityIntent);
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

    private void newMessageUi(MessageData msg){
        runOnUiThread(() -> {
            this.newMessage(msg);
            this.scrollMessageListToLatest();
        });
    }

    private void sendMessage(String messageLine){
        if(!messageLine.isEmpty()){
            Log.d(TAG, messageLine);
            newMessage(new MessageData(messageLine, MessageData.TYPE_RIGHT_BUBBLE));
            scrollMessageListToLatest();
            writeToSocket(messageLine.getBytes());
        }
    }

    private void disconnectPeer(){
        if (availableSocket != null){
            if(this.availableSocket.isConnected()){
                try {
                    this.availableSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private void writeToSocket(byte[] value){
        OutputStream tmpOut = null;
        try {
            tmpOut = availableSocket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            tmpOut.write(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        chatReceiver = new PeerMessageWaiter();
        chatReceiver.start();
    }

    private final BroadcastReceiver nativeDevicePickerReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "Pick Device: " + dev.getName() + "\n" + dev.getAddress());
                devAdapter.cancelDiscovery();

                if(dev.getBondState() == BluetoothDevice.BOND_NONE){
                    setPairingDeviceTitle();
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(devicePairReceiver, filter);
                    dev.createBond();
                }else{
                    connectPeer(dev);
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
                    unregisterReceiver(devicePairReceiver);
                    connectPeer(mDevice);
                }
            }
        }
    };

    private class PeerConnWaiter extends Thread {
        @SuppressLint("MissingPermission")
        public void run() {

            BluetoothServerSocket tmp;
            try {
                tmp = devAdapter.listenUsingInsecureRfcommWithServiceRecord(
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
                    try {
                        // close BluetoothServerSocket
                        tmp.close();
                    } catch (IOException e) {

                    }
                    Log.e(TAG, "Socket accepted "+ availableSocket.getRemoteDevice().getName());
                    connectionState = 1;
                    setUiConnected(availableSocket.getRemoteDevice().getName());
                    chatReceiver = new PeerMessageWaiter();
                    chatReceiver.start();
                }
            }
        }
    }

    private class PeerMessageWaiter extends Thread {
        @SuppressLint("MissingPermission")
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream tmpIn = null;

            try {
                tmpIn = availableSocket.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                while((bytes = tmpIn.read(buffer))!=-1) {
                    String readMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "RECEIVED: " + readMessage);
                    newMessageUi(new MessageData(readMessage, MessageData.TYPE_LEFT_BUBBLE));
                }
            } catch (IOException e) {
                disconnectPeer();
                setUiDisconnected();
            }
        }
    }
}
