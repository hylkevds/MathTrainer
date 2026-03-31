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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages duration averages for questions;
 */
public class AveragesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AveragesManager.class.getName());
    private final int size;
    private final long dflt;

    private final Set<String> activeProblems = new TreeSet<>();
    private final Map<String, RunningAverageLong> averages = new LinkedHashMap<>();

    public AveragesManager(int size, long dflt) {
        this.size = size;
        this.dflt = dflt;
    }

    public void add(String problem, long value) {
        RunningAverageLong avg = averages.computeIfAbsent(problem, p -> new RunningAverageLong(size, dflt));
        avg.add(value);
    }

    public void clearActiveProblems() {
        activeProblems.clear();
    }

    public void create(String problem) {
        activeProblems.add(problem);
        averages.computeIfAbsent(problem, p -> new RunningAverageLong(size, dflt));
    }

    public void remove(String problem) {
        averages.remove(problem);
    }

    public double getSum() {
        double sum = 0;
        for (var entry : averages.entrySet()) {
            if (!activeProblems.contains(entry.getKey())) {
                continue;
            }
            sum += entry.getValue().getValue();
        }
        return sum;
    }

    public String getProblemAt(double value) {
        double sum = 0;
        String key = "";
        for (var entry : averages.entrySet()) {
            key = entry.getKey();
            if (!activeProblems.contains(key)) {
                continue;
            }
            final double weight = entry.getValue().getValue();
            sum += weight;
            if (sum > value) {
                LOGGER.info("Found {} with weight {}", key, weight);
                return key;
            }
        }
        LOGGER.warn("Hit end of loop!");
        return key;
    }

    public double getValueForProblem(String problem) {
        RunningAverageLong ravg = averages.get(problem);
        return ravg.getValue();
    }

    public String findRandom() {
        final double sum = getSum();
        double rnd = RandomUtils.insecure().randomDouble(0, sum);
        LOGGER.info("Sum {}, Rnd {}", sum, rnd);
        return getProblemAt(rnd);
    }

    public Set<Map.Entry<String, RunningAverageLong>> entrySet() {
        return averages.entrySet();
    }
}
