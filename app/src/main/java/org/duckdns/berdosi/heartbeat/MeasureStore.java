package org.duckdns.berdosi.heartbeat;

import java.util.ArrayList;
import java.util.Date;

class MeasureStore {
    private final ArrayList<Measurement<Integer>> measurements = new ArrayList<>();
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

    ArrayList<Measurement<Float>> getStdValues() {
        ArrayList<Measurement<Float>> stdValues = new ArrayList<>();

        for (Measurement<Integer> measurement : measurements) {
            Measurement<Float> stdValue =
                    new Measurement<>(
                            measurement.timestamp,
                            (measurement.measurement - average) / (float) ((maximum - minimum) / 2));
            stdValues.add(stdValue);
        }

        return stdValues;
    }

    ArrayList<Measurement<Integer>> getLastStdValues(int count) {
        if (count < measurements.size()) {
            return  new ArrayList<>(measurements.subList(measurements.size() - 1 - count, measurements.size() - 1));
        } else {
            return measurements;
        }
    }

    Date getLastTimestamp() {
        return measurements.get(measurements.size() - 1).timestamp;
    }
}
