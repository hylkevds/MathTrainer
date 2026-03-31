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

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

public class ControllerScene implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerScene.class.getName());

    public static final Charset UTF8 = Charset.forName("UTF-8");

    private enum State {
        PAUSED,
        THINKING,
        ANSWERING,
        PROCESSING
    }

    private static final int AVERAGE_COUNT = 10;
    private static final long DEFAULT_ANSWER_TIME = 60_000;
    private static final int MIN_LEFT = 1;
    private static final int MAX_LEFT = 10;
    private static final int MIN_RIGHT = 1;
    private static final int MAX_RIGHT = 10;

    private static final Background BACKGROUND_NONE = null;
    private static final Background BACKGROUND_OK = Background.fill(new Color(0.5, 1, 0.5, 1));
    private static final Background BACKGROUND_WRONG = Background.fill(new Color(1, 0.5, 0.5, 1));
    private static final String TEXT_WRONG = "🗷";
    private static final String TEXT_OK = "🗹";

    private final AveragesManager durations = new AveragesManager(AVERAGE_COUNT, DEFAULT_ANSWER_TIME);
    private final List<CheckMenuItem> menuItemsProblems = new ArrayList<>();

    private State state = State.PAUSED;
    private Instant stateTime;
    private long curDuration;
    private String curProblem = "";
    private int curLeft;
    private int curRight;
    private String curAnswer;

    @FXML
    private MenuBar menuBarMain;
    @FXML
    private Menu menuMain;
    @FXML
    private AnchorPane paneCore;
    @FXML
    private GridPane gridPaneMain;
    @FXML
    private VBox vboxStats;

    @FXML
    private Label lblLeft;
    @FXML
    private Label lblMiddle;
    @FXML
    private Label lblRight;
    @FXML
    private Label lblAnswer;
    @FXML
    private Label lblHint;

    @FXML
    private void actionKeyReleased(KeyEvent event) {
        LOGGER.debug("Key: {}", event);
        switch (state) {
            case PAUSED:
                generateQuestion();
                break;
            case THINKING:
            case ANSWERING:
                recordAnswer(event.getText(), event.getCode());
        }
    }

    private void generateQuestion() {
        state = State.THINKING;
        String oldProblem = curProblem;
        while (curProblem.equals(oldProblem)) {
            curProblem = durations.findRandom();
        }
        String[] split = StringUtils.split(curProblem, 'x');
        if (split.length != 2) {
            LOGGER.error("Bad split for {}", curProblem);
        }
        curLeft = Integer.parseInt(split[0]);
        curRight = Integer.parseInt(split[1]);
        lblLeft.setText(split[0]);
        lblRight.setText(split[1]);
        curAnswer = "";
        lblAnswer.setText(curAnswer);
        stateTime = Instant.now();
    }

    private void recordAnswer(String key, KeyCode code) {
        LOGGER.debug("Key: {} -- {}", key, code);
        if (state != State.ANSWERING) {
            state = State.ANSWERING;
            Instant now = Instant.now();
            if (stateTime == null) {
                curDuration = DEFAULT_ANSWER_TIME;
            } else {
                curDuration = now.toEpochMilli() - stateTime.toEpochMilli();
            }
            curAnswer = "";
        }
        switch (key) {
            case "s":
                setPaused();
                break;

            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                curAnswer += key;
                lblAnswer.setText(curAnswer);
                break;
        }
        switch (code) {
            case BACK_SPACE:
                if (curAnswer.length() > 0) {
                    curAnswer = curAnswer.substring(0, curAnswer.length() - 1);
                    lblAnswer.setText(curAnswer);
                }
                break;

            case ENTER:
                if (curAnswer.length() > 0) {
                    submitAnswer();
                }
        }
    }

    private void submitAnswer() {
        state = State.PROCESSING;
        int answerValue = 0;
        try {
            answerValue = Integer.parseInt(curAnswer);
        } catch (NumberFormatException ex) {
            LOGGER.error("Answer is not an int: {}", curAnswer);
            lblHint.setText("Not a Number");
            lblHint.setBackground(BACKGROUND_WRONG);
        }
        if (answerValue == curLeft * curRight) {
            lblHint.setText(TEXT_OK);
            lblHint.setBackground(BACKGROUND_OK);
            LOGGER.info("Problem: {}. Duration: {}", curProblem, curDuration);
            durations.add(curProblem, curDuration);
            generateQuestion();
        } else {
            lblHint.setText(TEXT_WRONG);
            lblHint.setBackground(BACKGROUND_WRONG);
            state = State.THINKING;
        }
        updateStats();
    }

    private void setPaused() {
        state = State.PAUSED;
        lblLeft.setText("?");
        lblRight.setText("?");
        lblAnswer.setText("...");
        lblHint.setText("Any key to start");
        lblHint.setBackground(BACKGROUND_NONE);
        save();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initialising");
    }

    public void start() {
        for (int right = MIN_RIGHT; right <= MAX_RIGHT; right++) {
            CheckMenuItem menuItem = new CheckMenuItem("? x " + right);
            menuItem.setSelected(true);
            menuItem.setOnAction(e -> readMenuItems());
            menuItemsProblems.add(menuItem);
            menuMain.getItems().add(menuItem);
        }
        initProblems();
        load();
        LOGGER.info("Initialising Controller");
        paneCore.getScene().setOnKeyReleased(this::actionKeyReleased);
        setPaused();
    }

    private void readMenuItems() {
        setPaused();
        initProblems();
    }

    private void initProblems() {
        LOGGER.info("Initialising Averages");
        durations.clearActiveProblems();
        for (int right = MIN_RIGHT; right <= MAX_RIGHT; right++) {
            CheckMenuItem menuItem = menuItemsProblems.get(right - 1);
            if (!menuItem.isSelected()) {
                continue;
            }
            for (int left = MIN_LEFT; left <= MAX_LEFT; left++) {
                final String p = "" + left + 'x' + right;
                durations.create(p);
            }
        }
        updateStats();
    }

    private void updateStats() {
        vboxStats.getChildren().clear();
        for (int right = MIN_RIGHT; right <= MAX_RIGHT; right++) {
            String label = "? x " + right;
            double sum = 0;
            for (int left = MIN_LEFT; left <= MAX_LEFT; left++) {
                sum += durations.getValueForProblem("" + left + "x" + right);
            }
            long avg = Math.round(0.001 * sum / (MAX_LEFT - MIN_LEFT));
            label = label + ": " + avg;
            vboxStats.getChildren().add(new Label(label));
        }

    }

    private void load() {
        File file = new File("state.json");
        if (file.exists()) {
            DataFile data;
            try {
                data = SimpleJsonMapper.getSimpleObjectMapper().readValue(file, DataFile.class);
            } catch (JacksonException ex) {
                LOGGER.error("Failed to read.", ex);
                return;
            }
            if (data.selectedProblems != null) {
                int cnt = Math.max(data.selectedProblems.size(), menuItemsProblems.size());
                for (int idx = 0; idx < cnt; idx++) {
                    menuItemsProblems.get(idx).setSelected(data.selectedProblems.get(idx));
                }
            }
            for (var entry : data.weights.entrySet()) {
                String key = entry.getKey();
                List<Long> values = entry.getValue();
                for (Long value : values) {
                    durations.add(key, value);
                }
            }
        }
        readMenuItems();
    }

    private void save() {
        DataFile data = new DataFile();
        data.selectedProblems = new ArrayList<>();
        for (int right = MIN_RIGHT; right <= MAX_RIGHT; right++) {
            CheckMenuItem menuItem = menuItemsProblems.get(right - 1);
            data.selectedProblems.add(menuItem.isSelected());
        }

        data.weights = new TreeMap<>();
        for (var entry : durations.entrySet()) {
            String key = entry.getKey();
            RunningAverageLong values = entry.getValue();
            data.weights.put(key, values.getValues());
        }
        File file = new File("state.json");
        SimpleJsonMapper.getSimpleObjectMapper().writeValue(file, data);
    }

    private static class DataFile {

        public List<Boolean> selectedProblems;
        public Map<String, List<Long>> weights;

    }

}
