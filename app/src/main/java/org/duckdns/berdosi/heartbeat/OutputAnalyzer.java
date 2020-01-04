package org.duckdns.berdosi.heartbeat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.view.TextureView;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

class OutputAnalyzer {
    private final Activity activity;
    private final ChartDrawer chartDrawer;

    private MeasureStore store;

    private final int measurementInterval = 50;
    private final int measurementLength = 15000; // ensure the number of data points is the power of two
    private final int clipLength = 3500;
    private final float dropHeight = 0.15f;

    private int detectedValleys = 0;
    private int ticksPassed = 0;

    private final CopyOnWriteArrayList<Long> valleys = new CopyOnWriteArrayList<>();

    private CountDownTimer timer;


    OutputAnalyzer(Activity activity, TextureView graphTextureView) {
        this.activity = activity;
        this.chartDrawer = new ChartDrawer(graphTextureView);
    }

    private boolean detectValley() {
        final int valleyDetectionWindowSize = 15;
        CopyOnWriteArrayList<Measurement<Integer>> subList = store.getLastStdValues(valleyDetectionWindowSize);
        if (subList.size() < valleyDetectionWindowSize) {
            return false;
        } else {
            Integer referenceValue = subList.get((int) Math.ceil(valleyDetectionWindowSize / 2)).measurement;

            for (Measurement<Integer> measurement : subList) {
                if (measurement.measurement < referenceValue) return false;
            }

            // filter out consecutive measurements due to too high measurement rate
            return (! subList.get((int) Math.ceil(valleyDetectionWindowSize / 2)).measurement.equals(
                    subList.get((int) Math.ceil(valleyDetectionWindowSize / 2) - 1).measurement));
        }
    }

    void measurePulse(TextureView textureView) {

        // 10 times a second, get the amount of red on the picture.
        // over the past 5 seconds, get the minimum, maximum, and the standardized values

        Log.i("measure", "started");
        store = new MeasureStore();

        detectedValleys = 0;

        timer = new CountDownTimer(measurementLength, measurementInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                // skip the first measurements, which are broken by exposure metering
                if (clipLength > (++ticksPassed * measurementInterval)) return;

                Thread thread = new Thread(() -> {
                Bitmap currentBitmap = textureView.getBitmap();
                int pixelCount = textureView.getWidth() * textureView.getHeight();
                int measurement = 0;
                int[] pixels = new int[pixelCount];

                // todo this fails if
                currentBitmap.getPixels(pixels, 0, textureView.getWidth(), 0, 0, textureView.getWidth(), textureView.getHeight());

                // extract the red component
                // https://developer.android.com/reference/android/graphics/Color.html#decoding
                for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
                    measurement += (pixels[pixelIndex] >> 16) & 0xff;
                }
                // max int is 2^31 (2147483647) , so width and height can be at most 2^11,
                // as 2^8 * 2^11 * 2^11 = 2^30, just below the limit

                store.add(measurement);

                if (detectValley()) {
                    detectedValleys = detectedValleys + 1;
                    // in 13 seconds (13000 milliseconds), I expect 15 valleys. that would be a pulse of 15 / 130000 * 60 * 1000 = 69

                    String currentValue = String.format(
                        Locale.getDefault(),
                        "%f , %d valleys in %f seconds",
                        60f * (detectedValleys) / (Math.max(1, (measurementLength - millisUntilFinished - clipLength) / 1000f)),
                        detectedValleys,
                        1f * (measurementLength - millisUntilFinished - clipLength) / 1000f);
                    ((EditText) activity.findViewById(R.id.editText)).setText(currentValue);
                    valleys.add(store.getLastTimestamp().getTime());
                    ((TextView) activity.findViewById(R.id.textView)).setText(currentValue  );
                }

                    // draw the chart on a separate thread.
                    Thread chartDrawerThread = new Thread(() -> chartDrawer.draw(store.getStdValues()));
                    chartDrawerThread.start();
                });
                thread.start();
            }

            @Override
            public void onFinish() {
                CopyOnWriteArrayList<Measurement<Float>> stdValues = store.getStdValues();

                // clip the interval to the first till the last one - on this interval, there were detectedValleys - 1 periods
                String currentValue = String.format(
                        Locale.getDefault(),
                        "%f , %d valleys in %f seconds",
                        60f * (detectedValleys - 1) / (Math.max(1, (valleys.get(valleys.size() - 1) - valleys.get(0)) / 1000f)),
                        detectedValleys - 1,
                        1f * ( valleys.get(valleys.size() - 1) - valleys.get(0) ) / 1000f);

                ((EditText) activity.findViewById(R.id.editText)).setText(currentValue);

                // to the debug message, add a "raw" value
                returnValueSb.append(String.format(
                        Locale.getDefault(),
                        "%f , %d valleys in %f seconds",
                        60f * (detectedValleys) / (Math.max(1, (measurementLength - clipLength) / 1000f)),
                        detectedValleys,
                        1f * (measurementLength - clipLength) / 1000f));
                returnValueSb.append(activity.getString(R.string.row_separator));

                // look for "drops" of 0.15 - 0.75 in the value
                // a drop may take 2-3 ticks.
                int dropCount = 0;
                for (int stdValueIdx = 4; stdValueIdx < stdValues.size(); stdValueIdx++) {
                    if (((stdValues.get(stdValueIdx - 2).measurement - stdValues.get(stdValueIdx).measurement) > dropHeight) &&
                            !((stdValues.get(stdValueIdx - 3).measurement - stdValues.get(stdValueIdx - 1).measurement) > dropHeight) &&
                            !((stdValues.get(stdValueIdx - 4).measurement - stdValues.get(stdValueIdx - 2).measurement) > dropHeight)
                    ) {
                        dropCount++;
                    }
                }

                returnValueSb.append(activity.getString(R.string.detected_pulse));
                returnValueSb.append(activity.getString(R.string.separator));
                returnValueSb.append((float) dropCount / ((float) (measurementLength - clipLength) / 1000f / 60f));
                returnValueSb.append(activity.getString(R.string.row_separator));

                // building a double[] could help if Fourier transformation worked.
                // FFT requires its length to be a power of two.
                // double[] stdValuesDoubleArray = new double[(int) Math.pow(2, Math.ceil(Math.log(stdValues.size()) / Math.log(2)))];


                int doubleArrayIndex = 0;
                for (int stdValueIdx = 0; stdValueIdx < stdValues.size(); stdValueIdx++) {
                    Measurement<Float> value = stdValues.get(stdValueIdx);
                    // stdValues.forEach((value) -> {
                    returnValueSb.append(value.timestamp.getTime());
                    returnValueSb.append(activity.getString(R.string.separator));
                    returnValueSb.append(value.measurement);
                    returnValueSb.append(activity.getString(R.string.row_separator));

                    // stdValuesDoubleArray[doubleArrayIndex++] = (double) value.measurement;
                    // });
                }

                // add detected valleys location
                for (long tick : valleys) {
                    returnValueSb.append(tick);
                    returnValueSb.append(activity.getString(R.string.row_separator));
                }


                returnValueSb.append(activity.getString(R.string.fourier));
                returnValueSb.append(activity.getString(R.string.row_separator));

                // FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.UNITARY);
                // Complex[] transformationResult = transformer.transform(stdValuesDoubleArray, TransformType.FORWARD);

                // for (Complex complex : transformationResult) {
                //     returnValueSb.append(complex.getReal());
                //     returnValueSb.append(activity.getString(R.string.separator));
                //     returnValueSb.append(complex.getImaginary());
                //     returnValueSb.append(activity.getString(R.string.row_separator));
                // }

                ((EditText) activity.findViewById(R.id.editText)).setText(returnValueSb.toString());
            }
        };

        timer.start();
    }

    void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
