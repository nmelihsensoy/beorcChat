package com.nmelihsensoy.beorc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView githubLink = findViewById(R.id.about_dev_profile);
        Linkify.addLinks(githubLink, Linkify.WEB_URLS);

        ConstraintLayout background = (ConstraintLayout) findViewById(R.id.about_wrapper);
        background.setOnClickListener(view -> finish());
    }
}