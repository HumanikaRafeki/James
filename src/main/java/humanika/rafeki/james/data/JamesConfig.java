package humanika.rafeki.james.data;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import java.util.Map;

public class JamesConfig {
    public final URI botRepo;
    public final URI endlessSkyRepo;
    public final String endlessSkyData;
    public final String endlessSkyDataQuery;
    public final String endlessSkyRaw;
    public final String swizzledThumbnailPath;
    public final int maxExpandedPhraseLength;
    public final int maxPhraseRecursionDepth;
    public final int maxPhraseExpansions;
    public final File workArea;
    public final int maxPhraseCommandRepetitions;
    public final int maxPhraseAttachmentSize;
    public final int maxSwizzleImageWidth;
    public final int maxSwizzleImageHeight;
    public final int maxAllSwizzleWidth;
    public final int maxAllSwizzleHeight;
    public final Path botTokenFile;
    public final List<CreatorTemplate> creatorTemplates;

    public JamesConfig(String file, Logger logger) throws IOException, JSONException, URISyntaxException {
        // Read file and strip comments
        List<String> lines = Files.readAllLines(Paths.get(file));
        List<String> cleaned = new ArrayList<>();
        for(String line : lines) {
            if(line.trim().startsWith("//"))
                cleaned.add("");
            else
                cleaned.add(line);
        }
        String text = String.join("\n", cleaned);

        // Read the file and parse out settings from JSON
        JSONObject contents = new JSONObject(text);
        botRepo = new URI(contents.getString("bot_repo"));
        endlessSkyRepo = new URI(contents.getString("endless_sky_repo"));
        endlessSkyData = contents.getString("endless_sky_data");
        endlessSkyDataQuery = contents.getString("endless_sky_data_query");
        endlessSkyRaw = contents.getString("endless_sky_raw");
        swizzledThumbnailPath = contents.getString("swizzled_thumbnail_path");
        maxExpandedPhraseLength = contents.getInt("max_expanded_phrase_length");
        maxPhraseRecursionDepth = contents.getInt("max_phrase_recursion_depth");
        maxPhraseExpansions = contents.getInt("max_phrase_expansions");
        workArea = new File(contents.getString("work_area"));
        maxPhraseCommandRepetitions = contents.getInt("max_phrase_command_repetitions");
        maxPhraseAttachmentSize = contents.getInt("max_phrase_attachment_size");
        maxSwizzleImageWidth = contents.getInt("max_swizzle_image_width");
        maxSwizzleImageHeight = contents.getInt("max_swizzle_image_height");
        maxAllSwizzleWidth = contents.getInt("max_all_swizzle_width");
        maxAllSwizzleHeight = contents.getInt("max_all_swizzle_height");
        botTokenFile = new File(contents.getString("bot_token_file")).toPath();

        JSONArray jsonTemplates = contents.optJSONArray("creator_templates");
        ArrayList<CreatorTemplate> listTemplates = new ArrayList<>();
        for(Object obj : jsonTemplates.toList()) {
            Map jobj = (Map)obj;
            listTemplates.add(new CreatorTemplate(
                 (String)(jobj.get("id")),
                 (String)(jobj.get("explanation")),
                 (String)(jobj.get("url"))));
        }
        creatorTemplates = Collections.unmodifiableList(listTemplates);
    }
}
