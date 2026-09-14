package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class LobbyActivity extends AppCompatActivity {

    private Button btnStartMatch, btnSelectMode, btnStarPower;
    private TextView tvStatus, tvTrophies, tvGems;
    private boolean isSearching = false;
    private final Random random = new Random();
    private String selectedMode = "SOLO";
    private int selectedStarPower = 0; // 0: None, 1: Bush Healer, 2: Speed, 3: Strong Hitter

    private final ActivityResultLauncher<Intent> modeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedMode = result.getData().getStringExtra("selected_mode");
                    updateModeButton();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        btnStartMatch = findViewById(R.id.btnStartMatch);
        btnSelectMode = findViewById(R.id.btnSelectMode);
        btnStarPower = findViewById(R.id.btnStarPower);
        tvStatus = findViewById(R.id.tvStatus);
        tvTrophies = findViewById(R.id.tvTrophies);
        tvGems = findViewById(R.id.tvGems);

        loadStats();

        btnStartMatch.setOnClickListener(v -> {
            if (!isSearching) {
                startMatchmaking();
            }
        });

        findViewById(R.id.btnShop).setOnClickListener(v -> 
                startActivity(new Intent(this, ShopActivity.class)));
        
        findViewById(R.id.btnSelectBrawler).setOnClickListener(v -> 
                startActivity(new Intent(this, BrawlerSelectionActivity.class)));

        findViewById(R.id.btnTrophyRoad).setOnClickListener(v -> 
                startActivity(new Intent(this, TrophyRoadActivity.class)));

        findViewById(R.id.btnBrawlBox).setOnClickListener(v -> openBrawlBox());

        btnSelectMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ModeSelectionActivity.class);
            modeLauncher.launch(intent);
        });

        btnStarPower.setOnClickListener(v -> toggleStarPower());
        updateModeButton();
    }

    private void updateModeButton() {
        String display;
        switch (selectedMode) {
            case "CLASSIC": display = getString(R.string.mode_classic); break;
            case "DUO": display = getString(R.string.mode_duo); break;
            case "3V3": display = getString(R.string.mode_3v3); break;
            case "5V5": display = getString(R.string.mode_5v5); break;
            case "BOSS": display = getString(R.string.mode_boss); break;
            default: display = getString(R.string.mode_solo); break;
        }
        btnSelectMode.setText(display);
    }

    private void toggleStarPower() {
        selectedStarPower = (selectedStarPower + 1) % 4;
        String name = getStarPowerName(selectedStarPower);
        
        String display = selectedStarPower == 0 ? "⭐" : "⭐\n" + name;
        btnStarPower.setText(display);
        getSharedPreferences("BrawlPrefs", MODE_PRIVATE)
                .edit()
                .putInt("selected_star_power", selectedStarPower)
                .apply();
        Toast.makeText(this, "Star Power: " + name, Toast.LENGTH_SHORT).show();
    }

    private String getStarPowerName(int id) {
        switch (id) {
            case 1: return "Bush Healer";
            case 2: return "Speedy";
            case 3: return "Hard Hitter";
            default: return "None";
        }
    }

    private void openBrawlBox() {
        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        int currentCoins = prefs.getInt("total_coins", 0);
        
        if (currentCoins >= 100) {
            int coinsAfterPay = currentCoins - 100;
            int gemsWon = random.nextInt(5) + 1;
            int coinsWon = random.nextInt(50) + 20;
            
            prefs.edit()
                .putInt("total_gems", prefs.getInt("total_gems", 0) + gemsWon)
                .putInt("total_coins", coinsAfterPay + coinsWon)
                .apply();
                
            loadStats();
            String msg = getString(R.string.box_opened_message, coinsWon, gemsWon);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.need_coins_message), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        isSearching = false;
        btnStartMatch.setEnabled(true);
        tvStatus.setText(R.string.ready_for_battle);
    }

    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        int trophies = prefs.getInt("total_trophies", 0);
        int gems = prefs.getInt("total_gems", 0);
        selectedStarPower = prefs.getInt("selected_star_power", 0);
        
        tvTrophies.setText(getString(R.string.trophies_display, trophies));
        tvGems.setText(getString(R.string.gems_display, gems));
        
        String name = getStarPowerName(selectedStarPower);
        String display = selectedStarPower == 0 ? "⭐" : "⭐\n" + name;
        btnStarPower.setText(display);
    }

    private void startMatchmaking() {
        isSearching = true;
        btnStartMatch.setEnabled(false);
        tvStatus.setText(getString(R.string.searching_players, 1));

        new CountDownTimer(2000, 200) {
            int playersFound = 1;

            @Override
            public void onTick(long millisUntilFinished) {
                playersFound++;
                if (playersFound <= 10) {
                    tvStatus.setText(getString(R.string.searching_players, playersFound));
                }
            }

            @Override
            public void onFinish() {
                tvStatus.setText(getString(R.string.brawl_start));
                Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
                intent.putExtra("game_mode", selectedMode);
                startActivity(intent);
            }
        }.start();
    }
}
