package pl.mickor.typyingtest;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.List;

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

        Button startButton = new Button("Generate test");
        //textFlow = new TextFlow();

        words = new Words(textFlow);
        startButton.setOnAction(actionEvent -> {
            if (languageSelection.getValue() != null && timeSelection.getValue() != null) {
                this.selectedLanguage = languageSelection.getValue();
                int selectedTime = Integer.parseInt(timeSelection.getValue());

                try {
                    //Why the fuck when I press again this bitch adds not clears the hoe ffs
                    textFlow.getChildren().clear();
                    if (!stringOfWords.isEmpty()) {
                        stringOfWords.clear();
                    }
                    stringOfWords = generator.generateTest(selectedLanguage);
                    words.populateTextFlowWithWords(stringOfWords, textFlow);
                    words.initializeOriginalChars();
                    updateTextFlow();
                    startTimer(selectedTime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("WARNING!");
                alert.setHeaderText("Selection box(es) empty!");
                alert.setContentText("One or more selection boxes are empty. Please make selections and try again");
                alert.showAndWait();
            }
        });

        VBox sideBar = new VBox();
        sideBar.getChildren().addAll(language, languageSelection, time, timeSelection, averageWPM, timeLeft, timeLeftLabel, startButton);
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

        initializeTextBindingListener();
    }

    private void initializeTextBindingListener() {
        textBinding.addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                char c = newValue.charAt(newValue.length() - 1);
                char o = newValue.charAt(newValue.length() - 1);
                //Need to finish this off
                if (c == ' ') {
                    words.skipWord();
                } else if (c == '\b') {
                    words.removeChar(o);
                } else {
                    words.addChar(c);
                }
                updateTextFlow();
                words.updateEnteredText();
            }
        });
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
        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1),
                        new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent event) {
                                remainingTime--;
                                timeLeftLabel.setText(String.valueOf(remainingTime));
                                if (remainingTime <= 0) {
                                    timeline.stop();
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
    public String getLanguage(){
        return selectedLanguage;
    }


}