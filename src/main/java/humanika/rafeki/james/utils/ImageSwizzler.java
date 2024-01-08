package humanika.rafeki.james.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;

public class ImageSwizzler {

    private final static String[] SWIZZLE_PALLETE = {
            "rbg",
            "grb",
            "brg",
            "gbr",
            "bgr",
            "gbb",
            "rbb",
            "rgg",
            "bbb",
            "ggg",
            "rrr",
            "bbg",
            "bbr",
            "ggr",
            "bgg",
            "brr",
            "grr",
            "bgb",
            "brb",
            "grg",
            "ggb",
            "rrb",
            "rrg",
            "gbg",
            "rbr",
            "rgr",
            "r00",
            "000"
    };

    public final static int FIRST_SWIZZLE = 0;
    public final static int LAST_SWIZZLE = SWIZZLE_PALLETE.length;
    public static final Pattern VALID_SWIZZLE_STRING = Pattern.compile("\\A\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*\\z");
    public static final int[] NICE_IMAGE_BOUNDS = { 120, 240, 360, 480, 640, 800, 1024, 1260 };
    public static final int[] LARGEST_IMAGE_BOUNDS = { 120, 240, 360, 480, 640, 800, 1024, 1260, 1600, 1920, 2048, 3840, 4096 };

    private SwizzleCollage collage = null;
    private BufferedImage swizzled = null;

    private ImageSwizzler() {
    }

    /**
     * Swizzles (swaps color channels) an image. Creates a collage of the swizzles.
     * @param img The image.
     * @param swizzles Swizzles to target
     * @return A BufferedImage with the swizzled image.
     */
    public static ImageSwizzler swizzleImage(BufferedImage img, BitSet swizzles, int[] allowedBounds) throws IOException {
        ImageSwizzler swizzler = new ImageSwizzler();
        final int width = img.getWidth();
        final int height = img.getHeight();
        final SwizzleCollage collage = new SwizzleCollage(swizzles, new Point(width, height), allowedBounds);
        final int[] imgData = new int[width * height];
        final int[] work = new int[width * height];
        img.getRGB(0, 0, width, height, imgData, 0, width);

        final BufferedImage swizzled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage output = swizzled;
        if(collage.swizzleCount() == 1) {
            if(collage.get(0, 0) > 0) // swizzle 0 = no change to image
                createSwizzledImage(width, height, imgData, work, swizzled, SWIZZLE_PALLETE[collage.get(0, 0) - 1], 0, 0);
        } else
            output = createCollage(collage, imgData, work, swizzled);

        swizzler.collage = collage;
        swizzler.swizzled = output;
        return swizzler;
    }

    public BufferedImage getSwizzledImage() {
        return swizzled;
    }

    public SwizzleCollage getCollage() {
        return collage;
    }

    public static BitSet bitSetForSwizzles(String input) throws IllegalArgumentException {
        String cleaned = input.replaceAll("\\s+", "");
        if(!VALID_SWIZZLE_STRING.matcher(cleaned).matches())
            throw new IllegalArgumentException("Invalid swizzle string \"" + input + "\". It should be a comma-separated list of swizzles or swizzle ranges. Examples:\n- `3` = swizzle 3\n- `1-5` = swizzles 1, 2, 3, 4, and 5\n- `1-3,7-13,21` = Swizzles 1, 2, 3, 7, 8, 9, 10, 11, 12, 13, and 21");

        BitSet swizzles = new BitSet();
        String[] ranges = input.replaceAll("\\s+","").split(",");
        for(String range : ranges) {
            String[] values = range.split("-");
            int first = Integer.parseInt(values[0], 10);
            int last = first;
            if(values.length > 1)
                last = Integer.parseInt(values[1]);
            if(first < ImageSwizzler.FIRST_SWIZZLE || first > ImageSwizzler.LAST_SWIZZLE)
                throw new IllegalArgumentException("Invalid swizzle " + first + ". Swizzles must be in the range of "
                                                   + ImageSwizzler.FIRST_SWIZZLE + "-" + ImageSwizzler.LAST_SWIZZLE);
            if(last < ImageSwizzler.FIRST_SWIZZLE || last > ImageSwizzler.LAST_SWIZZLE)
                throw new IllegalArgumentException("Invalid swizzle " + last + ". Swizzles must be in the range of "
                                                   + ImageSwizzler.FIRST_SWIZZLE + "-" + ImageSwizzler.LAST_SWIZZLE);
            swizzles.set(first, last + 1, true);
        }
        if(swizzles.isEmpty())
            throw new IllegalArgumentException("Invalid swizzle string \"" + input + "\": no swizzles are selected.");
        return swizzles;
    }

    private static BufferedImage createCollage(SwizzleCollage collage, int[] imgData, int[] work, BufferedImage swizzled) {
        final Point tiles = collage.getTileCounts();
        final Point image = collage.getImageSize();
        final BufferedImage output = new BufferedImage(image.x * tiles.x, image.y * tiles.y, BufferedImage.TYPE_INT_ARGB);
        final int scansize = image.x * tiles.x;
        final int nswizzles = collage.swizzleCount();
        for(int y = 0; y < tiles.y; y++) {
            int nrow = collage.length(y);
            boolean shouldSlide = nrow < tiles.x;
            int slide = shouldSlide ? image.x / 2 : 0;
            for(int x = 0; x < nrow; x++) {
                int swizzle = collage.get(x, y);
                int swizzleIndex = swizzle - 1;
                if(swizzleIndex >= 0)
                    createSwizzledImage(image.x, image.y, imgData, work, output, SWIZZLE_PALLETE[swizzleIndex],
                                        image.x * x + slide, image.y * y);
                else
                    output.setRGB(image.x * x + slide, image.y * y, image.x, image.y, imgData, 0, image.x);
            }
        }
        return output;
    }

    private static void createSwizzledImage(int width, int height, int[] input, int[] work, BufferedImage swizzled, String swizzle, int offsetX, int offsetY) {
        int channelOne = getChannelIndexByChar(swizzle.charAt(0));
        int channelTwo = getChannelIndexByChar(swizzle.charAt(1));
        int channelThree = getChannelIndexByChar(swizzle.charAt(2));

        int[] channels = new int[5];
        channels[4] = 0;

        int points = width * height;
        for(int i = 0; i < points; i++) {
            int rgb = input[i];
            //int rgb = original.getRGB(col, row);
            channels[0] = (rgb >> 24) & 0x000000FF;
            channels[1] = (rgb >> 16) & 0x000000FF;
            channels[2] = (rgb >> 8) & 0x000000FF;
            channels[3] = (rgb) & 0x000000FF;
            int a = channels[0];
            int r = channels[channelOne];
            int g = channels[channelTwo];
            int b = channels[channelThree];
            int pixel = (a << 24) | (r << 16) | (g << 8) | b;
            work[i] = pixel;
        }
        swizzled.setRGB(offsetX, offsetY, width, height, work, 0, width);
    }

    private static int getChannelIndexByChar(char c) {
        if(c == 'r') return 1;
        else if(c == 'g') return 2;
        else if(c == 'b') return 3;
        else if(c == '0') return 4;
        else return 0;
    }
}
