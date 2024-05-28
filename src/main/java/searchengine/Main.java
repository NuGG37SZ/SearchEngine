package searchengine;

import searchengine.services.WordLemmatizer;

import java.io.IOException;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        String text = "Макароны с сыром самые лучшее блюдо всех времен и народов";
        try {
            WordLemmatizer wordLemmatizer = WordLemmatizer.getInstance();
            Set<String> strings = wordLemmatizer.getLemmaSet(text);
            strings.stream().forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
