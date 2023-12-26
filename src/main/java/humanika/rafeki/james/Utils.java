package humanika.rafeki.james;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import humanika.rafeki.james.utils.KorathCipher;
import humanika.rafeki.james.utils.Translator;
import humanika.rafeki.james.James;

public class Utils {
    /** Reads the entire contents of a stream of known size
     * @param size the length of the stream in bytes
     * @param stream the stream to read
     * @param timeout total number of nanoseconds to read before giving up
     * @returns a byte array containing all characters that were read,
     * or null if nothing was read. The array may be shorter than size
     * if the entire file could not be written before the timeout or
     * end-of-file. */
    public static byte[] readWholeStream(int size, InputStream stream, long timeout) throws IOException {
        long startTime = System.nanoTime();
        byte[] b = new byte[size];
        int count = 0;
        int offset = 0;
        while(count >= 0 && offset < size && System.nanoTime() - startTime < timeout) {
            count = stream.read(b, offset, size - offset);
            if(count > 0)
                offset += count;
        }
        if(offset <= 0)
            return null;
        if(offset < b.length)
            b = Arrays.copyOf(b, offset);
        return b;
    }

    /** Applies the Korath cipher to the Indonesian text.
     * @param indonesian a string containing text that should be in the Indonesian language
     * @returns a KorathCipher object where the indonesian, exile, and efreti fields are valid */
    public static KorathCipher applyKorathCipher(String indonesian) {
        return new KorathCipher(null, indonesian).indokorath();
    }

    /** Translates English text to Indonesian and applies the Korath cipher to the Indonesian text.
     * @param english a string containing text that should be in the English language
     * @returns a KorathCipher object where the english, indonesian, exile, and efreti fields are valid */
    public static KorathCipher translateToKorath(String english) throws IOException {
        String indonesian = Translator.translate("en", "id", english, James.getHttpClient());
        return new KorathCipher(english, indonesian).indokorath();
    }

    /**
     * Gets a specific resource file as a Stream<String>
     *
     * @param fileName The file path omitting "resources/"
     * @return The contents of the file as a Stream<String>, otherwise throws an exception
     */
    public static List<String> readResourceLines(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileName)) {
            if (resourceAsStream == null) return null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                Stream<String> stream = reader.lines();
                List<String> result = stream.collect(Collectors.toList());
                return result;
            }
        }
    }
}
