package org.duckdns.berdosi.heartbeat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class MeasureStore {
    private final List<Measurement<Integer>> measurements = new ArrayList<>();
    private int minimum =  2147483647;
    private int maximum =  -2147483648;
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
                            (measurement.measurement - average) / (float)((maximum - minimum) / 2));
            stdValues.add(stdValue);
        }

        return stdValues;
    }
}
