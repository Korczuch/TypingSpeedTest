package pl.mickor.typyingtest;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainWindow extends Application {

    private Words words;

    private SequentialTransition waveAnimation;

    TestGenerator generator = new TestGenerator();

    private static final String DICTIONARY_FOLDER = "dictionary";

    private StringBinding textBinding;
    private TextFlow textFlow = new TextFlow();

    private Label timeLeftLabel;
    private int remainingTime;
    public static List<String> stringOfWords = new ArrayList<>();
    private String selectedLanguage;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private Timeline timeline = new Timeline();
    private Button startButton = new Button("Generate test");


    @Override
    public void start(Stage stage) throws IOException {

        TextField textField = new TextField();
        BorderPane root = new BorderPane();

        Label language = new Label("Language:");
        Label time = new Label("Time (in seconds):");
        Label averageWPM = new Label("Average WPM: ");
        Label timeLeft = new Label("Time Left:");
        timeLeftLabel = new Label();

        ComboBox<String> languageSelection = new ComboBox<>();

        languageSelection.getItems().addAll(getTextFileNames());
        languageSelection.setPromptText("Select Language");

        ComboBox<String> timeSelection = new ComboBox<>();
        timeSelection.getItems().addAll("15", "20", "45", "60", "90", "120", "300");
        timeSelection.setPromptText("Select time for test");

        //Button startButton = new Button("Generate test");
        //textFlow = new TextFlow();

        //this part is gonna get deleted later
        Button testButton = new Button("End test");

        testButton.setOnAction(actionEvent -> {
            transitionToFinishScene();
        });

        words = new Words(textFlow);
        startButton.setOnAction(actionEvent -> {
            if (languageSelection.getValue() != null && timeSelection.getValue() != null) {
                words.selectedLanguage = languageSelection.getValue();
                int selectedTime = Integer.parseInt(timeSelection.getValue());

                try {
                    //Why the fuck when I press again this bitch adds not clears the hoe ffs
                    textFlow.getChildren().clear();
                    if(!words.classifiedCharacters.isEmpty()){
                        words.classifiedCharacters.clear();
                    }
                    if (!stringOfWords.isEmpty()) {
                        stringOfWords.clear();
                    }
                    if (!textFlow.getChildren().isEmpty()) {
                        textFlow.getChildren().clear();
                    }
                    textField.clear();
                    words.enteredWord.clear();
                    words.currentlyEnteredWord.clear();
                    words.indexTextFlow = 0;
                    words.clearOriginalTextFlow();
                    words.clearEnteredTextFlow();
                    stringOfWords = generator.generateTest(words.selectedLanguage);
                    words.populateTextFlowWithWords(stringOfWords, textFlow);
                    words.initializeOriginalChars();
                    updateTextFlow();
                    startTimer(selectedTime);
                    animateLabelColors(language);
                    animateLabelColors(time);
                    animateLabelColors(timeLeft);
                    isStarted = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("WARNING!");
                alert.setHeaderText("Selection box(es) empty!");
                alert.setContentText("One or more selection boxes are empty. Please make selections and try again");
                alert.showAndWait();
                spinLabel(language);
                spinLabel(time);
                spinLabel(timeLeft);
            }
        });

        VBox sideBar = new VBox();
        sideBar.getChildren().addAll(language, languageSelection, time, timeSelection, averageWPM, timeLeft, timeLeftLabel, startButton, testButton);
        sideBar.setMinWidth(300);

        sideBar.setSpacing(10);

        sideBar.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(sideBar.getMinWidth(), root.getWidth() * 0.2), // Adjust the multiplier as needed
                root.widthProperty()
        ));

        sideBar.prefHeightProperty().bind(root.heightProperty());

        sideBar.setStyle("-fx-background-color: lightslategray;");
        language.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        time.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        averageWPM.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        timeLeft.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        timeLeftLabel.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        root.setStyle("-fx-background-color: dimgray;");

        root.setLeft(sideBar);

        textFlow.setStyle("-fx-font-size: 25px;");

        words.updateEnteredText();
        root.setCenter(words.getEnteredTextFlow());

        textField.setOpacity(50);
        root.setBottom(textField);

        textBinding = Bindings.createStringBinding(() -> textField.getText(), textField.textProperty());

        Scene scene = new Scene(root, 1400, 600);
        stage.setTitle("S26625 typing test");
        stage.setScene(scene);
        stage.show();

        initializeTextBindingListener(scene);
    }

    //Another idea for removing characters, to avoid the last char after pressing backspace being considered a new
    //char, is to make it so that each time we press a character, we clear the textbinding so that it doesn't know the
    //previous characters, and with that, have a seperate method that when backspace is pressed, we call
    //words.removeChar()

    private void initializeTextBindingListener(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (isStarted) {
                if (event.getCode() == KeyCode.TAB && event.isControlDown()) {
                    handleTabEnterShortcut();
                } else if (event.getCode() == KeyCode.P && event.isShiftDown() && event.isControlDown()) {
                    handleCtrlShiftPShortcut();
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    handleEscapeShortcut();
                }
            }
        });

        textBinding.addListener((observable, oldValue, newValue) -> {
            if (!isPaused) {
                if (!newValue.isEmpty()) {
                    char c = newValue.charAt(newValue.length() - 1);
                    char o = newValue.charAt(newValue.length() - 1);
                    //Need to finish this off
                    if (c == ' ') {
                        try {
                            words.skipWord();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (c == '\b') {
                        words.removeChar();
                    } else {
                        words.addChar(c);
                    }
                    updateTextFlow();
                    words.updateEnteredText();
                }
            }
        });
    }

    //this one don't worky.....
    private void handleTabEnterShortcut() {
        System.out.println("TAB + ENTER pressed");
        startButton.fire();
    }

    private void handleCtrlShiftPShortcut() {
        // Handle CTRL + SHIFT + P shortcut
        if (!isPaused) {
            isPaused = true;
            timeline.pause();
        } else {
            isPaused = false;
            timeline.play();
        }
    }

    private void handleEscapeShortcut() {
        System.out.println("ESC pressed");
        transitionToFinishScene();
    }

    private void updateTextFlow() {
        textFlow.getChildren().clear();
        for (ClassifiedChar classifiedChar : words.classifiedCharacters) {
            Text textNode = words.colourCharacters(classifiedChar);
            textFlow.getChildren().add(textNode);
        }

    }

    private String[] getTextFileNames() {
        File folder = new File(DICTIONARY_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            fileNames[i] = fileName.substring(0, fileName.lastIndexOf(".txt"));
        }
        return fileNames;
    }

    private void startTimer(int selectedTime) {
        remainingTime = selectedTime;
        timeLeftLabel.setText(String.valueOf(remainingTime));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1),
                        new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent event) {
                                remainingTime--;
                                timeLeftLabel.setText(String.valueOf(remainingTime));
                                if (remainingTime <= 0) {
                                    timeline.stop();
                                    transitionToFinishScene();
                                }
                                if (remainingTime <= 5) {
                                    timeLeftLabel.setTextFill(Color.RED);
                                } else {
                                    timeLeftLabel.setTextFill(Color.ORANGE);
                                }
                            }
                        }));
        timeline.play();
    }

    public String getLanguage() {
        return selectedLanguage;
    }

    private void animateLabelColors(Label label) {
        // Create a timeline with a duration of 10 seconds
        Timeline timeline = new Timeline();

        // Generate a new random color every 1 second
        for (int i = 0; i < 10; i++) {
            KeyFrame keyFrame = new KeyFrame(Duration.seconds(i + 0.5),
                    event -> label.setTextFill(Color.web(getRandomColor())));
            timeline.getKeyFrames().add(keyFrame);
        }

        // Set the cycle count to 1 to play the animation only once
        timeline.setCycleCount(1);

        // When the animation finishes, reset the label's color
        timeline.setOnFinished(event -> label.setTextFill(Color.ORANGE));

        // Start the animation
        timeline.play();
    }

    private String getRandomColor() {
        // Generate random RGB values
        int red = (int) (Math.random() * 256);
        int green = (int) (Math.random() * 256);
        int blue = (int) (Math.random() * 256);

        // Format the RGB values as a CSS color string
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    public static void spinLabel(Label label) {
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(5), label);
        rotateTransition.setByAngle(360); // Rotate 360 degrees
        rotateTransition.setCycleCount(1); // Perform one full rotation
        rotateTransition.setAutoReverse(false); // Do not reverse the animation

        rotateTransition.play();
    }


    private void transitionToFinishScene() {
        words.calculateWPM();
        words.calculateAverageWPM();
        words.generateWordFile();

        Stage stage = new Stage();
        BorderPane newRoot = new BorderPane();
        timeline.pause();
        Scene newScene = new Scene(newRoot, 800, 600);
        stage.setScene(newScene);

        int totalCharsTyped = words.correctChars + words.incorrectChars + words.extraChars + words.skippedChars;
        double accuracy = ((double) words.correctChars / totalCharsTyped) * 100;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#.##", symbols);
        double roundedAccuracy = Double.parseDouble(decimalFormat.format(accuracy));

        Label correctChars = new Label("Correct Chars: " + words.correctChars);
        Label incorrectChars = new Label("Incorrect Chars: " + words.incorrectChars);
        Label extraChars = new Label("Extra Chars: " + words.extraChars);
        Label missedChars = new Label("Missed/Skipped Chars: " + words.skippedChars);
        Label totalChars = new Label("Total: " + totalCharsTyped);
        Label accuracyPercent = new Label("acc: " + roundedAccuracy + "%");
        Label averageWPM = new Label("Average WPM: " + words.averageWPM);

        VBox sideBar = new VBox();
        sideBar.getChildren().addAll(totalChars, correctChars, incorrectChars, extraChars, missedChars,
                accuracyPercent, averageWPM);
        sideBar.setMinWidth(300);

        sideBar.setSpacing(10);

        sideBar.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(sideBar.getMinWidth(), newRoot.getWidth() * 0.2), // Adjust the multiplier as needed
                newRoot.widthProperty()
        ));

        sideBar.prefHeightProperty().bind(newRoot.heightProperty());

        sideBar.setStyle("-fx-background-color: lightslategray;");
        correctChars.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        incorrectChars.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        extraChars.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        missedChars.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        totalChars.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        accuracyPercent.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");
        averageWPM.setStyle("-fx-font-size: 25px; -fx-text-fill: orange;");

        newRoot.setStyle("-fx-background-color: dimgray;");

        newRoot.setLeft(sideBar);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Seconds");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("WPM");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("WPM per second");

        lineChart.setStyle("-fx-background-color: dimgray;");
        lineChart.lookup(".chart-plot-background")
                .setStyle("-fx-background-color: dimgray;");
        lineChart.lookup(".axis")
                .setStyle("-fx-text-fill: white;");
        lineChart.lookup(".axis-label")
                .setStyle("-fx-text-fill: white;");


        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Word Entered");

        ObservableList<XYChart.Data<Number, Number>> dataPoints = series.getData();
        for (int i = 0; i < words.wordsPerMinute.size(); i++) {
            double seconds = calculateTotalSeconds(i);
            double wpm = words.wordsPerMinute.get(i);
            dataPoints.add(new XYChart.Data<>(seconds, wpm));
        }
        lineChart.getData().add(series);

        newRoot.setCenter(lineChart);

        stage.show();
    }

    private double calculateTotalSeconds(int index) {
        double totalSeconds = 0.0;
        for (int i = 0; i <= index; i++) {
            totalSeconds += words.secondsPerWord.get(i);
        }
        return totalSeconds;
    }

    public void restartTest(){

    }
}