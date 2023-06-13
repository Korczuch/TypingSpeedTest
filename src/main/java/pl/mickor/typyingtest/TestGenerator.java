package pl.mickor.typyingtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestGenerator {




    public List<String> generateTest(String language) throws IOException {
        List<String> randomWords = new ArrayList<>();
        if(!randomWords.isEmpty()){
            randomWords.clear();
        }
        randomWords = getRandomWords(("dictionary/" + language + ".txt"), 30);
        return randomWords;
   }

    private List<String> getRandomWords(String filePath, int wordCount) throws IOException {
        List<String> words = Files.readAllLines(Path.of(filePath));
        List<String> randomWords = new ArrayList<>();
        if(!randomWords.isEmpty()){
            randomWords.clear();
        }
        Random random = new Random();

        for(int i = 0; i < wordCount; i++){
            int randomIndex = random.nextInt(words.size());
            String word = words.get(randomIndex);
            randomWords.add(word);
        }
        return randomWords;
    }


}
