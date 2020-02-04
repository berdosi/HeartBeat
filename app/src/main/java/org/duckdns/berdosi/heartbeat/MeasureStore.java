package org.duckdns.berdosi.heartbeat;

import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

class MeasureStore {
    private final CopyOnWriteArrayList<Measurement<Integer>> measurements = new CopyOnWriteArrayList<>();
    private int minimum = 2147483647;
    private int maximum = -2147483648;
    private int count = 0;
    private float average = 0;
    private int rollingAverageSize = 3;

    void add(int measurement) {
        Measurement<Integer> measurementWithDate = new Measurement<>(new Date(), measurement);

        count++;
        if (count == 1) average = (float) measurement;
        else average = (((count - 1) * average) + measurement) / count;

        measurements.add(measurementWithDate);
        if (measurement < minimum) minimum = measurement;
        if (measurement > maximum) maximum = measurement;
    }

    CopyOnWriteArrayList<Measurement<Float>> getStdValues() {
        CopyOnWriteArrayList<Measurement<Float>> stdValues = new CopyOnWriteArrayList<>();

        for (int i = 0; i < measurements.size(); i++) {
            float sum = measurements.get(i).measurement;
            for (int rollingAverageCounter = 0; rollingAverageCounter < rollingAverageSize; rollingAverageCounter++) {
                sum += measurements.get(Math.max(0, i - rollingAverageCounter)).measurement;
            }

            Measurement<Float> stdValue =
                    new Measurement<>(
                            measurements.get(i).timestamp,
                            (sum / rollingAverageSize - average) / (float) ((maximum - minimum) / 2));
            stdValues.add(stdValue);
        }

        return stdValues;
    }

    CopyOnWriteArrayList<Measurement<Integer>> getLastStdValues(int count) {
        if (count < measurements.size()) {
            return  new CopyOnWriteArrayList<>(measurements.subList(measurements.size() - 1 - count, measurements.size() - 1));
        } else {
            return measurements;
        }
    }

    Date getLastTimestamp() {
        return measurements.get(measurements.size() - 1).timestamp;
    }
}
