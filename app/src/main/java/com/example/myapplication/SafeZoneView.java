package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class SafeZoneView extends View {
    private final Paint poisonPaint;
    private final Paint transparentPaint;
    private float safeX = 1500f, safeY = 1500f, safeRadius = 2000f;

    public SafeZoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        poisonPaint = new Paint();
        poisonPaint.setColor(0xBB2E7D32); 
        poisonPaint.setAntiAlias(true);

        transparentPaint = new Paint();
        transparentPaint.setAntiAlias(true);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void updateSafeZone(float x, float y, float radius) {
        this.safeX = x;
        this.safeY = y;
        this.safeRadius = radius;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        // צביעת כל המסך ברעל - משתמשים במידות גדולות כדי להבטיח כיסוי במפה הגדולה
        canvas.drawRect(-5000, -5000, 5000, 5000, poisonPaint);
        
        // "חיתוך" האזור הבטוח
        canvas.drawCircle(safeX, safeY, safeRadius, transparentPaint);
    }
}
