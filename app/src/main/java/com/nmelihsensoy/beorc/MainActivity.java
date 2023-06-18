package com.nmelihsensoy.beorc;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BeorcDEBUG";
    Menu topBarMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

}