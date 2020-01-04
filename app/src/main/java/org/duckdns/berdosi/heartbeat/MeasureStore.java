package org.duckdns.berdosi.heartbeat;

import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

class MeasureStore {
    private final CopyOnWriteArrayList<Measurement<Integer>> measurements = new CopyOnWriteArrayList<>();
    private int minimum = 2147483647;
    private int maximum = -2147483648;
    private int count = 0;
    private float average = 0;

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

        for (Measurement<Integer> measurement : measurements) {
            Measurement<Float> stdValue =
                    new Measurement<>(
                            measurement.timestamp,
                            (measurement.measurement - average) / (float) ((maximum - minimum) / 2));
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
