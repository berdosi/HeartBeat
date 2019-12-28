package org.duckdns.berdosi.heartbeat;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;

class ChartDrawer
{
    private final Canvas chartCanvas;
    private final Paint paint = new Paint();

    ChartDrawer(Canvas chartCanvas) {
        this.chartCanvas = chartCanvas;
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(2);

    }

    void draw(ArrayList<Measurement<Float>> data) {
        Path graphPath = new Path();
        float width = (float)chartCanvas.getWidth();
        float height = (float)chartCanvas.getHeight();
        int dataAmount = data.size();

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (Measurement<Float> dataPoint :data) {
            if (dataPoint.measurement < min) min = dataPoint.measurement;
            if (dataPoint.measurement > max) max = dataPoint.measurement;
        }

        graphPath.moveTo(
                0,
                height * (data.get(0).measurement - min) / (max - min) );

        for (int dotIndex = 1; dotIndex < dataAmount; dotIndex++) {
            graphPath.lineTo(
                    width * (dotIndex) / dataAmount,
                    height * (data.get(dotIndex).measurement - min) / (max - min) );

        }

        chartCanvas.drawPath(graphPath, paint);
    }

}
