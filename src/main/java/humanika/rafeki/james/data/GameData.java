package humanika.rafeki.james.data;

import me.mcofficer.esparser.DataNode;
import me.mcofficer.esparser.Sources;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import humanika.rafeki.james.James;

public class GameData {

    /** Fetches the content reached at the URL as String. If the response is invalid, returns empty String.
     * @param url
     * @return
     */
    @CheckReturnValue
    public static String getContentFromUrl(String url) { {
        return getContentFromUrl(url, new HashMap<>());
    }}

    /** Fetches the content reached at the URL as String. If the response is invalid, returns empty String.
     * @param url
     * @param headers
     * @return
     */
    @CheckReturnValue
    public static String getContentFromUrl(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("User-Agent", "MarioB(r)owser4.2");
        headers.forEach(builder::addHeader);
        try (Response response = James.getHttpClient().newCall(builder.build()).execute()) {
            return response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /** Downloads a File and saves it in the target dir.
     * @param url
     * @param targetDir
     * @throws IOException
     */
    public static void downloadFile(String url, Path targetDir, @Nullable String filename) throws IOException, URISyntaxException {
        if (filename == null)
            filename = url.substring(url.lastIndexOf('/'));
        ReadableByteChannel channel = Channels.newChannel(new URI(url).toURL().openStream());
        FileOutputStream outputStream = new FileOutputStream(targetDir + "/" + filename);
        outputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
    }

    /** Saves the ES data files and returns the temporary directory they were saved in. Should be removed afterwards.
     * @param githubToken
     * @return
     * @throws IOException
     */
    public static ArrayList<File> fetchGameData(String githubToken) throws IOException, URISyntaxException  {
        Path temp = Files.createTempDirectory("james");
        File data = new File(temp.toAbsolutePath() + "/data/");
        data.mkdir();
        data.deleteOnExit();

        fetchGameDataRecursive(githubToken, data, "data");
        ArrayList<File> sources = Sources.getSources(temp, null);
        Files.walk(temp)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::deleteOnExit);
        return sources;
    }

    private static void fetchGameDataRecursive(String githubToken, File dataFolder, String repoPath) throws URISyntaxException {
        String url = String.format("https://api.github.com/repos/endless-sky/endless-sky/contents/%s?ref=master", repoPath);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "token "+ githubToken);
        JSONArray json = new JSONArray(getContentFromUrl(url, headers));
        for (Object o : json) {
            try {
                JSONObject j = (JSONObject) o;
                if (j.getString("name").endsWith(".txt")) {
                    downloadFile(j.getString("download_url"), dataFolder.toPath(), j.getString("path").replaceAll("/", "_"));
                } else { // assume we have a directory
                    fetchGameDataRecursive(githubToken, dataFolder, j.getString("path"));
                }
            }
            catch (IOException | JSONException e ) {
                e.printStackTrace();
            }
        }
    }
}
