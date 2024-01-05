package humanika.rafeki.james;

import humanika.rafeki.james.James;
import humanika.rafeki.james.utils.KorathCipher;
import humanika.rafeki.james.utils.Translator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Utils {
    public static String downloadAsString(URL url) throws IOException {
        return download(url).string();
    }

    public static byte[] downloadAsBytes(URL url) throws IOException {
        return download(url).bytes();
    }

    /**
     * Checks a URL for the HTTP status code.
     * <p>
     * Returns 0 if an IOException occurs.
     * Returns 1 if a MalformedURLException occurs.
     * Returns -1 if the Response is invalid.
     * @param url The url to check.
     * @return The HTTP Status Code.
     */
    public static int getHttpStatus(String url) {
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", "MarioB(r)owser4.2");
            connection.connect();
            return connection.getResponseCode();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return 1;
        }
        catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static ResponseBody download(URL url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0")
                .get()
                .build();
        OkHttpClient okHttpClient = James.getHttpClient();
        Response response = okHttpClient.newCall(request).execute();
        return response.body();
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
