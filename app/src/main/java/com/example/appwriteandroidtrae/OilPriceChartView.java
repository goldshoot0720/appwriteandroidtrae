package com.example.appwriteandroidtrae;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OilPriceChartView extends View {

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<OilPricePoint> points = new ArrayList<>();
    private final SimpleDateFormat labelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

    public OilPriceChartView(Context context) {
        this(context, null);
    }

    public OilPriceChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OilPriceChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        axisPaint.setColor(Color.parseColor("#374151"));
        axisPaint.setStrokeWidth(dp(1.5f));

        gridPaint.setColor(Color.parseColor("#E5E7EB"));
        gridPaint.setStrokeWidth(dp(1f));

        linePaint.setColor(Color.parseColor("#2563EB"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3f));

        pointPaint.setColor(Color.parseColor("#1D4ED8"));
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#4B5563"));
        textPaint.setTextSize(sp(12f));

        emptyPaint.setColor(Color.parseColor("#6B7280"));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(sp(15f));
    }

    public void setPoints(List<OilPricePoint> newPoints) {
        points.clear();
        points.addAll(newPoints);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.isEmpty()) {
            canvas.drawText("尚無石油價格資料", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        float left = dp(44);
        float top = dp(20);
        float right = getWidth() - dp(16);
        float bottom = getHeight() - dp(34);
        float width = right - left;
        float height = bottom - top;

        double min = points.get(0).price;
        double max = points.get(0).price;
        for (OilPricePoint point : points) {
            min = Math.min(min, point.price);
            max = Math.max(max, point.price);
        }
        if (Math.abs(max - min) < 0.001) {
            max += 1.0;
            min -= 1.0;
        }

        for (int i = 0; i <= 4; i++) {
            float y = top + (height * i / 4f);
            canvas.drawLine(left, y, right, y, gridPaint);
            double value = max - ((max - min) * i / 4f);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", value), dp(4), y + dp(4), textPaint);
        }

        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? left + width / 2f : left + (width * i / (points.size() - 1f));
            float y = (float) (bottom - ((points.get(i).price - min) / (max - min) * height));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);

        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? left + width / 2f : left + (width * i / (points.size() - 1f));
            float y = (float) (bottom - ((points.get(i).price - min) / (max - min) * height));
            canvas.drawCircle(x, y, dp(4), pointPaint);
        }

        int labelCount = Math.min(4, points.size());
        for (int i = 0; i < labelCount; i++) {
            int index = labelCount == 1 ? points.size() - 1 : Math.round(i * (points.size() - 1f) / (labelCount - 1f));
            float x = points.size() == 1 ? left + width / 2f : left + (width * index / (points.size() - 1f));
            String label = labelFormat.format(new Date(points.get(index).dateMillis));
            float textWidth = textPaint.measureText(label);
            canvas.drawText(label, x - textWidth / 2f, getHeight() - dp(8), textPaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
