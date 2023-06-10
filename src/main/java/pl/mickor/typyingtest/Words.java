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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Words {
    private TextFlow originalTextFlow;
    private TextFlow enteredTextFlow;
    TestGenerator generator = new TestGenerator();

    private List<ClassifiedChar> originalChars;
    private List<ClassifiedChar> enteredChars;
    public ArrayList<Character> currentlyEnteredWord = new ArrayList<>();
    public ArrayList<String> sourceWords = new ArrayList<>();
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
    public ArrayList<Double> timePerWord = new ArrayList<>();
    public boolean wordCompleted = false;
    private boolean animationStarted = false;
    private Duration wordStartTime;
    private boolean wordInProgress;

    public Words(TextFlow originalTextFlow) {
        this.originalTextFlow = originalTextFlow;
        this.enteredTextFlow = new TextFlow();
        this.originalChars = new ArrayList<>();
        this.enteredChars = new ArrayList<>();

        initializeOriginalChars();
    }
    /*
    Each time we type we should add the character to some sort of list, but when we press space to go to the next word
    that character should be reset back to 0.
    But the list that supplies textflow should keep the original list of all the added chars and be based off of that.
    Which should be done by enteredChars but is fucking shit up idk why jfc

     */


    public ClassifiedChar addChar(char c) {
        ClassifiedChar classifiedChar;
        sourceWord = sourceWords.get(indexTextFlow);

        if (wordCompleted == false) {
            classifiedChar = classifiedCharacters.get(enteredWord.size());
            if (c == sourceWord.charAt(currentlyEnteredWord.size())) {
                classifiedChar.classification = CharClassification.CORRECT;
                correctChars++;

                // Check if there is a subsequent character in the word
                if (currentlyEnteredWord.size() + 1 < sourceWord.length()) {
                    char nextChar = sourceWord.charAt(currentlyEnteredWord.size() + 1);

                    // Move to evaluating the subsequent character
                    currentlyEnteredWord.add(nextChar);
                    enteredWord.add(nextChar);
                    classifiedChar = classifiedCharacters.get(enteredWord.size() - 1);
                    classifiedChar.classification = CharClassification.CORRECT;
                    correctChars++;
                }
            } else if (c == sourceWord.charAt(currentlyEnteredWord.size() + 1)) {
                if (classifiedChar.classification != CharClassification.INCORRECT) {
                    classifiedChar.classification = CharClassification.SKIPPED_CHAR;
                    skippedChars++;
                }

                // Move to evaluating the character we got correct
                currentlyEnteredWord.add(sourceWord.charAt(currentlyEnteredWord.size()));
                currentlyEnteredWord.add(c);
                enteredWord.add(sourceWord.charAt(currentlyEnteredWord.size()));
                enteredWord.add(c);
                classifiedChar = classifiedCharacters.get(enteredWord.size() - 1);
                classifiedChar.classification = CharClassification.CORRECT;
                correctChars++;
            } else {
                classifiedChar.classification = CharClassification.INCORRECT;
                incorrectChars++;
            }
        } else {
            classifiedChar = new ClassifiedChar(c, CharClassification.EXTRA_CHAR);
            classifiedCharacters.add(enteredWord.size() + 1, classifiedChar);
            extraChars++;
            System.out.println(c);
        }

        if (!wordInProgress){
            wordStartTime = Duration.ZERO;
            wordInProgress = true;
        }

        updateEnteredText();
        checkWordCompleted();
        return classifiedChar;
    }

    public void populateTextFlowWithWords(List<String> words, TextFlow textFlow) {
        textFlow.getChildren().clear();

        for (String word : words) {
            Text wordText = new Text(word + " ");
            textFlow.getChildren().add(wordText);
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
//I think we should also add the skipped word (i) to entered word?... Think so chief.
    //the logic for skipWord ain't right
    //it needs to check if next char in source word is a space, if it is, then we aren't within current word,
    //if it ain't a space, then we are not within current word and need to classify the characters as skipped, and
    //add them to the classifiedCharacters arraylist, all the way to the space, once we get to the space,
    //we call move to next word
    //skipping characters in a word needs to also add them to enteredWord

    //there is now a little fucky wucky where the word is the right length after the first word but later missing a space
    //need to llok at that, potentially if original logic is right, this will not be an issue



    public void skipWord() {
        boolean withinCurrentWord;

        if(currentlyEnteredWord.size() < sourceWord.length()){
            withinCurrentWord = true;
        }else{
            withinCurrentWord = false;
        }

        for (ClassifiedChar classifiedChar : classifiedCharacters) {
            if (withinCurrentWord && classifiedChar.classification == CharClassification.MISSING_CHAR) {
                classifiedChar.classification = CharClassification.SKIPPED_CHAR;
                skippedChars++;
                //need to add the skipped characters here to entered word...
                enteredWord.add('a');

            }

            if (classifiedChar.character == ' ') {
                withinCurrentWord = false;
            }
        }
        wordInProgress = false;
        moveToNextWord();
    }

    private void moveToNextWord() {
        currentlyEnteredWord.clear();
        //enteredWord.add(' ');
        enteredWord.add(' ');
        //if (originalChars.size() > classifiedCharacters.size()) {
            int nextWordStartIndex = classifiedCharacters.size();
            for (int i = nextWordStartIndex; i < originalChars.size(); i++) {
                ClassifiedChar classifiedChar = originalChars.get(i);
                classifiedCharacters.add(classifiedChar);
                //this shit ain't working, and the character aren't getting added
                //probbably because classifiedChar isn't a ring? fuck me if I know?
                enteredWord.add(classifiedChar.getCharacter());
           // }
        }
        indexTextFlow++;
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
            timePerWord.add(wordStartTime.toSeconds());
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
        if (!classifiedCharacters.isEmpty()) {
            int lastEnteredIndex = currentlyEnteredWord.size() - 1;
            int lastEnteredCharIndex = enteredWord.size() - 1;
            char lastEnteredChar = enteredWord.get(lastEnteredCharIndex);

            // Find the last character with a classification and mark it as missing
            for (int i = lastEnteredIndex; i >= 0; i--) {
                ClassifiedChar classifiedChar = classifiedCharacters.get(i);
                if (classifiedChar.classification != CharClassification.MISSING_CHAR) {
                    classifiedChar.classification = CharClassification.MISSING_CHAR;
                    break;
                }
            }

            classifiedCharacters.remove(classifiedCharacters.size() - 1);
            currentlyEnteredWord.remove(lastEnteredIndex);
            enteredWord.remove(lastEnteredCharIndex);
        }
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
            wordStartTime = Duration.ZERO;
        }
    }
    //generate a new string of 30 words, and set them as the textFlow (same thing that's done at the start...)
    //We need to generate a new string, and pass it through the methods to generate textFlow
    //We need to get language from MainWindow
    public void newParagraph() throws IOException {
        if(indexTextFlow >= 29 && wordCompleted){
            indexTextFlow = 0;
            MainWindow mainWindow = new MainWindow();

            newSourceWords = generator.generateTest(mainWindow.getLanguage());


        }
    }
/*
WPM calculations...
Average WPM calculation is easy. We take the amount of words we typed and the time it took. Since our times are
set by us, we either multiply by x or divide by x to get words per minute.

For current WPM (calculte for each word), we have the times it took to complete a word stored. We take that time (y).
60/y, which gives us the the WPM speed for that word, and we store it in a list. Having both the time it took to
complete the word and the WPM, we can display it, where the x axis (seconds), it added to each previous time, so that
we can display it, and set the corresponding y for it.

The thing we're missing is time between words.... hmmm

Add another list that stores the time elapsed from start to write that word??
 */



public void clearOriginalTextFlow(){
    originalTextFlow.getChildren().clear();
}

}