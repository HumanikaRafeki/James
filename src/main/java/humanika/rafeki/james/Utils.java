package humanika.rafeki.james;

import humanika.rafeki.james.James;
import humanika.rafeki.james.utils.KorathCipher;
import humanika.rafeki.james.utils.Translator;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Utils {
    private final static Base64.Encoder encoder = Base64.getUrlEncoder();
    private final static Random random = new Random();

    public static String downloadAsString(URL url) throws IOException {
        return download(url).string();
    }

    public static byte[] downloadAsBytes(URL url) throws IOException {
        return download(url).bytes();
    }

    public static String randomBase64(int randomBytes) {
        byte[] bytes = new byte[randomBytes];
        random.nextBytes(bytes);
        return encoder.encodeToString(bytes);
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
            URL u = new URI(url).toURL();
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", "MarioB(r)owser4.2");
            connection.connect();
            return connection.getResponseCode();
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return 1;
        }
        catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Creates an InputStream for reading the given image
     * @param img the image to stream
     * @returns an InputStream that reads the image
     * @throws IOException if an error occurs during reading.
     * Blame {@link ImageIO#read(InputStream)} or {@link ImageWriter#write(IIOMetadata, IIOImage, ImageWriteParam)}.
     */
    public static InputStream imageStream(BufferedImage img) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        writer.setOutput(new MemoryCacheImageOutputStream(os));

        ImageWriteParam param = writer.getDefaultWriteParam();
        if(param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f);
        }

        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();

        return new ByteArrayInputStream(os.toByteArray());
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

    /** Applies the Korath reverse cipher to the Efreti text.
     * @param efreti the efreti output of the Korath cipher
     * @returns reverse-ciphered text which might be Indonesian */
    public static KorathCipher reverseEfretiCipher(String efreti) {
        return new KorathCipher(null, null, null, efreti).unefreti();
    }

    /** Applies the Korath reverse cipher to the Exile text.
     * @param exile the exile output of the Korath cipher
     * @returns reverse-ciphered text which might be Indonesian */
    public static KorathCipher reverseExileCipher(String exile) {
        return new KorathCipher(null, null, exile, null).unexile();
    }

    /** Applies the Korath cipher to the Indonesian text.
     * @param indonesian a string containing text that should be in the Indonesian language
     * @returns a KorathCipher object where the indonesian, exile, and efreti fields are valid */
    public static KorathCipher applyKorathCipher(String indonesian) {
        return new KorathCipher(null, indonesian, null, null).indokorath();
    }

    /** Translates English text to Indonesian and applies the Korath cipher to the Indonesian text.
     * The English translation requires web access.
     * @param english a string containing text that should be in the English language
     * @returns a KorathCipher object where the english, indonesian, exile, and efreti fields are valid
     * @throws IOException if something goes wrong while translating to Indonesian */
    public static KorathCipher translateToKorath(String english) throws IOException {
        String indonesian = Translator.translate("en", "id", english, James.getHttpClient());
        return new KorathCipher(english, indonesian, null, null).indokorath();
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
