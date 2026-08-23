/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.name;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class NameFormat {

    public static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]+$");

    private NameFormat() {}
    
    public static String validate(String name) {
        Objects.requireNonNull(name, "Name cannot be null");

        if (!VALID_ID.matcher(name).matches()) {
            throw new IllegalStateException(String.format(
                "Name '%s' contains invalid characters. Only letters (A-Z, a-z), numbers (0-9), and hyphens (-) are allowed. ", name)
            );
        }

        return name;
    }
    
    public static String toId(String name) {
        return validate(String.join("", tokenize(name)));
    }
    
    public static String display(String name) {
        return String.join(" ", tokenize(name));
    }
    
    public static String canonical(String name) {
        StringBuilder canonical = new StringBuilder(name.length());

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                canonical.append(Character.toLowerCase(c));
            }
        }

        return canonical.toString();
    }
    
    public static boolean matches(String a, String b) {
        return canonical(a).equals(canonical(b));
    }

    private static List<String> tokenize(String name) {
        Objects.requireNonNull(name, "Name cannot be null");

        List<String> words = new ArrayList<>(4);
        StringBuilder word = new StringBuilder(name.length());

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (!Character.isLetterOrDigit(c)) {
                flush(words, word);
                continue;
            }

            if (!word.isEmpty() && Character.isUpperCase(c) && startsNewWord(word, name, i)) {
                flush(words, word);
            }

            word.append(c);
        }

        flush(words, word);

        if (words.isEmpty()) {
            throw new IllegalStateException("Name '" + name + "' contains no letters or digits.");
        }

        return words;
    }
    
    private static boolean startsNewWord(StringBuilder word, String name, int index) {
        char previous = word.charAt(word.length() - 1);

        if (!Character.isLetter(previous)) {
            return false;
        }

        if (Character.isLowerCase(previous)) {
            return true;
        }

        return index + 1 < name.length() && Character.isLowerCase(name.charAt(index + 1));
    }

    private static void flush(List<String> words, StringBuilder word) {
        if (word.isEmpty()) {
            return;
        }

        words.add(capitalize(word.toString()));
        word.setLength(0);
    }
    
    private static String capitalize(String word) {
        String rest = word.substring(1);

        if (isUpperCase(word)) {
            rest = rest.toLowerCase(Locale.ROOT);
        }

        return Character.toUpperCase(word.charAt(0)) + rest;
    }

    private static boolean isUpperCase(String word) {
        if (word.length() < 2) {
            return false;
        }

        for (int i = 0; i < word.length(); i++) {
            if (!Character.isUpperCase(word.charAt(i))) {
                return false;
            }
        }

        return true;
    }

}
