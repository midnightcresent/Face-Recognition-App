package com.example.facerecognitionapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class FaceBoundingBoxView extends View {
    private RectF boundingBox;
    private final Paint paint;

    public FaceBoundingBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
    }

    public void setBoundingBox(Rect rect, int imageWidth, int imageHeight) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        float scaleFactorX = (float) viewWidth / (float) imageHeight;
        float scaleFactorY = (float) viewHeight / (float) imageWidth;
        boundingBox = new RectF(
                rect.left * scaleFactorX,
                rect.top * scaleFactorY,
                rect.right * scaleFactorX,
                rect.bottom * scaleFactorY
        );
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (boundingBox != null) {
            canvas.drawRect(boundingBox, paint);
        }
    }
}
