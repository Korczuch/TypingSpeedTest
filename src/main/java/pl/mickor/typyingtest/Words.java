package pl.mickor.typyingtest;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class Words {
    private TextFlow originalTextFlow;
    private TextFlow enteredTextFlow;
    TestGenerator generator = new TestGenerator();

    private List<ClassifiedChar> originalChars;
    private List<ClassifiedChar> enteredChars;
    public ArrayList<Character> currentlyEnteredWord = new ArrayList<>();
    public ArrayList<String> sourceWords = new ArrayList<>();
    //If we generate 30 new words, we will need to have a place to save all the source words we have, for when
    //we generate the text file
    public ArrayList<String> allSourceWords = new ArrayList<>();
    public List<String> newSourceWords = new ArrayList<>();
    public String sourceWord = new String();
    public ArrayList<Character> enteredWord = new ArrayList<>();
    public ArrayList<ClassifiedChar> classifiedCharacters = new ArrayList<>();
    public ArrayList<ClassifiedChar> allClassifiedChars = new ArrayList<>();
    public int correctChars = 0;
    public int incorrectChars = 0;
    public int extraChars = 0;
    public int skippedChars = 0;
    public int indexTextFlow = 0;
    public int indexCharacter = 0;
    public ArrayList<Double> secondsPerWord = new ArrayList<>();
    public boolean wordCompleted = false;
    private boolean animationStarted = false;
//    private Duration wordStartTime;
    private boolean wordInProgress;
    private Double startTime = Double.valueOf(System.currentTimeMillis());
    private Double endTime;
    private Double duration;
    private boolean wordStarted = false;
    public ArrayList<Double> wordsPerMinute = new ArrayList<>();
    double averageWPM = 0;
    String selectedLanguage;

    public Words(TextFlow originalTextFlow) {
        this.originalTextFlow = originalTextFlow;
        this.enteredTextFlow = new TextFlow();
        this.originalChars = new ArrayList<>();
        this.enteredChars = new ArrayList<>();

        initializeOriginalChars();
    }

    public ClassifiedChar addChar(char c) {
        ClassifiedChar classifiedChar;
        sourceWord = sourceWords.get(indexTextFlow);
        if (!wordStarted) {
            startTime = Double.valueOf(System.currentTimeMillis());
            wordStarted = true;
        }

        if (wordCompleted == false) {

            classifiedChar = classifiedCharacters.get(enteredWord.size());

            if (c == sourceWord.charAt(currentlyEnteredWord.size())) {
                classifiedChar.classification = CharClassification.CORRECT;
                correctChars++;

            } else if (currentlyEnteredWord.size() + 1 < sourceWord.length() && c ==
                    sourceWord.charAt(currentlyEnteredWord.size() + 1)) {
                if (classifiedChar.classification != CharClassification.INCORRECT) {
                    classifiedChar.classification = CharClassification.SKIPPED_CHAR;
                    skippedChars++;
                }
                // Move to evaluating the character we got correct
                currentlyEnteredWord.add('$');

                enteredWord.add(sourceWord.charAt(currentlyEnteredWord.size()));
                classifiedChar = classifiedCharacters.get(enteredWord.size());

                classifiedChar.classification = CharClassification.CORRECT;
                correctChars++;
            } else {
                classifiedChar.classification = CharClassification.INCORRECT;
                incorrectChars++;
            }
        } else {
            classifiedChar = new ClassifiedChar(c, CharClassification.EXTRA_CHAR);
            classifiedCharacters.add(enteredWord.size(), classifiedChar);


            extraChars++;
            System.out.println(c);
        }
        currentlyEnteredWord.add(c);
        enteredWord.add(c);

        if (!wordInProgress) {
//            wordStartTime = Duration.ZERO;
            wordInProgress = true;
        }

        updateEnteredText();
        checkWordCompleted();
//        System.out.println("Source word " + sourceWord.length());
//        System.out.println("current word: " + currentlyEnteredWord.size());
//        System.out.println("skipped" + skippedChars);
//        System.out.println("incorrect " + incorrectChars);
//        System.out.println("correct " + correctChars);
//        System.out.println("currently entered word: " + currentlyEnteredWord.stream().map(x -> x.toString()).collect(Collectors.joining()));
//        System.out.println("entered word: " + enteredWord.stream().map(x -> x.toString()).collect(Collectors.joining()));
//        System.out.println(wordStarted);
//        System.out.println(startTime);
//        System.out.println(endTime);
        return classifiedChar;
    }

    public void populateTextFlowWithWords(List<String> words, TextFlow textFlow) {
        textFlow.getChildren().clear();

        for (String word : words) {
            Text wordText = new Text(word + " ");
            textFlow.getChildren().add(wordText);
            String wordToAdd = wordText.getText();
            allSourceWords.add(wordToAdd);
        }
        sourceWords = (ArrayList<String>) words;
    }

    void initializeOriginalChars() {
        List<ClassifiedChar> originalChars = new ArrayList<>();
        List<String> words = extractWordsFromTextFlow(originalTextFlow);

        for (String word : words) {
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                ClassifiedChar classifiedChar = new ClassifiedChar(c, CharClassification.MISSING_CHAR);
                originalChars.add(classifiedChar);
            }

            if (!word.isEmpty()) {
                char space = ' ';
                ClassifiedChar classifiedChar = new ClassifiedChar(space, CharClassification.MISSING_CHAR);
                originalChars.add(classifiedChar);
            }
        }
        this.originalChars = originalChars;
        classifiedCharacters.addAll(originalChars);
        allClassifiedChars.addAll(originalChars);
        updateEnteredText();
    }

    private List<String> extractWordsFromTextFlow(TextFlow textFlow) {
        List<String> words = new ArrayList<>();
        StringBuilder wordBuilder = new StringBuilder();

        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                Text textNode = (Text) node;
                String text = textNode.getText();
                if (!text.isEmpty()) {
                    if (text.charAt(0) != ' ') {
                        wordBuilder.append(text);
                    } else {
                        words.add(wordBuilder.toString());
                        wordBuilder.setLength(0);
                    }
                }
            }
        }

        if (wordBuilder.length() > 0) {
            words.add(wordBuilder.toString());
        }

        return words;
    }

    public void skipWord() throws IOException {
        wordStarted = false;
        endTime = Double.valueOf(System.currentTimeMillis());
        storeTimeForWord();
        System.out.println(sourceWord);
        int size = enteredWord.size();
        for (int i = 0; i < sourceWord.length() - currentlyEnteredWord.size(); i++) {
            classifiedCharacters.get(size + i).classification = CharClassification.SKIPPED_CHAR;
            enteredWord.add('$');
            skippedChars++;
        }

        wordCompleted = false;
        moveToNextWord();
    }

    private void moveToNextWord() throws IOException {
        boolean movingToNewWords = false;
        if(indexTextFlow >= 29){
            movingToNewWords = true;
        }
        System.out.println(wordCompleted);
        System.out.println(indexTextFlow);
        newParagraph();
        currentlyEnteredWord.clear();
        if(!movingToNewWords) {
            enteredWord.add(' ');
            indexTextFlow++;
        }
        sourceWord = sourceWords.get(indexTextFlow);

    }

    Text colourCharacters(ClassifiedChar c) {
        Text text = new Text();
        switch (c.classification) {
            case CORRECT:
                text.setFill(Color.GREEN);
                break;
            case INCORRECT:
                text.setFill(Color.RED);
                break;
            case MISSING_CHAR:
                text.setFill(Color.GRAY);
                break;
            case EXTRA_CHAR:
                text.setFill(Color.ORANGE);
                break;
            case SKIPPED_CHAR:
                text.setFill(Color.BLACK);
                break;
        }
        text.setText(String.valueOf(c.character));
        return text;
    }

    public void updateEnteredText() {
        enteredTextFlow.getChildren().clear();
        for (ClassifiedChar classifiedChar : classifiedCharacters) {
            Text text = colourCharacters(classifiedChar);
            text.setStyle("-fx-font-size: 25px;");
            enteredTextFlow.getChildren().add(text);
        }
        if (wordCompleted && wordInProgress) {
//            secondsPerWord.add(wordStartTime.toSeconds());
            wordInProgress = false;
        }
        waveAnimation(enteredTextFlow);

    }

    public void displayEnteredText() {
        originalTextFlow.getChildren().clear();
        originalTextFlow.getChildren().addAll(enteredTextFlow.getChildren());
    }

    public TextFlow getEnteredTextFlow() {
        return enteredTextFlow;
    }

    public TextFlow getOriginalTextFlow() {
        return originalTextFlow;
    }


    public void removeChar() {
        if (!enteredWord.isEmpty() && !currentlyEnteredWord.isEmpty()) {
            int lastEnteredIndex = currentlyEnteredWord.size() - 1;
            int lastEnteredCharIndex = enteredWord.size() - 1;
            System.out.println("Removed");

            enteredWord.remove(lastEnteredCharIndex);
            currentlyEnteredWord.remove(lastEnteredIndex);
            for (int i = lastEnteredCharIndex; i >= 0; i--) {
                ClassifiedChar classifiedChar = classifiedCharacters.get(i);
                if(classifiedChar.classification == CharClassification.INCORRECT){
                    incorrectChars--;
                }
                if(classifiedChar.classification == CharClassification.SKIPPED_CHAR){
                    skippedChars--;
                }
                if(classifiedChar.classification == CharClassification.CORRECT){
                    correctChars--;
                }
                if(classifiedChar.classification == CharClassification.EXTRA_CHAR){
                    extraChars--;
                    classifiedCharacters.remove(i);
                }
                if (classifiedChar.classification != CharClassification.MISSING_CHAR) {
                    classifiedChar.classification = CharClassification.MISSING_CHAR;
                    break;
                }

            }

//            if(enteredWord.get(lastEnteredCharIndex-1) == '$'){
//                enteredWord.remove(lastEnteredCharIndex-1);
//                currentlyEnteredWord.remove(lastEnteredIndex-1);
//            }
        }
        wordCompleted = false;
    }

    //if no more characters return false
    public boolean getCurrentCharacter() {
        int currentCharIndex = currentlyEnteredWord.size();
        if (currentCharIndex < sourceWord.length() - 1) {
            return true;
        }
        return false;
    }

    public static void waveAnimation(TextFlow textFlow) {
        SequentialTransition sequentialTransition = new SequentialTransition();
        for (Node node : textFlow.getChildren()) {
            TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.2), node);
            translateTransition.setFromY(0);
            translateTransition.setToY(-10);
            translateTransition.setAutoReverse(true);
            translateTransition.setCycleCount(2);

            PauseTransition pauseTransition = new PauseTransition(Duration.seconds(0.05));

            sequentialTransition.getChildren().addAll(translateTransition, pauseTransition);
        }
        sequentialTransition.setCycleCount(Timeline.INDEFINITE);
        sequentialTransition.play();
    }

    public void checkWordCompleted() {
        if ((currentlyEnteredWord.size()) < sourceWord.length()) {
            wordCompleted = false;
        } else {
            wordCompleted = true;
//            wordStartTime = Duration.ZERO;
        }
    }

    //generate a new string of 30 words, and set them as the textFlow (same thing that's done at the start...)
    //We need to generate a new string, and pass it through the methods to generate textFlow
    //We need to get language from MainWindow
    public void newParagraph() throws IOException {
        if (indexTextFlow >= 29) {
            indexTextFlow = 0;

            newSourceWords = generator.generateTest(selectedLanguage);
            enteredWord.clear();
            currentlyEnteredWord.clear();
            System.out.println("ogp"+originalTextFlow.getChildren().size());
            System.out.println("efp"+enteredTextFlow.getChildren().size());
            currentlyEnteredWord.clear();
            enteredTextFlow.getChildren().clear();
            originalTextFlow.getChildren().clear();
            classifiedCharacters.clear();
            System.out.println("og"+originalTextFlow.getChildren().size());
            System.out.println("ef"+enteredTextFlow.getChildren().size());
            populateTextFlowWithWords(newSourceWords, originalTextFlow);
            initializeOriginalChars();
            sourceWord = sourceWords.get(indexTextFlow);
            indexTextFlow = 0;
            enteredWord.clear();
            currentlyEnteredWord.clear();
        }
    }

    private void storeTimeForWord(){
        double timeForWord = endTime - startTime;
        double secondsForWord = timeForWord/1000;
        secondsPerWord.add(secondsForWord);
        System.out.println(timeForWord);
        System.out.println(secondsForWord);
    }

    public void calculateWPM() {
        for(int i = 0; i < secondsPerWord.size(); i++){
            double WPM;
            WPM = 60/secondsPerWord.get(i);
            wordsPerMinute.add(WPM);
        }
    }

    public void calculateAverageWPM(){
        long sum = 0;
        for(Double wpm : wordsPerMinute){
            sum += wpm;
        }
        if(!wordsPerMinute.isEmpty()) {
            averageWPM = sum / wordsPerMinute.size();
        }else
        {averageWPM = 0;}
    }

    public void clearOriginalTextFlow() {
        originalTextFlow.getChildren().clear();
    }

    //Generate a word file with the entered words
    public void generateWordFile() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
        String fileName = now.format(formatter);

        String folderPath = "Results";

        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, fileName + ".txt");
            FileWriter writer = new FileWriter(file);

            for (int i = 0; i < wordsPerMinute.size(); i++) {
                String word = allSourceWords.get(i);
                double wpm = wordsPerMinute.get(i);

                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                symbols.setDecimalSeparator('.');
                DecimalFormat decimalFormat = new DecimalFormat("#.##", symbols);
                double roundedWPM = Double.parseDouble(decimalFormat.format(wpm));


                String line = word + " --> " + roundedWPM + "wpm";
                writer.write(line);
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearEnteredTextFlow(){
        enteredTextFlow.getChildren().clear();
    }



}