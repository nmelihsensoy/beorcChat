package com.nmelihsensoy.beorc;

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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BeorcDEBUG";
    Menu topBarMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAllRuntimePerms();
        setContentView(R.layout.activity_main);

        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar1);
        toolbar1.setSubtitle("not connected");

        testMessageBubble();
    }

    private void testMessageBubble(){
        MessageData[] myList = new MessageData[]{
                new MessageData("hello ping", 1),
                new MessageData("hello pong", 2),
                new MessageData("acknowledgement", 1),
                new MessageData("acknowledgement", 2)
        };

        RecyclerView recyclerView = findViewById(R.id.message_list);
        CustomAdapter adapter = new CustomAdapter(myList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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
            return true;
        }
        if (id == R.id.menu_disconnect) {
            Log.d(TAG, "Menu Disconnect");
            return true;
        }


        return super.onOptionsItemSelected(item);
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

}