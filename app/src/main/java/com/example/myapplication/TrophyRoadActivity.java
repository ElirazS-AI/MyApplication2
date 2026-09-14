package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TrophyRoadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trophy_road);

        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        int trophies = prefs.getInt("total_trophies", 0);

        TextView tvTrophyProgress = findViewById(R.id.tvTrophyProgress);
        tvTrophyProgress.setText(getString(R.string.your_trophies_label, trophies));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
