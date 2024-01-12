package humanika.rafeki.james.utils;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ImageSwizzler {

    /** All known swizzles. */
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

    /** Pattern for finding swizzle lists like "1,3-8,19,24-15" */
    public static final Pattern VALID_SWIZZLE_STRING = Pattern.compile("\\A\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*\\z");

    /** Nice-looking image linear sizes up to 1280 */
    public static final int[] NICE_IMAGE_BOUNDS = { 120, 240, 360, 480, 640, 800, 1024, 1280 };

    /** Nice-looking image linear sizes up to 4096 */
    public static final int[] LARGEST_IMAGE_BOUNDS = { 120, 240, 360, 480, 640, 800, 1024, 1280, 1600, 1920, 2048, 3840, 4096 };

    /** First known swizzle; always 0 */
    public final static int FIRST_SWIZZLE = 0;

    /** Last known swizzle */
    public final static int LAST_SWIZZLE = SWIZZLE_PALLETE.length;

    /** One of the swizzles changes nothing. This can be optimized by copying the input to the output */
    public final static int SWIZZLE_COPY_IMAGE = 0;

    /** One swizzle is an edge detector. */
    public final static int SWIZZLE_MAKE_OUTLINE = 28;

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
    public static ImageSwizzler swizzleImage(BufferedImage img, BitSet swizzles, int[] allowedBounds) {
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
            if(collage.get(0, 0) == SWIZZLE_MAKE_OUTLINE)
                createImageOutline(width, height, imgData, work, swizzled, 0, 0);
            else if(collage.get(0, 0) != SWIZZLE_COPY_IMAGE)
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

    private final static void appendSwizzleRange(StringBuilder builder, int firstInSeries, int lastInSeries) {
        if(builder.length() > 0)
            builder.append(',');
        builder.append(firstInSeries);
        if(lastInSeries > firstInSeries)
            builder.append('-').append(lastInSeries);
    }

    public static String describeSwizzleSet(BitSet swizzles) {
        int firstInSeries = FIRST_SWIZZLE - 2;
        int lastInSeries = FIRST_SWIZZLE - 2;
        StringBuilder builder = new StringBuilder();
        for(int i = FIRST_SWIZZLE; i <= LAST_SWIZZLE; i++) {
            if(!swizzles.get(i))
                continue;
            boolean haveSeries = lastInSeries >= FIRST_SWIZZLE;
            boolean inSeries = i == lastInSeries + 1;
            if(!haveSeries)
                firstInSeries = i;
            else if(!inSeries) {
                appendSwizzleRange(builder, firstInSeries, lastInSeries);
                firstInSeries = i;
            }
            lastInSeries = i;
        }
        appendSwizzleRange(builder, firstInSeries, lastInSeries);
        return builder.toString();
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
                if(swizzle == SWIZZLE_COPY_IMAGE)
                    output.setRGB(image.x * x + slide, image.y * y, image.x, image.y, imgData, 0, image.x);
                else if(swizzle == SWIZZLE_MAKE_OUTLINE)
                    createImageOutline(image.x, image.y, imgData, work, output,
                                       image.x * x + slide, image.y * y);
                else
                    createSwizzledImage(image.x, image.y, imgData, work, output, SWIZZLE_PALLETE[swizzle - 1],
                                        image.x * x + slide, image.y * y);
            }
        }
        return output;
    }

    private static void createSwizzledImage(int width, int height, int[] input, int[] work, BufferedImage swizzled, String swizzle, int offsetX, int offsetY) {
        int channelOne = getChannelIndexByChar(swizzle.charAt(0));
        int channelTwo = getChannelIndexByChar(swizzle.charAt(1));
        int channelThree = getChannelIndexByChar(swizzle.charAt(2));

        int[] channels = new int[5];
        channels[0] = 1; // optimization: no swizzles use or change alpha
        channels[4] = 0; // "channel" 4 is an alias for value 0

        int points = width * height;
        for(int i = 0; i < points; i++) {
            int rgb = input[i];
            //channels[0] = (rgb >>> 24) & 255;
            channels[1] = (rgb >> 16) & 255;
            channels[2] = (rgb >> 8) & 255;
            channels[3] = (rgb) & 255;
            //int a = channels[0];
            int r = channels[channelOne];
            int g = channels[channelTwo];
            int b = channels[channelThree];
            int pixel = (rgb & 0xFF000000) | (r << 16) | (g << 8) | b;
            work[i] = pixel;
        }
        swizzled.setRGB(offsetX, offsetY, width, height, work, 0, width);
    }

    private static void createImageOutline(int width, int height, int[] input, int[] work, BufferedImage swizzled, int offsetX, int offsetY) {
        int npixels = width * height;
        final double normkernel = 1.0 / Math.sqrt(128);
        final double weight = /* 0.618034 * */ 0.5;
        double myWeight = 4 * (1 - weight) + 4 * (1 - weight * weight) + 1;
        double normweight = 1/(weight + myWeight);
        double pixelnorm = 1.0 / 255.0;
        double pi2 = 3.14159 / 2;
        double[] grey = new double[npixels];
        double[] sobel = new double[npixels];

        for(int i = 0; i < npixels; i++) {
            int pixel = input[i];
            grey[i] = (  0.4 * ((pixel       ) & 255)
                       + 0.4 * ((pixel >>>  8) & 255)
                       + 0.4 * ((pixel >>> 16) & 255)
                       +       ((pixel >>> 24) & 255)) * pixelnorm;
        }

        for(int y = 0, i = 0; y < height; y++) {
            int up = y == 0 ? 0 : -width;
            int down = y == height - 1 ? 0 : width;
            for(int x = 0; x < width; x++, i++) {
                int left = x == 0 ? 0 : -1;
                int right = x == width - 1 ? 0 : 1;
                // sobel filter kernels:
                // int[] xkernel = { {-1,  0,  1},
                //                   {-2,  0,  2},
                //                   {-1,  0,  1} };
                // int[] ykernel = { {-1, -2, -1},
                //                   { 0,  0,  0},
                //                   { 1,  2,  1} };
                double xtotal = -    grey[i + up + left]   +     grey[i + up + right]
                                -2 * grey[i + left]        + 2 * grey[i + right]
                                -    grey[i + down + left] +     grey[i + down + right];
                double ytotal = -grey[i + up + left]   -2 * grey[i + up]   - grey[i + up + right]
                                +grey[i + down + left] +2 * grey[i + down] + grey[i + down + right];
                sobel[i] = xtotal * xtotal + ytotal * ytotal;
            }
        }

        for(int y = 0, i = 0; y < height; y++) {
            int up = y == 0 ? 0 : -width;
            int down = y == height - 1 ? 0 : width;
            for(int x = 0; x < width; x++, i++) {
                int left = x == 0 ? 0 : -1;
                int right = x == width - 1 ? 0 : 1;
                double alpha = myWeight * sobel[i] +
                    weight * (sobel[i + up] + sobel[i + down] + sobel[i + left] + sobel[i + right] +
                           weight * (sobel[i + up + left] + sobel[i + up + right]
                                     + sobel[i + down + left] + sobel[i + down + right]));
                double scale1 = Math.sin(Math.min(pi2, Math.sqrt(alpha) * 0.125));
                int ialpha = Math.max(0, Math.min(255, (int)(scale1 * 255)));
                work[i] = (ialpha << 24) | (ialpha << 16) | (ialpha << 8) | ialpha;
            }
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
