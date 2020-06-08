package com.example.amplifyapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class Counter extends AppCompatActivity {

    private SharedPreferences mPrefs;
    private static final String PREFS_NAME="PrefsFile";
    TextView t1,t2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter);
        mPrefs=getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        t1=findViewById(R.id.counterAll);
        t2=findViewById(R.id.counterPlain);
        t1.setText(mPrefs.getString("count_ALL",  "0"));
        t2.setText(mPrefs.getString("count_PLAIN",  "0"));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
