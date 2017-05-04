package com.edillower.heymavic;

/**
 * Handler of Battery UI
 * @maintainer Melody Cai
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


public class BatteryView extends View {
    private float percent = 1.0f;

    public BatteryView(Context context, AttributeSet set) {
        super(context, set);
    }

    @Override
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint outerFlame = new Paint();
        Paint innerColor = new Paint();
        Paint head = new Paint();
        outerFlame.setAntiAlias(true);
        outerFlame.setStyle(Paint.Style.FILL);
        // Change color
        if (percent > 0.3f) {
            outerFlame.setColor(Color.GREEN);
        } else {
            outerFlame.setColor(Color.RED);
        }

        innerColor.setAntiAlias(true);
        innerColor.setStyle(Paint.Style.STROKE);
        innerColor.setStrokeWidth(4);
        innerColor.setColor(Color.WHITE);
        head.setAntiAlias(true);
        head.setStyle(Paint.Style.FILL);
        head.setColor(Color.WHITE);
        int a = getWidth() - 4;
        int b = getHeight() - 4;
        // draw
        float d = a * percent;
        RectF re1 = new RectF(4, 4, d - 10, b);
        RectF re2 = new RectF(0, 0, a - 6, b + 4);
        RectF re3 = new RectF(a - 8, b / 2 - 8, a, b - 6);
        canvas.drawRect(re1, outerFlame);
        canvas.drawRect(re2, innerColor);
        canvas.drawRect(re3, head);
    }

    // update battery information
    public synchronized void setProgress(int percent) {
        this.percent = (float) (percent / 100.0);
        postInvalidate();
    }
}
