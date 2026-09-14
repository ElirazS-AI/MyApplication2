package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private FrameLayout gameArena;
    private ImageView imgPlayer;
    private Button btnUlti;
    private ProgressBar pbPlayerHp;
    private View ammo1, ammo2, ammo3;
    private SafeZoneView safeZoneView;
    private TextView tvScore, tvGameMessage;
    private LinearLayout killFeedContainer;

    private int playerHp;
    private int playerMaxHp = 100;
    private int ammo = 3;
    private float ultiCharge = 0f;
    private int matchKills = 0; 
    
    // Game Mode Settings
    private String currentGameMode = "SOLO";
    private final int playerTeam = 0;
    private boolean isGasEnabled = true;
    private boolean isBossMode = false;
    private int activeStarPower = 0;

    private boolean isSuperShieldActive = false;

    // Showdown Mechanics
    private float safeZoneRadius = 2000f; 
    private final List<View> crates = new ArrayList<>();
    private final List<View> powerCubes = new ArrayList<>();
    private final List<View> healingZones = new ArrayList<>();
    private final List<View> waterTiles = new ArrayList<>();
    private final List<View> toRemoveList = new ArrayList<>(); 
    private int collectedCubes = 0;
    private boolean isShowdownTriggered = false;

    // Brawl Meta
    private int playersAlive = 10;
    private int playerRank = 10;
    private boolean isGameOver = false;

    private float playerDx = 0, playerDy = 0;
    private float lastAngle = 0;
    private float moveSpeed = 12f;
    private int playerDamage = 1;
    private static final float BULLET_SPEED = 25f;
    
    private float enemySpeedMultiplier = 1.0f;
    private long enemyShootInterval = 2500;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<View> obstacles = new ArrayList<>();
    private final List<Bullet> bulletsToRemove = new ArrayList<>();
    private final List<Enemy> enemiesToRemove = new ArrayList<>();

    private final Handler gameHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private long lastHitTime = 0;

    private static class Enemy {
        ImageView view; ProgressBar hpBar; TextView nameTag;
        int hp, maxHp, team, type;
        float speed; long lastShootTime = 0;
        Enemy(ImageView v, ProgressBar h, TextView n, int ty, float sm, int t) {
            this.view = v; this.hpBar = h; this.nameTag = n; this.type = ty; this.team = t;
            String[] names = {"Shadow", "Spike", "Colt", "Crow", "Leon", "Bull", "Brock", "Shelly"};
            n.setText(names[new Random().nextInt(names.length)]);
            n.setTextColor(team == 0 ? Color.CYAN : Color.WHITE);
            switch (ty) {
                case 1: this.maxHp = 4; this.speed = 5.5f * sm; v.setBackgroundColor(0xFFFB8C00); break;
                case 2: this.maxHp = 15; this.speed = 2.5f * sm; v.setBackgroundColor(0xFF7B1FA2); 
                        v.setLayoutParams(new FrameLayout.LayoutParams(130, 130)); break;
                case 3:
                    this.maxHp = 300;
                    this.speed = 2.2f;
                    v.setBackgroundColor(Color.MAGENTA);
                    n.setText(R.string.raid_boss_label);
                    v.setLayoutParams(new FrameLayout.LayoutParams(300, 300));
                    break;
                default: this.maxHp = 6; this.speed = 4f * sm; v.setBackgroundColor(0xFFEF233C);
            }
            if (team == 0) {
                view.setBackgroundColor(Color.CYAN);
                n.setText(R.string.ally_label);
            }
            this.hp = maxHp; h.setMax(maxHp); h.setProgress(hp);
        }
    }

    private static class Bullet {
        View view; float vx, vy; int team;
        Bullet(View v, float x, float y, int t) { this.view = v; this.vx = x; this.vy = y; this.team = t; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        String mode = getIntent().getStringExtra("game_mode");
        if (mode != null) currentGameMode = mode;
        
        gameArena = findViewById(R.id.gameArena);
        imgPlayer = findViewById(R.id.imgPlayer);
        
        applyModeSettings();
        loadUpgrades();
        
        // Safety Reset
        enemies.clear();
        bullets.clear();
        obstacles.clear();
        crates.clear();
        powerCubes.clear();
        healingZones.clear();
        waterTiles.clear();

        gameArena.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (gameArena.getWidth() > 0 && gameArena.getHeight() > 0) {
                    gameArena.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    
                    imgPlayer.setX(gameArena.getWidth() / 2f);
                    imgPlayer.setY(gameArena.getHeight() / 2f);
                    updateCamera();

                    spawnInitialEntities();
                    generateMap();
                    spawnCrates();
                    spawnHealingZones();
                    
                    startGameLoop();
                    startRegenAndAmmoLoop();
                    startMeteorLoop();
                }
            }
        });

        JoystickView joystick = findViewById(R.id.joystick);
        Button btnShoot = findViewById(R.id.btnShoot);
        btnUlti = findViewById(R.id.btnUlti);
        pbPlayerHp = findViewById(R.id.pbPlayerHp);
        tvScore = findViewById(R.id.tvScore);
        ammo1 = findViewById(R.id.ammo1);
        ammo2 = findViewById(R.id.ammo2);
        ammo3 = findViewById(R.id.ammo3);
        safeZoneView = findViewById(R.id.safeZoneView);
        tvGameMessage = findViewById(R.id.tvGameMessage);
        killFeedContainer = findViewById(R.id.killFeedContainer);

        pbPlayerHp.setMax(playerMaxHp);
        playerHp = playerMaxHp;
        pbPlayerHp.setProgress(playerHp);
        tvScore.setText(getString(R.string.players_left_label, playersAlive));

        joystick.setOnMoveListener((angle, strength) -> {
            if (strength > 0) {
                double rad = Math.toRadians(angle);
                playerDx = (float) (Math.cos(rad) * strength / 100.0 * moveSpeed);
                playerDy = (float) (-Math.sin(rad) * strength / 100.0 * moveSpeed);
                lastAngle = angle; imgPlayer.setRotation(90 - angle);
            } else { playerDx = 0; playerDy = 0; }
        });

        btnShoot.setOnClickListener(v -> {
            if (ammo > 0) { autoAimAndShoot(); ammo--; updateAmmoUI(); }
        });

        btnUlti.setOnClickListener(v -> {
            if (ultiCharge >= 100) { 
                useBrawlerSuper();
                ultiCharge = 0; 
                updateUltiUI(); 
            }
        });
    }

    private void useBrawlerSuper() {
        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        int bId = prefs.getInt("selected_brawler", 0);
        
        switch (bId) {
            case 1:  // Tanker: Shield
                isSuperShieldActive = true;
                imgPlayer.setColorFilter(0x8800BCD4, PorterDuff.Mode.SRC_ATOP);
                showEmote(imgPlayer, "🛡️");
                gameHandler.postDelayed(() -> {
                    isSuperShieldActive = false;
                    imgPlayer.clearColorFilter();
                }, 5000);
                break;
            case 2:  // Sniper: Mega Shot
                spawnMegaBullet(lastAngle);
                showEmote(imgPlayer, "🎯");
                break;
            default:  // Shapey: Meteor Strike
                spawnTargetedMeteor();
                showEmote(imgPlayer, "🔥");
                break;
        }
    }

    private void spawnMegaBullet(float angle) {
        View v = new View(this);
        v.setLayoutParams(new FrameLayout.LayoutParams(60, 60));
        v.setBackgroundColor(Color.YELLOW);
        v.setX(imgPlayer.getX() + 20); v.setY(imgPlayer.getY() + 20);
        
        double rad = Math.toRadians(angle);
        float vx = (float) (Math.cos(rad) * BULLET_SPEED * 1.5f);
        float vy = (float) (-Math.sin(rad) * BULLET_SPEED * 1.5f);
        
        gameArena.addView(v);
        Bullet b = new Bullet(v, vx, vy, playerTeam);
        b.view.setTag("mega"); 
        bullets.add(b);
    }

    private void spawnTargetedMeteor() {
        Enemy nearestTarget = null;
        float minDist = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.team != playerTeam) {
                float d = distSq(imgPlayer, e.view);
                if (d < minDist) {
                    minDist = d;
                    nearestTarget = e;
                }
            }
        }
        
        final Enemy finalTarget = nearestTarget;
        float tx = finalTarget != null ? finalTarget.view.getX() + 45 : imgPlayer.getX() + 500;
        float ty = finalTarget != null ? finalTarget.view.getY() + 45 : imgPlayer.getY();
        
        View warning = new View(this);
        warning.setLayoutParams(new FrameLayout.LayoutParams(300, 300));
        warning.setBackgroundResource(R.drawable.mushroom_style);
        warning.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        warning.setX(tx - 150); warning.setY(ty - 150);
        gameArena.addView(warning);
        
        gameHandler.postDelayed(() -> {
            gameArena.removeView(warning);
            View meteor = new View(this);
            meteor.setLayoutParams(new FrameLayout.LayoutParams(350, 350));
            meteor.setBackgroundColor(Color.YELLOW); meteor.setAlpha(0.7f);
            meteor.setX(tx - 175); meteor.setY(ty - 175);
            gameArena.addView(meteor);
            shakeScreen(60);
            
            Rect mR = new Rect((int)meteor.getX(), (int)meteor.getY(), (int)(meteor.getX()+350), (int)(meteor.getY()+350));
            for (Enemy e : enemies) if (Rect.intersects(mR, getRect(e.view))) { e.hp -= 50; e.hpBar.setProgress(e.hp); }
            gameHandler.postDelayed(() -> gameArena.removeView(meteor), 300);
        }, 1500);
    }

    private void startMeteorLoop() {
        gameHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGameOver && isGasEnabled) {
                    spawnMeteor();
                    gameHandler.postDelayed(this, 15000 + random.nextInt(5000));
                }
            }
        }, 10000);
    }

    private void spawnMeteor() {
        if (gameArena == null || gameArena.getWidth() <= 400) return;
        float x = random.nextInt(gameArena.getWidth() - 400) + 200;
        float y = random.nextInt(gameArena.getHeight() - 400) + 200;
        
        View warning = new View(this);
        warning.setLayoutParams(new FrameLayout.LayoutParams(300, 300));
        warning.setBackgroundResource(R.drawable.mushroom_style); 
        warning.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        warning.setX(x - 150); warning.setY(y - 150);
        gameArena.addView(warning);
        
        gameHandler.postDelayed(() -> {
            gameArena.removeView(warning);
            View meteor = new View(this);
            meteor.setLayoutParams(new FrameLayout.LayoutParams(350, 350));
            meteor.setBackgroundColor(Color.YELLOW);
            meteor.setAlpha(0.7f);
            meteor.setX(x - 175); meteor.setY(y - 175);
            gameArena.addView(meteor);
            shakeScreen(50);
            
            Rect mRect = new Rect((int)meteor.getX(), (int)meteor.getY(), (int)(meteor.getX()+350), (int)(meteor.getY()+350));
            if (Rect.intersects(mRect, getRect(imgPlayer))) {
                playerHp -= 30; pbPlayerHp.setProgress(playerHp);
                if (playerHp <= 0) { playerRank = playersAlive; gameOver(); }
            }
            for (Enemy e : enemies) {
                if (Rect.intersects(mRect, getRect(e.view))) {
                    e.hp -= 15; e.hpBar.setProgress(e.hp);
                }
            }
            gameHandler.postDelayed(() -> gameArena.removeView(meteor), 300);
        }, 2000);
    }

    private void generateMap() {
        String mapStyle = "ARENA";
        if (currentGameMode.equals("SOLO") || currentGameMode.equals("DUO")) 
            mapStyle = random.nextBoolean() ? "FOREST" : "DESERT";
        
        if ("FOREST".equals(mapStyle)) {
            gameArena.setBackgroundColor(0xFF1B4332);
            for (int i = 0; i < 20; i++) spawnObstacle("bush", 150);
            for (int i = 0; i < 5; i++) spawnObstacle("wall", 120);
            for (int i = 0; i < 3; i++) spawnObstacle("water", 200);
            for (int i = 0; i < 2; i++) spawnObstacle("tnt", 100);
        } else if ("DESERT".equals(mapStyle)) {
            gameArena.setBackgroundColor(0xFFBC8F8F);
            for (int i = 0; i < 15; i++) spawnObstacle("wall", 150);
            for (int i = 0; i < 4; i++) spawnObstacle("bush", 100);
            for (int i = 0; i < 4; i++) spawnObstacle("tnt", 100);
        } else {
            gameArena.setBackgroundColor(0xFF1B1D2A);
            for (int i = 0; i < 12; i++) spawnObstacle("wall", 150);
            for (int i = 0; i < 5; i++) spawnObstacle("tnt", 100);
        }
    }

    private void spawnObstacle(String tag, int size) {
        if (gameArena == null || gameArena.getWidth() <= size * 2) return;
        View v = new View(this);
        v.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        
        switch (tag) {
            case "wall":
                v.setBackgroundColor(0xFF4A4E69);
                break;
            case "bush":
                v.setBackgroundColor(0x882D6A4F);
                break;
            case "water":
                v.setBackgroundColor(0xAA0288D1);
                break;
            case "tnt":
                v.setBackgroundColor(Color.RED);
                ProgressBar hp = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                hp.setLayoutParams(new FrameLayout.LayoutParams(size, 10));
                hp.setMax(2);
                hp.setProgress(2);
                hp.setTag(2);
                v.setTag(hp);
                break;
        }
        
        float x = random.nextInt(Math.max(1, gameArena.getWidth() - size*2)) + size;
        float y = random.nextInt(Math.max(1, gameArena.getHeight() - size*2)) + size;
        if (Math.abs(x - 1500) < 300 && Math.abs(y - 1500) < 300) x += 600;
        v.setX(x); v.setY(y); v.setTag(tag);
        gameArena.addView(v);
        if ("water".equals(tag)) {
            waterTiles.add(v);
        } else {
            obstacles.add(v);
        }
    }

    private void applyModeSettings() {
        isGasEnabled = currentGameMode.equals("SOLO") || currentGameMode.equals("DUO");
        isBossMode = "BOSS".equals(currentGameMode);
        playersAlive = isBossMode ? 4 : ("5V5".equals(currentGameMode) ? 10 : ("3V3".equals(currentGameMode) ? 6 : 10));
    }

    private void loadUpgrades() {
        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        int trophies = prefs.getInt("total_trophies", 0);
        activeStarPower = prefs.getInt("selected_star_power", 0);
        enemySpeedMultiplier = 1.0f + (trophies / 2000f);
        enemyShootInterval = Math.max(800, 2500 - trophies);
        
        int bId = prefs.getInt("selected_brawler", 0);
        if (bId == 1) { playerMaxHp = 200; playerDamage = 1; moveSpeed = 8f; imgPlayer.setBackgroundColor(Color.BLUE); }
        else if (bId == 2) { playerMaxHp = 80; playerDamage = 2; moveSpeed = 14f; imgPlayer.setBackgroundColor(Color.RED); }
        else { playerMaxHp = 100; playerDamage = 1; moveSpeed = 12f; imgPlayer.setBackgroundColor(0xFF00B4D8); }
        
        playerMaxHp += prefs.getInt("hp_level", 0) * 20;
        playerDamage += prefs.getInt("damage_level", 0);
        moveSpeed += prefs.getInt("speed_level", 0) * 0.5f;
        
        if (activeStarPower == 2) moveSpeed += 3; 
    }

    private void autoAimAndShoot() {
        Enemy n = null; float mD = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.team == playerTeam) continue;
            float d = distSq(imgPlayer, e.view);
            if (d < mD) { mD = d; n = e; }
        }
        float a = lastAngle;
        if (n != null && mD < 800*800) {
            a = (float) Math.toDegrees(Math.atan2(-(n.view.getY()-imgPlayer.getY()), n.view.getX()-imgPlayer.getX()));
            imgPlayer.setRotation(90 - a);
        }
        
        boolean powerShot = (activeStarPower == 3 && random.nextInt(5) == 0);
        spawnBullet(a);
        if (powerShot) { spawnBullet(a+5); spawnBullet(a-5); }
    }

    private void updateAmmoUI() {
        ammo1.setAlpha(ammo >= 1 ? 1.0f : 0.2f); ammo2.setAlpha(ammo >= 2 ? 1.0f : 0.2f); ammo3.setAlpha(ammo >= 3 ? 1.0f : 0.2f);
    }

    private void startRegenAndAmmoLoop() {
        gameHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ammo < 3) { ammo++; updateAmmoUI(); }
                if (System.currentTimeMillis() - lastHitTime > 3000 && playerHp < playerMaxHp) {
                    int regen = activeStarPower == 1 && imgPlayer.getAlpha() < 1.0f ? 10 : 3;
                    playerHp = Math.min(playerMaxHp, playerHp + regen);
                    pbPlayerHp.setProgress(playerHp);
                }
                gameHandler.postDelayed(this, 1200);
            }
        }, 1200);
    }

    private void spawnInitialEntities() {
        if (isBossMode) { spawnEnemy(3, 1); spawnEnemy(0, 0); spawnEnemy(0, 0); }
        else if ("3V3".equals(currentGameMode)) { for (int i=0; i<2; i++) spawnEnemy(0,0); for (int i=0; i<3; i++) spawnEnemy(0,1); }
        else if ("5V5".equals(currentGameMode)) { for (int i=0; i<4; i++) spawnEnemy(0,0); for (int i=0; i<5; i++) spawnEnemy(0,1); }
        else if ("DUO".equals(currentGameMode)) { spawnEnemy(0,0); for (int i=0; i<8; i++) spawnEnemy(0, (i/2)+1); }
        else { for (int i=0; i<9; i++) spawnEnemy(0, i+1); }
    }

    private void spawnEnemy(int tyO, int team) {
        if (gameArena == null || gameArena.getWidth() <= 300) return;
        int ty = tyO == 0 ? (random.nextInt(10) > 7 ? (random.nextBoolean() ? 1 : 2) : 0) : tyO;
        ImageView ev = new ImageView(this); int s = ty == 3 ? 300 : 90;
        ev.setLayoutParams(new FrameLayout.LayoutParams(s, s)); ev.setImageResource(R.drawable.ic_launcher_foreground);
        float x = random.nextInt(Math.max(1, gameArena.getWidth()-300))+150; float y = random.nextInt(Math.max(1, gameArena.getHeight()-300))+150;
        if (Math.abs(x-imgPlayer.getX()) < 700) x = (x+1200)%(gameArena.getWidth()-200);
        ev.setX(x); ev.setY(y);
        ProgressBar hp = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        hp.setLayoutParams(new FrameLayout.LayoutParams(s, 12));
        TextView nt = new TextView(this); nt.setLayoutParams(new FrameLayout.LayoutParams(s, FrameLayout.LayoutParams.WRAP_CONTENT));
        nt.setGravity(Gravity.CENTER);
        gameArena.addView(ev); gameArena.addView(hp); gameArena.addView(nt);
        enemies.add(new Enemy(ev, hp, nt, ty, enemySpeedMultiplier, team));
    }

    private void spawnCrates() {
        if (gameArena == null || gameArena.getWidth() <= 200) return;
        for (int i = 0; i < 8; i++) {
            View c = new View(this); c.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
            c.setBackgroundResource(R.drawable.crate_style);
            c.setX(random.nextInt(Math.max(1, gameArena.getWidth()-200))+100); c.setY(random.nextInt(Math.max(1, gameArena.getHeight()-200))+100);
            ProgressBar hp = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            hp.setLayoutParams(new FrameLayout.LayoutParams(100, 10)); hp.setMax(4); hp.setProgress(4); hp.setTag(4);
            gameArena.addView(c); gameArena.addView(hp); crates.add(c); c.setTag(hp);
        }
    }

    private void spawnHealingZones() {
        if (gameArena == null || gameArena.getWidth() <= 400) return;
        for (int i = 0; i < 5; i++) {
            View m = new View(this); m.setLayoutParams(new FrameLayout.LayoutParams(250, 250));
            m.setBackgroundResource(R.drawable.mushroom_style);
            m.setX(random.nextInt(Math.max(1, gameArena.getWidth()-400))+200); m.setY(random.nextInt(Math.max(1, gameArena.getHeight()-400))+200);
            gameArena.addView(m); healingZones.add(m);
        }
    }

    private void spawnBullet(float a) {
        View v = new View(this); v.setLayoutParams(new FrameLayout.LayoutParams(20, 20));
        v.setBackgroundColor(Color.WHITE);
        float sX = imgPlayer.getX()+25; float sY = imgPlayer.getY()+25;
        double rad = Math.toRadians(a);
        float vx = (float) (Math.cos(rad) * BULLET_SPEED); float vy = (float) (-Math.sin(rad) * BULLET_SPEED);
        v.setX(sX); v.setY(sY); gameArena.addView(v); bullets.add(new Bullet(v, vx, vy, playerTeam));
    }

    private void startGameLoop() {
        gameHandler.post(new Runnable() {
            @Override
            public void run() { 
                if (!isGameOver) { 
                    try { updateGame(); } catch (Exception e) { /* Robustness */ }
                    gameHandler.postDelayed(this, 20); 
                } 
            }
        });
    }

    private void updateCamera() {
        if (gameArena == null || imgPlayer == null) return;
        float sCX = getResources().getDisplayMetrics().widthPixels / 2f;
        float sCY = getResources().getDisplayMetrics().heightPixels / 2f;
        gameArena.setTranslationX(sCX - imgPlayer.getX() - 25); gameArena.setTranslationY(sCY - imgPlayer.getY() - 25);
    }

    private void updateGame() {
        if (gameArena.getWidth() <= 0) return;
        if (isGasEnabled) {
            if (safeZoneView != null) {
                safeZoneView.setVisibility(View.VISIBLE);
                safeZoneRadius = Math.max(0, safeZoneRadius - 0.25f);
                safeZoneView.updateSafeZone(gameArena.getWidth()/2f, gameArena.getHeight()/2f, safeZoneRadius);
            }
            float dx = imgPlayer.getX()-gameArena.getWidth()/2f; float dy = imgPlayer.getY()-gameArena.getHeight()/2f;
            if (Math.sqrt(dx*dx+dy*dy) > safeZoneRadius) {
                playerHp -= 1; pbPlayerHp.setProgress(playerHp); if (playerHp <= 0) { playerRank = playersAlive; gameOver(); }
            }
        }
        for (View m : healingZones) if (Rect.intersects(getRect(imgPlayer), getRect(m))) {
            if (playerHp < playerMaxHp) { playerHp = Math.min(playerMaxHp, playerHp + 1); pbPlayerHp.setProgress(playerHp); }
        }
        if (isGasEnabled && playersAlive == 2 && !isShowdownTriggered) {
            isShowdownTriggered = true; tvGameMessage.setText(R.string.showdown_text); tvGameMessage.setVisibility(View.VISIBLE);
            gameHandler.postDelayed(() -> tvGameMessage.setVisibility(View.GONE), 3000); shakeScreen(30);
        }

        float nx = imgPlayer.getX() + playerDx; float ny = imgPlayer.getY() + playerDy;
        for (View o : obstacles) if ("wall".equals(o.getTag()) && Rect.intersects(getRect(imgPlayer), getRect(o))) {
            nx = imgPlayer.getX() - playerDx * 1.5f; ny = imgPlayer.getY() - playerDy * 1.5f; break;
        }
        for (View w : waterTiles) if (Rect.intersects(getRect(imgPlayer), getRect(w))) {
            nx = imgPlayer.getX() - playerDx * 2f; ny = imgPlayer.getY() - playerDy * 2f; break;
        }
        if (nx >= 0 && nx <= gameArena.getWidth() - 50) imgPlayer.setX(nx);
        if (ny >= 0 && ny <= gameArena.getHeight() - 50) imgPlayer.setY(ny);
        updateCamera();

        boolean inBush = false;
        for (View o : obstacles) if ("bush".equals(o.getTag()) && Rect.intersects(getRect(imgPlayer), getRect(o))) { inBush = true; break; }
        imgPlayer.setAlpha(inBush ? 0.5f : 1.0f);

        for (View c : powerCubes) if (Rect.intersects(getRect(imgPlayer), getRect(c))) {
            collectedCubes++; playerMaxHp += 400; playerHp = Math.min(playerMaxHp, playerHp+400);
            pbPlayerHp.setMax(playerMaxHp); pbPlayerHp.setProgress(playerHp); toRemoveList.add(c);
        }
        for (View v : toRemoveList) { gameArena.removeView(v); powerCubes.remove(v); } toRemoveList.clear();

        bulletsToRemove.clear();
        for (Bullet b : bullets) {
            b.view.setX(b.view.getX() + b.vx); b.view.setY(b.view.getY() + b.vy);
            if (b.view.getX() < -500 || b.view.getX() > gameArena.getWidth() + 500) { bulletsToRemove.add(b); continue; }
            if (b.team != playerTeam && Rect.intersects(getRect(b.view), getRect(imgPlayer))) {
                int damage = isSuperShieldActive ? 2 : 8;
                playerHp -= damage; pbPlayerHp.setProgress(playerHp); lastHitTime = System.currentTimeMillis();
                bulletsToRemove.add(b); if (playerHp <= 0) { playerRank = playersAlive; gameOver(); }
            }
            for (Enemy e : enemies) if (b.team != e.team && b.view.getTag() != e.view && Rect.intersects(getRect(b.view), getRect(e.view))) {
                e.hp -= (b.team == 0 ? (playerDamage + collectedCubes + matchKills) : 2); e.hpBar.setProgress(e.hp);
                if (b.team == 0) { 
                    showDamageText(e.view.getX(), e.view.getY(), playerDamage + collectedCubes + matchKills);
                    ultiCharge = Math.min(100, ultiCharge+5); updateUltiUI(); 
                }
                if (b.view.getTag() == null || !"mega".equals(b.view.getTag())) {
                    bulletsToRemove.add(b);
                }
                break;
            }
            for (View o : obstacles) {
                if (o.getTag() instanceof ProgressBar && Rect.intersects(getRect(b.view), getRect(o))) {
                    ProgressBar h = (ProgressBar) o.getTag(); int cH = (int) h.getTag(); cH--; h.setProgress(cH); h.setTag(cH);
                    if (cH <= 0) {
                        explodeTNT(o.getX(), o.getY());
                        gameArena.removeView(o); gameArena.removeView(h); toRemoveList.add(o);
                    }
                    bulletsToRemove.add(b); break;
                }
            }
            for (View cr : crates) if (Rect.intersects(getRect(b.view), getRect(cr))) {
                ProgressBar h = (ProgressBar) cr.getTag(); int cH = (int) h.getTag(); cH--; h.setProgress(cH); h.setTag(cH);
                if (cH <= 0) { spawnPowerCube(cr.getX(), cr.getY()); gameArena.removeView(cr); gameArena.removeView(h); toRemoveList.add(cr); }
                bulletsToRemove.add(b); break;
            }
            for (View c : toRemoveList) crates.remove(c); toRemoveList.clear();
        }
        for (Bullet b : bulletsToRemove) { gameArena.removeView(b.view); bullets.remove(b); }

        enemiesToRemove.clear();
        for (Enemy e : enemies) {
            View t = null; float mD = Float.MAX_VALUE;
            if (playerTeam != e.team) { t = imgPlayer; mD = distSq(e.view, imgPlayer); }
            for (Enemy other : enemies) {
                if (other.team != e.team) {
                    float d = distSq(e.view, other.view);
                    if (d < mD) {
                        mD = d;
                        t = other.view;
                    }
                }
            }
            if (t != null) {
                float dx = t.getX()-e.view.getX(); float dy = t.getY()-e.view.getY(); float dist = (float) Math.sqrt(dx*dx+dy*dy);
                for (Bullet b : bullets) if (b.team != e.team && distSq(e.view, b.view) < 400*400) {
                    e.view.setX(e.view.getX() + b.vy*0.15f); e.view.setY(e.view.getY() - b.vx*0.15f);
                }
                float edxC = e.view.getX()-gameArena.getWidth()/2f; float edyC = e.view.getY()-gameArena.getHeight()/2f;
                if (isGasEnabled && Math.sqrt(edxC*edxC+edyC*edyC) > safeZoneRadius-100) {
                    dx = gameArena.getWidth()/2f-e.view.getX(); dy = gameArena.getHeight()/2f-e.view.getY(); dist = (float) Math.sqrt(dx*dx+dy*dy);
                }
                if (dist > 300) { e.view.setX(e.view.getX()+(dx/dist)*e.speed); e.view.setY(e.view.getY()+(dy/dist)*e.speed); }
                e.view.setRotation((float) Math.toDegrees(Math.atan2(dy, dx)) + 90);
                if (System.currentTimeMillis()-e.lastShootTime > enemyShootInterval && dist < 850) {
                    float lX = dx; float lY = dy; 
                    if (t == imgPlayer) { lX += playerDx*20; lY += playerDy*20; }
                    spawnEnemyBullet(e.view.getX()+e.view.getWidth()/2f, e.view.getY()+e.view.getHeight()/2f, (float) Math.toDegrees(Math.atan2(-lY, lX)), e.view, e.team);
                    e.lastShootTime = System.currentTimeMillis();
                }
            }
            e.hpBar.setX(e.view.getX()); e.hpBar.setY(e.view.getY()-25); e.nameTag.setX(e.view.getX()); e.nameTag.setY(e.view.getY()-55);
            if (e.hp <= 0) enemiesToRemove.add(e);
        }
        for (Enemy e : enemiesToRemove) {
            gameArena.removeView(e.view); gameArena.removeView(e.hpBar); gameArena.removeView(e.nameTag);
            addToKillFeed(getString(e.team == 0 ? R.string.ally_defeated : R.string.enemy_eliminated));
            if (e.team != 0) {
                matchKills++;
                showEmote(imgPlayer, "😁");
            }
            if (random.nextBoolean()) spawnPowerCube(e.view.getX(), e.view.getY());
            enemies.remove(e); playersAlive--; tvScore.setText(getString(R.string.players_left_label, playersAlive));
            if (playersAlive == 1 && playerHp > 0) { playerRank = 1; gameOver(); }
        }
    }

    private void explodeTNT(float x, float y) {
        View exp = new View(this); exp.setLayoutParams(new FrameLayout.LayoutParams(400, 400));
        exp.setBackgroundColor(0xFFFFA500); exp.setAlpha(0.6f); exp.setX(x - 150); exp.setY(y - 150);
        gameArena.addView(exp); shakeScreen(40);
        Rect eR = new Rect((int)exp.getX(), (int)exp.getY(), (int)(exp.getX()+400), (int)(exp.getY()+400));
        if (Rect.intersects(eR, getRect(imgPlayer))) { playerHp -= 25; pbPlayerHp.setProgress(playerHp); if(playerHp<=0){playerRank=playersAlive; gameOver();} }
        for (Enemy e : enemies) if (Rect.intersects(eR, getRect(e.view))) { e.hp -= 40; e.hpBar.setProgress(e.hp); }
        gameHandler.postDelayed(() -> gameArena.removeView(exp), 250);
    }

    private void spawnEnemyBullet(float x, float y, float a, View s, int t) {
        View v = new View(this); v.setLayoutParams(new FrameLayout.LayoutParams(20, 20));
        v.setBackgroundColor(Color.RED); v.setX(x); v.setY(y); v.setTag(s);
        double rad = Math.toRadians(a); float vx = (float) (Math.cos(rad) * (BULLET_SPEED-5)); float vy = (float) (-Math.sin(rad) * (BULLET_SPEED-5));
        gameArena.addView(v); bullets.add(new Bullet(v, vx, vy, t));
    }

    private void spawnPowerCube(float x, float y) {
        View c = new View(this); c.setLayoutParams(new FrameLayout.LayoutParams(40, 40));
        c.setBackgroundResource(R.drawable.power_cube_style); c.setX(x); c.setY(y); gameArena.addView(c); powerCubes.add(c);
    }

    private void shakeScreen(int i) {
        gameArena.animate().translationXBy(i).setDuration(50).withEndAction(() -> 
            gameArena.animate().translationXBy(-i*2).setDuration(50).withEndAction(() -> 
                gameArena.animate().translationXBy(i).setDuration(50))).start();
    }

    private float distSq(View v1, View v2) { float dx = v1.getX()-v2.getX(); float dy = v1.getY()-v2.getY(); return dx*dx+dy*dy; }
    private Rect getRect(View v) { return new Rect((int)v.getX(), (int)v.getY(), (int)(v.getX()+v.getWidth()), (int)(v.getY()+v.getHeight())); }
    private void showDamageText(float x, float y, int a) {
        TextView d = new TextView(this); d.setText(getString(R.string.damage_dealt_label, a)); d.setTextColor(Color.YELLOW); d.setX(x); d.setY(y);
        gameArena.addView(d); d.animate().translationYBy(-100).alpha(0).setDuration(500).withEndAction(() -> gameArena.removeView(d)).start();
    }

    private void showEmote(@NonNull View anchor, String emoji) {
        TextView tv = new TextView(this);
        tv.setText(emoji); tv.setTextSize(24);
        tv.setX(anchor.getX()); tv.setY(anchor.getY() - 100);
        gameArena.addView(tv);
        tv.animate().translationYBy(-150).alpha(0).setDuration(1500).withEndAction(() -> gameArena.removeView(tv)).start();
    }

    private void updateUltiUI() { btnUlti.setAlpha(ultiCharge >= 100 ? 1.0f : 0.5f); btnUlti.setEnabled(ultiCharge >= 100); }
    private void addToKillFeed(String m) {
        TextView tv = new TextView(this); tv.setText(m); tv.setTextColor(Color.WHITE); tv.setTextSize(12);
        tv.setBackgroundColor(0x66000000); tv.setPadding(10, 5, 10, 5);
        killFeedContainer.addView(tv, 0); if (killFeedContainer.getChildCount() > 5) killFeedContainer.removeViewAt(5);
        gameHandler.postDelayed(() -> killFeedContainer.removeView(tv), 4000);
    }

    private void gameOver() {
        if (isGameOver) return; isGameOver = true;
        int tC = 0; 
        switch (playerRank) {
            case 1: tC = 10; break; case 2: tC = 8; break; case 3: tC = 6; break;
            case 4: tC = 4; break; case 5: tC = 2; break; case 8: tC = -2; break; case 9: tC = -4; break; case 10: tC = -6; break;
        }
        SharedPreferences prefs = getSharedPreferences("BrawlPrefs", MODE_PRIVATE);
        int tT = Math.max(0, prefs.getInt("total_trophies", 0) + tC);
        int tCns = prefs.getInt("total_coins", 0) + (10-playerRank)*5;
        prefs.edit().putInt("total_trophies", tT).putInt("total_coins", tCns).apply();
        Toast.makeText(this, getString(R.string.rank_message, playerRank, (tC >= 0 ? "+" : ""), tC), Toast.LENGTH_LONG).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> { startActivity(new Intent(this, LobbyActivity.class)); finish(); }, 2000);
    }
}
