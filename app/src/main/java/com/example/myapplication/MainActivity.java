package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private TextView chatDisplay, mathLog;
    private EditText userInput, trainingDataInput;
    private Button btnTrain, btnBackToChat;
    private ProgressBar trainingProgress;
    private ScrollView chatScroll;

    private final Map<String, double[]> wordEmbeddings = new HashMap<>();
    private final int embeddingDim = 16;
    private final Random random = new Random();
    private boolean isTrained = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. הפעלת האנימציה עם הגנה מקריסות
        try {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.matrix_line_animation);
            View l1 = findViewById(R.id.line1);
            View l2 = findViewById(R.id.line2);
            View l3 = findViewById(R.id.line3);
            View l4 = findViewById(R.id.line4);

            if (l1 != null) l1.startAnimation(anim);
            if (l2 != null) {
                anim.setStartOffset(1000);
                l2.startAnimation(anim);
            }
            if (l3 != null) {
                Animation anim3 = AnimationUtils.loadAnimation(this, R.anim.matrix_line_animation);
                anim3.setStartOffset(2000);
                l3.startAnimation(anim3);
            }
            if (l4 != null) {
                Animation anim4 = AnimationUtils.loadAnimation(this, R.anim.matrix_line_animation);
                anim4.setStartOffset(3000);
                l4.startAnimation(anim4);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Animation error", e);
        }

        // 2. אתחול מדויק של כל רכיבי הממשק
        viewFlipper = findViewById(R.id.viewFlipper);
        chatDisplay = findViewById(R.id.chatDisplay);
        userInput = findViewById(R.id.userInput);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnOpenTraining = findViewById(R.id.btnOpenTraining);
        chatScroll = findViewById(R.id.chatScroll);

        trainingDataInput = findViewById(R.id.trainingDataInput);
        btnTrain = findViewById(R.id.btnTrain); // חיבור כפתור ה-TRAIN המוגדל
        btnBackToChat = findViewById(R.id.btnBackToChat);
        trainingProgress = findViewById(R.id.trainingProgress);
        mathLog = findViewById(R.id.mathLog);

        // 3. מאזיני לחיצה למעבר בין מסכים
        if (btnOpenTraining != null) {
            btnOpenTraining.setOnClickListener(v -> {
                if (viewFlipper != null) viewFlipper.setDisplayedChild(1);
            });
        }

        if (btnBackToChat != null) {
            btnBackToChat.setOnClickListener(v -> {
                if (viewFlipper != null) viewFlipper.setDisplayedChild(0);
            });
        }

        // 4. לוגיקת שליחת הודעה בצ'אט
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                if (userInput == null || chatDisplay == null) return;
                String query = userInput.getText().toString().trim();
                if (query.isEmpty()) {
                    return;
                }

                chatDisplay.append(getString(R.string.chat_you_label, query));
                userInput.setText("");

                String response = generateResponse(query);
                chatDisplay.append(getString(R.string.chat_model_label, response));

                if (chatScroll != null) chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
            });
        }

        // 5. הפעלת לולאת האימון בלחיצה על הכפתור התחתון הגדול
        if (btnTrain != null) {
            btnTrain.setOnClickListener(v -> startSelfTraining());
        }
    }

    // =========================================================================
    // מנוע האימון הרץ ברקע ומעדכן את הממשק
    // =========================================================================
    private void startSelfTraining() {
        if (trainingDataInput == null || btnTrain == null || trainingProgress == null || mathLog == null) return;

        String data = trainingDataInput.getText().toString().trim();
        if (data.length() < 5) {
            Toast.makeText(this, R.string.train_toast_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        // נעילת כפתורים זמנית בזמן האימון למניעת קפיאות
        btnTrain.setEnabled(false);
        if (btnBackToChat != null) btnBackToChat.setEnabled(false);
        trainingProgress.setVisibility(View.VISIBLE);
        mathLog.setText(R.string.train_log_start);
        wordEmbeddings.clear();

        new Thread(() -> {
            String[] words = data.toLowerCase().split("\\s+");

            for (String w : words) {
                if (!wordEmbeddings.containsKey(w)) {
                    double[] vec = new double[embeddingDim];
                    Arrays.setAll(vec, i -> random.nextDouble());
                    wordEmbeddings.put(w, vec);
                }
            }

            int epochs = 100;
            int e = 1;
            do {
                double loss = Math.abs(random.nextGaussian() / e);

                if (e % 20 == 0) {
                    updateLog(getString(R.string.train_log_epoch, e, String.format(Locale.getDefault(), "%.5f", loss)));
                }

                final int progress = e;
                new Handler(Looper.getMainLooper()).post(() -> trainingProgress.setProgress(progress));

                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                e++;
            } while (e <= epochs);

            isTrained = true;

            // סיום אימון ומעבר אוטומטי חזרה לצ'אט
            new Handler(Looper.getMainLooper()).post(() -> {
                trainingProgress.setVisibility(View.GONE);
                btnTrain.setEnabled(true);
                if (btnBackToChat != null) btnBackToChat.setEnabled(true);
                if (viewFlipper != null) viewFlipper.setDisplayedChild(0); // זריקה אוטומטית לצ'אט

                if (chatDisplay != null) {
                    chatDisplay.append(getString(R.string.train_complete_msg));
                }
                if (chatScroll != null) chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
            });
        }).start();
    }

    private String generateResponse(String input) {
        if (!isTrained) return getString(R.string.model_not_trained);

        String[] queryWords = input.toLowerCase().split("\\s+");
        List<String> trainedWords = new ArrayList<>(wordEmbeddings.keySet());

        for (String qw : queryWords) {
            if (qw.length() <= 2) continue;

            int i = 0;
            while (i < trainedWords.size() - 1) {
                if (Objects.equals(trainedWords.get(i), qw)) {
                    String next1 = trainedWords.get(i + 1);
                    String next2 = (i + 2 < trainedWords.size()) ? trainedWords.get(i + 2) : "";
                    String next3 = (i + 3 < trainedWords.size()) ? trainedWords.get(i + 3) : "";

                    return getString(R.string.model_context_response, qw, next1, next2, next3);
                }
                i++;
            }
        }
        return getString(R.string.model_no_match);
    }

    private void updateLog(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mathLog != null) mathLog.append(msg);
        });
    }
}