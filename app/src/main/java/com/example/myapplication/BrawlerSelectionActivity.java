package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BrawlerSelectionActivity extends AppCompatActivity {

    private int totalTrophies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brawler_selection);

        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        totalTrophies = prefs.getInt("total_trophies", 0);

        findViewById(R.id.btnBrawler0).setOnClickListener(v -> selectBrawler(0));
        findViewById(R.id.btnBrawler1).setOnClickListener(v -> selectBrawler(1));
        findViewById(R.id.btnBrawler2).setOnClickListener(v -> selectBrawler(2));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void selectBrawler(int id) {
        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        
        boolean isUnlocked = (id == 0) || (id == 1 && totalTrophies >= 100) || (id == 2 && totalTrophies >= 300);

        if (isUnlocked) {
            prefs.edit().putInt("selected_brawler", id).apply();
            Toast.makeText(this, R.string.brawler_selected, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.collect_trophies_unlock, Toast.LENGTH_SHORT).show();
        }
    }
}
