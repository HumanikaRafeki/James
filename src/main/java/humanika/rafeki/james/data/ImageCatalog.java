package humanika.rafeki.james.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

class ImageCatalog extends SimpleFileVisitor<Path> {
    private final static MessageDigest hasher = makeHasher();
    private final static Base64.Encoder encoder = Base64.getEncoder();
    private final static StringMetric metric = StringMetrics.needlemanWunch();

    private static final MessageDigest makeHasher() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException nsae) {
            // Should never happen because SHA-256 is required by Java standard.
            return null;
        }
    }

    private final static Pattern pattern = Pattern.compile("^(?<name>.*?)(?<number>[~+-][0-9]*)?\\.(?<extension>[a-z0-9A-Z_]*)$");

    /** Map from Endless Sky image name (ie. effects/something) to all
     * paths that match that name (ie. effects/something-1.png and
     * effects/something-2.png). The values are either Path objects or
     * a List of them. */
    private Map<String, Object> files = new HashMap<>();

    /** Map from Endless Sky image name to a base64 encoded sha hash of the name (reverse of hashName) */
    private Map<String, String> nameHash = new HashMap<>();

    /** Map from base64 encoded sha hash of an Endless Sky image name to the name (reverse of nameHash) */
    private Map<String, String> hashName = new HashMap<>();

    /** Map from Endless Sky image name to its search string */
    private Map<String, String> nameSearch = new HashMap<>();

    /** The Endless Sky "images" directory */
    private Path root;

    /** Directory components while recursing through image directory tree */
    private Stack<String> relative = new Stack<>();

    ImageCatalog(Path root) throws IOException {
        this.root = root;
        Files.walkFileTree(root, this);
        relative = null;
    }

    public Path getRoot() {
        return root;
    }

    public List<SearchResult> fuzzyMatchRelativePaths(String query, int maxSearch, Predicate<String> condition) {
        int threshold = maxSearch > 0 ? 3 * maxSearch : 0;
        ShrinkableArrayList<SearchResult> work = new ShrinkableArrayList<>();
        for(Map.Entry<String, String> entry : nameSearch.entrySet()) {
            String name = entry.getKey();
            String searchString = entry.getValue();
            if(condition.test(name)) {
                float score = metric.compare(query, searchString);
                work.add(SearchResult.of(score, searchString, nameHash.get(name), name));
                if(threshold > 0 && work.size() > threshold) {
                    work.sort(SearchResult::lessThan);
                    work.shrink(maxSearch);
                }
            }
        }
        if(work.size() < 1)
            return Collections.emptyList();
        if(work.size() > 1)
            work.sort(SearchResult::lessThan);
        if(work.size() > maxSearch)
            work.shrink(maxSearch);
        return Collections.unmodifiableList(work);
    }

    public Optional<String> imageWithHash(String hash) {
        return Optional.ofNullable(hashName.get(hash));
    }

    public Optional<SearchResult> dummyResultWithHash(String hash) {
        String name = hashName.get(hash);
        if(name == null)
            return Optional.empty();
        return Optional.of(SearchResult.of(0, null, hash, name));
    }

    public Optional<Path> getImagePath(String name) {
        Object there = files.get(name);
        if(there == null)
            return Optional.empty();
        else if(there instanceof Path)
            return Optional.of((Path)there);
        else if(there instanceof ArrayList) {
            ArrayList<Path> list = (ArrayList<Path>)there;
            return Optional.of(list.get(list.size()/2));
        }
        return Optional.empty();
    }

    public Optional<Path> getImageRelativePath(String name) {
        Optional<Path> path = getImagePath(name);
        if(!path.isPresent())
            return path;
        return Optional.of(root.relativize(path.get()));
    }

    public Optional<BufferedImage> loadImage(String name) throws IOException {
        Optional<Path> path = getImagePath(name);
        if(path.isPresent())
            return Optional.of(ImageIO.read(path.get().toFile()));
        return Optional.empty();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
        relative.push(root.relativize(path).toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException exception) throws IOException {
        relative.pop();
        if(exception != null)
            throw exception;
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        if(!attrs.isRegularFile())
            return FileVisitResult.CONTINUE;
        String filename = path.toFile().getName();
        // Split /path/to/myfile~3.png into "myfile" and "~3" and "png"
        Matcher matcher = pattern.matcher(filename);
        if(matcher.matches()) {
            String extension = matcher.group("extension");
            // Only accept known image extensions.
            if(extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg")) {
                String name = matcher.group("name");
                String combined = relative.peek() + "/" + name;
                add(combined, path);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    private void add(String name, Path path) {
        // Store as /path/to/myfile
        Object got = files.get(name);
        if(got == null) {
            files.put(name, path);
            String hash = encoder.encodeToString(hasher.digest(name.getBytes(StandardCharsets.UTF_8)));
            nameHash.put(name, hash);
            hashName.put(hash, name);
            String search = name.replaceAll("[^a-zA-Z0-9-]+", " ").replaceAll("\\s+", " ").strip();
            nameSearch.put(name, search);
        } else if(got instanceof Path) {
            ArrayList<Path> list = new ArrayList<>();
            list.add((Path)got);
            list.add(path);
            files.put(name, list);
        } else if(got instanceof ArrayList) {
            ArrayList<Path> list = (ArrayList<Path>)got;
            list.add(path);
        }
    }
}
