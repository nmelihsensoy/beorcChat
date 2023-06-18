package com.nmelihsensoy.beorc;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BeorcDEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar1);
        toolbar1.setSubtitle("not connected");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_connect) {

            Log.d(TAG, "Menu Connect");
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

}