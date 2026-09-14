package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ShopActivity extends AppCompatActivity {

    private int totalCoins, totalGems;
    private int hpLevel, damageLevel, speedLevel;
    private TextView tvCoins, tvGems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        totalCoins = prefs.getInt("total_coins", 0);
        totalGems = prefs.getInt("total_gems", 0);
        hpLevel = prefs.getInt("hp_level", 0);
        damageLevel = prefs.getInt("damage_level", 0);
        speedLevel = prefs.getInt("speed_level", 0);

        tvCoins = findViewById(R.id.tvCoins);
        tvGems = findViewById(R.id.tvGems);
        updateUI();

        findViewById(R.id.btnBuyHp).setOnClickListener(v -> buyUpgrade("hp", 50));
        findViewById(R.id.btnBuyDamage).setOnClickListener(v -> buyUpgrade("damage", 100));
        findViewById(R.id.btnBuySpeed).setOnClickListener(v -> buyUpgrade("speed", 75));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void buyUpgrade(String type, int cost) {
        if (totalCoins >= cost) {
            totalCoins -= cost;
            SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("total_coins", totalCoins);
            
            switch (type) {
                case "hp": editor.putInt("hp_level", ++hpLevel); break;
                case "damage": editor.putInt("damage_level", ++damageLevel); break;
                case "speed": editor.putInt("speed_level", ++speedLevel); break;
            }
            
            editor.apply();
            updateUI();
            Toast.makeText(this, getString(R.string.upgrade_purchased), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.not_enough_coins), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        tvCoins.setText(getString(R.string.coins_label, totalCoins));
        tvGems.setText(getString(R.string.gems_label, totalGems));
    }
}
