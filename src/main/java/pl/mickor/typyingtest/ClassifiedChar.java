package pl.mickor.typyingtest;

public class ClassifiedChar {
    char character;
    CharClassification classification;

    public ClassifiedChar(char character, CharClassification classification) {
        this.character = character;
        this.classification = classification;
    }
}

enum CharClassification {
    CORRECT,
    INCORRECT,
    MISSING_CHAR,
    EXTRA_CHAR,
    SKIPPED_CHAR
}
