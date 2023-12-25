package humanika.rafeki.james;

import java.io.InputStream;

public class Utils {
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

    public static String readResourceString(String fileName) throws IOException {
        String[] resource = readResourceLines(fileName);
        if(resource == null)
            return null;
        return "\n".join(resource);
    }

    public static String[] readResourceLines(String fileName) throws IOException {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        try(InputStream stream = loader.getResourceAsStream(fileName)) {
            if(stream == null)
                return new String[0];
            try(InputStreamReader reader = new InputStreamReader(stream)) {
                return new BufferedReader(reader).lines();
            }
        }
    }
}
