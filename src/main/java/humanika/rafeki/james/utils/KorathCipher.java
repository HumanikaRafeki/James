package humanika.rafeki.james.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;

public class KorathCipher {
    private static final Map<Character, Character> toExile = createExileMap();
    private static final Map<Character, Character> toEfreti = createEfretiMap();
    private static final Map<Character, Character> fromExile = reverseMap(toExile);
    private static final Map<Character, Character> fromEfreti = reverseMap(toEfreti);
    private static final Set<Character> indonesianLetters = stringToSet("abcdefghijklmnopqrstuvwxyz");
    private static final Set<Character> indonesianPlus = stringToSet("abcdefghijklmnopqrstuvwxyz'");

    private static final Locale locale = new Locale("id");

    private String english;
    private String indonesian;
    private String exile;
    private String efreti;

    public KorathCipher(@Nullable String english, @Nullable String indonesian, @Nullable String exile, @Nullable String efreti) {
        this.english = english;
        this.indonesian = indonesian;
        this.exile = exile;
        this.efreti = efreti;
    }

    public String getEnglish() {
        return english;
    }

    public String getIndonesian() {
        return indonesian;
    }

    public String getExile() {
        return exile;
    }

    public String getEfreti() {
        return efreti;
    }

    /** Applies the reverse cipher to efreti, producing indonesian field */
    public KorathCipher unefreti() {
        char[][] reversedChars = reverseStrings(efreti.toLowerCase(locale), indonesianLetters);
        char[][] undone = new char[reversedChars.length][];
        applyCipher(reversedChars, undone, fromEfreti);
        indonesian = join(undone);
        return this;
    }

    /** Applies the reverse cipher to exile, producing indonesian field */
    public KorathCipher unexile() {
        char[][] reversedChars = reverseStrings(exile.toLowerCase(locale), indonesianPlus);
        char[][] undone = new char[reversedChars.length][];
        applyCipher(reversedChars, undone, fromExile);
        indonesian = join(undone);
        return this;
    }

    /** Applies cipher steps to indonesian, creating exile and efreti fields.
     * @returns this */
    public KorathCipher indokorath() {
        if(indonesian != null)
            cipherSteps();
        else {
            exile = null;
            efreti = null;
        }
        return this;
    }

    /** Applies cipher steps to indonesian, creating exile and efreti fields. Assumes that indonesian is non-null.
     * @returns this */
    private void cipherSteps() {
        char[][] reversedChars = reverseStrings(indonesian.toLowerCase(locale), indonesianLetters);
        char[][] korath = new char[reversedChars.length][];
        applyCipher(reversedChars, korath, toExile);
        exile = join(korath);
        applyCipher(reversedChars, korath, toEfreti);
        efreti = join(korath);
    }

    /** Splits from into words and reverses the order of the letters in each word.
     * @returns An array of words. */
    private char[][] reverseStrings(String from, Set<Character> letters) {
        String delim = " \t\n\r\f";
        StringTokenizer tokenizer = new StringTokenizer(from, delim, true);
        int words = tokenizer.countTokens();
        char[][] reverse = new char[words][];
        for(int i = 0; tokenizer.hasMoreTokens(); i++) {
            char[] chars = tokenizer.nextToken().toCharArray();
            if(chars.length > 0 && delim.indexOf(chars[0]) < 0) {
                char swapper;
                int left = 0, right = chars.length - 1;
                for(; left < right && !letters.contains(chars[right]); right--) {}
                for(; left < right && !letters.contains(chars[left]); left++) {}
                for(; left < right; left++, right--) {
                    swapper = chars[left];
                    chars[left] = chars[right];
                    chars[right] = swapper;
                }
            }
            reverse[i] = chars;
        }
        return reverse;
    }

    /** Applies a cipher to the fromStrings to generate the toStrings.
     * @param fromStrings Strings to cipher; should be from reverseStrings()
     * @param toStrings Result of cipher. Elements must be null or of the same length as corresponding elements of fromStrings
     * @param the cipher map. Should be from createExileMap() or createEfretiMap()
     */
    private void applyCipher(char[][] fromStrings, char[][] toStrings, Map<Character, Character> map) {
        int words = fromStrings.length;
        for(int i = 0; i < words; i++) {
            char[] from = fromStrings[i];
            char[] to = toStrings[i];
            if(to == null) {
                to = new char[from.length];
                toStrings[i] = to;
            }
            for(int j = 0; j < to.length; j++) {
                to[j] = map.getOrDefault(from[j], from[j]);
            }
            toStrings[i] = to;
        }
    }

    /** Appends the character arrays to make a String.
     * @param words an array of characters
     * @return a String resulting from concatenating those words */
    private String join(char[][] words) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < words.length; i++) {
            builder.append(words[i]);
        }
        return builder.toString();
    }

    private static Map<Character, Character> reverseMap(Map<Character, Character> from) {
        Map<Character, Character> to = new HashMap<>();
        for(Map.Entry<Character, Character> entry : from.entrySet())
            to.put(entry.getValue(), entry.getKey());
        return to;
    }

    /** Generates the Korath cipher map to cipher Indonesian into Exile
     * @returns a map from Indonesian character to Exile character. */
    private static Map<Character, Character> createExileMap() {
        return new HashMap<Character, Character>() {{
            put('A', 'A'); put('E', 'E'); put('I', 'I'); put('O', 'U');
            put('U', 'O'); put('B', 'H'); put('C', 'D'); put('D', 'S');
            put('F', 'J'); put('G', 'N'); put('H', 'P'); put('J', 'V');
            put('K', 'T'); put('L', 'M'); put('M', 'F'); put('N', 'R');
            put('P', 'B'); put('Q', 'Z'); put('R', 'L'); put('S', '\'');
            put('T', 'K'); put('V', 'Q'); put('W', 'C'); put('X', 'Y');
            put('Y', 'G'); put('Z', 'W');

            put('a', 'a'); put('e', 'e'); put('i', 'i'); put('o', 'u');
            put('u', 'o'); put('b', 'h'); put('c', 'd'); put('d', 's');
            put('f', 'j'); put('g', 'n'); put('h', 'p'); put('j', 'v');
            put('k', 't'); put('l', 'm'); put('m', 'f'); put('n', 'r');
            put('p', 'b'); put('q', 'z'); put('r', 'l'); put('s', '\'');
            put('t', 'k'); put('v', 'q'); put('w', 'c'); put('x', 'y');
            put('y', 'g'); put('z', 'w'); }};
    }

    /** Generates the Korath cipher map to cipher Indonesian into Efreti
     * @returns a map from Indonesian character to Efreti character. */
    private static Map<Character, Character> createEfretiMap() {
        return new HashMap<Character, Character>() {{
            put('A', 'A'); put('E', 'E'); put('I', 'I'); put('O', 'U');
            put('U', 'O'); put('B', 'B'); put('C', 'V'); put('D', 'T');
            put('F', 'Y'); put('G', 'L'); put('H', 'H'); put('J', 'W');
            put('K', 'S'); put('L', 'N'); put('M', 'F'); put('N', 'R');
            put('P', 'C'); put('Q', 'T'); put('R', 'P'); put('S', 'M');
            put('T', 'K'); put('V', 'R'); put('W', 'G'); put('X', 'S');
            put('Y', 'D'); put('Z', 'K');
            
            put('a', 'a'); put('e', 'e'); put('i', 'i'); put('o', 'u');
            put('u', 'o'); put('b', 'b'); put('c', 'v'); put('d', 't');
            put('f', 'y'); put('g', 'l'); put('h', 'h'); put('j', 'w');
            put('k', 's'); put('l', 'n'); put('m', 'f'); put('n', 'r');
            put('p', 'c'); put('q', 't'); put('r', 'p'); put('s', 'm');
            put('t', 'k'); put('v', 'r'); put('w', 'g'); put('x', 's');
            put('y', 'd'); put('z', 'k'); }};
    }

    private static Set<Character> stringToSet(String s) {
        Set<Character> result = new HashSet<>();
        for(char c : s.toCharArray())
            result.add(c);
        return result;
    }
}
