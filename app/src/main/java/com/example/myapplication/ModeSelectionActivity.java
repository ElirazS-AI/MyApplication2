package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ModeSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selection);

        findViewById(R.id.modeSolo).setOnClickListener(v -> selectMode("SOLO"));
        findViewById(R.id.modeClassic).setOnClickListener(v -> selectMode("CLASSIC"));
        findViewById(R.id.modeDuo).setOnClickListener(v -> selectMode("DUO"));
        findViewById(R.id.mode3v3).setOnClickListener(v -> selectMode("3V3"));
        findViewById(R.id.mode5v5).setOnClickListener(v -> selectMode("5V5"));
        findViewById(R.id.modeBoss).setOnClickListener(v -> selectMode("BOSS"));
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void selectMode(String mode) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_mode", mode);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
