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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.IntentCompat;
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
    }

    /* https://stackoverflow.com/a/48758656 */
    public void restartApplication(final @NonNull Activity activity) {
        final PackageManager pm = activity.getPackageManager();
        final Intent intent = pm.getLaunchIntentForPackage(activity.getPackageName());
        activity.finishAffinity();
        activity.startActivity(intent);
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (availableSocket != null){
            if(this.availableSocket.isConnected()){
                try {
                    this.availableSocket.close();
                } catch (IOException e) {

                }
            }
        }
        chatPeer = null;
        connectionState = -1;
        chatReceiver = null;
        Runtime.getRuntime().gc();
    }

    @Override
    protected void onStart() {
        super.onStart();
        enableBt();
        chatPeer = new PeerConnWaiter();
        chatPeer.start();
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

    private void newMessageUi(MessageData msg){
        runOnUiThread(() -> {
            this.newMessage(msg);
            this.scrollMessageListToLatest();
        });
    }

    private void sendMessage(String messageLine){
        if(!messageLine.isEmpty()){
            Log.d(TAG, messageLine);
            newMessage(new MessageData(messageLine, 2));
            scrollMessageListToLatest();
            writeToSocket(messageLine.getBytes());
        }
    }

    private void disconnectPeer(){
        runOnUiThread(() -> {
            if (availableSocket != null){
                if(this.availableSocket.isConnected()){
                    try {
                        this.availableSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            restartApplication(MainActivity.this);
        });
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

    private class PeerConnWaiter extends Thread {
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
                    newMessageUi(new MessageData(readMessage, 1));
                }
            } catch (IOException e) {
                disconnectPeer();
            }
        }
    }
}
