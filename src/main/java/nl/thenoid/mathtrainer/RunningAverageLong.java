/*
 * Copyright (C) 2026 Hylke van der Schaaf.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.thenoid.mathtrainer;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/**
 * Tracks a running average.
 */
public class RunningAverageLong {

    private final int size;
    private final ArrayDeque<Long> values;
    private double current;
    private long sum;

    public RunningAverageLong(int size, long dflt) {
        this.size = size;
        values = new ArrayDeque<>();
        if (dflt != 0) {
            for (int i = 0; i < size; i++) {
                values.add(dflt);
            }
            sum = size * dflt;
            current = dflt;
        }
    }

    public double add(long number) {
        long old = 0;
        if (values.size() >= size) {
            old = values.poll();
            sum -= old;
        }
        values.add(number);
        sum += number;
        current = 1.0 * sum / size;
        return current;
    }

    public double getValue() {
        return current;
    }

    public List<Long> getValues() {
        return Arrays.asList(values.toArray(new Long[size]));
    }

}
