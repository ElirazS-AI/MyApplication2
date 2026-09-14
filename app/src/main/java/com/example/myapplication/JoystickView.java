package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class JoystickView extends View {

    private Paint backgroundPaint;
    private Paint handlePaint;
    private float centerX, centerY;
    private float backgroundRadius;
    private float handleRadius;
    private float handleX, handleY;
    private OnMoveListener onMoveListener;

    public interface OnMoveListener {
        void onMove(int angle, int strength);
    }

    public JoystickView(Context context) {
        super(context);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#2B2D42"));
        backgroundPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.parseColor("#FF4757"));
        handlePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerX = w / 2f;
        centerY = h / 2f;
        backgroundRadius = Math.min(w, h) / 2.5f;
        handleRadius = backgroundRadius / 2f;
        handleX = centerX;
        handleY = centerY;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawCircle(centerX, centerY, backgroundRadius, backgroundPaint);
        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
            case MotionEvent.ACTION_MOVE:
                float dx = x - centerX;
                float dy = y - centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance <= backgroundRadius) {
                    handleX = x;
                    handleY = y;
                } else {
                    handleX = centerX + dx / distance * backgroundRadius;
                    handleY = centerY + dy / distance * backgroundRadius;
                    distance = backgroundRadius;
                }

                if (onMoveListener != null && backgroundRadius > 0) {
                    int angle = (int) Math.toDegrees(Math.atan2(centerY - handleY, handleX - centerX));
                    if (angle < 0) angle += 360;
                    int strength = (int) (distance / backgroundRadius * 100);
                    onMoveListener.onMove(angle, strength);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                handleX = centerX;
                handleY = centerY;
                if (onMoveListener != null) {
                    onMoveListener.onMove(0, 0);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.onMoveListener = listener;
    }
}
