package humanika.rafeki.james.data;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import javax.imageio.ImageIO;

class ImageCatalog extends SimpleFileVisitor<Path> {
    private final static Pattern pattern = Pattern.compile("^(?<name>.*?)(?<number>[~+-][0-9]+)?\\.(?<extension>[a-z0-9A-Z_]*)$");

    private HashMap<String, Object> files = new HashMap<>();
    private Path root;
    private Stack<String> relative = new Stack<>();

    ImageCatalog(Path root) throws IOException {
        this.root = root;
        Files.walkFileTree(root, this);
        relative = null;
    }

    public Path getRoot() {
        return root;
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
        if(got == null)
            files.put(name, path);
        else if(got instanceof Path) {
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
