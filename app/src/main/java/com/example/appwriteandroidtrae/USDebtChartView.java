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

public class USDebtChartView extends View {

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<USDebtPoint> points = new ArrayList<>();
    private final SimpleDateFormat labelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

    public USDebtChartView(Context context) {
        this(context, null);
    }

    public USDebtChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public USDebtChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        axisPaint.setColor(Color.parseColor("#374151"));
        axisPaint.setStrokeWidth(dp(1.5f));

        gridPaint.setColor(Color.parseColor("#E5E7EB"));
        gridPaint.setStrokeWidth(dp(1f));

        linePaint.setColor(Color.parseColor("#0F766E"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3f));

        pointPaint.setColor(Color.parseColor("#115E59"));
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#4B5563"));
        textPaint.setTextSize(sp(12f));

        emptyPaint.setColor(Color.parseColor("#6B7280"));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(sp(15f));
    }

    public void setPoints(List<USDebtPoint> newPoints) {
        points.clear();
        points.addAll(newPoints);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.isEmpty()) {
            canvas.drawText("No U.S. debt history yet", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        float left = dp(52);
        float top = dp(20);
        float right = getWidth() - dp(16);
        float bottom = getHeight() - dp(34);
        float width = right - left;
        float height = bottom - top;

        long min = points.get(0).debtValue;
        long max = points.get(0).debtValue;
        for (USDebtPoint point : points) {
            min = Math.min(min, point.debtValue);
            max = Math.max(max, point.debtValue);
        }
        if (max == min) {
            max += 1_000_000L;
            min -= 1_000_000L;
        }

        for (int i = 0; i <= 4; i++) {
            float y = top + (height * i / 4f);
            canvas.drawLine(left, y, right, y, gridPaint);
            long value = max - ((max - min) * i / 4L);
            canvas.drawText(formatAxis(value), dp(4), y + dp(4), textPaint);
        }

        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? left + width / 2f : left + (width * i / (points.size() - 1f));
            float y = (float) (bottom - ((points.get(i).debtValue - min) / (double) (max - min) * height));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);

        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? left + width / 2f : left + (width * i / (points.size() - 1f));
            float y = (float) (bottom - ((points.get(i).debtValue - min) / (double) (max - min) * height));
            canvas.drawCircle(x, y, dp(4), pointPaint);
        }

        int labelCount = Math.min(4, points.size());
        for (int i = 0; i < labelCount; i++) {
            int index = labelCount == 1 ? points.size() - 1 : Math.round(i * (points.size() - 1f) / (labelCount - 1f));
            float x = points.size() == 1 ? left + width / 2f : left + (width * index / (points.size() - 1f));
            String label = labelFormat.format(new Date(points.get(index).fetchedAtMillis));
            float textWidth = textPaint.measureText(label);
            canvas.drawText(label, x - textWidth / 2f, getHeight() - dp(8), textPaint);
        }
    }

    private String formatAxis(long value) {
        return String.format(Locale.getDefault(), "$%.1fT", value / 1_000_000_000_000.0);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
