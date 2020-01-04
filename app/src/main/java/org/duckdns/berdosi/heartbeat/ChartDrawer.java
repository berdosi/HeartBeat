package org.duckdns.berdosi.heartbeat;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.TextureView;

import java.util.concurrent.CopyOnWriteArrayList;

class ChartDrawer
{
    private  final TextureView chartTextureView;
    private final Paint paint = new Paint();
    private final Paint fillWhite = new Paint();

    ChartDrawer(TextureView chartTextureView) {
        this.chartTextureView = chartTextureView;


        paint.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);

        fillWhite.setStyle(Paint.Style.FILL);
        fillWhite.setColor(Color.WHITE);

    }

    void draw(CopyOnWriteArrayList<Measurement<Float>> data) {
        Canvas chartCanvas = chartTextureView.lockCanvas();

        if (chartCanvas == null) return;

        chartCanvas.drawPaint(fillWhite);
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


        chartTextureView.unlockCanvasAndPost(chartCanvas);
    }

}
