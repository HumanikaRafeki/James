package humanika.rafeki.james.utils;

import javax.annotation.Nullable;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalInt;

public class ImageSwizzler {

    public static final int ALL_OLD_SWIZZLES = -6;
    public static final int TWENTY_NINE_SWIZZLES = -29;

    private final static String[] swizzles = {
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

    /**
     * Swizzles (swaps color channels) an image.
     * If arg is defined, swaps an image's color channels in accordance to arg's value in {@link #swizzles}.
     * Else returns an image with 2x height and 3x width, containing images of all color channel swaps defined in swizzles.
     * @param imgStream The image.
     * @param arg The number of the swizzle that should be applied.
     * @return An InputStream containing the .png-encoded image.
     * @throws IOException if an error occurs during reading or writing.
     * Blame {@link ImageIO#read(InputStream)} or {@link ImageWriter#write(IIOMetadata, IIOImage, ImageWriteParam)}.
     */
    public InputStream swizzle(BufferedImage img, int swizzle) throws IOException {
        BufferedImage output = swizzleToImage(img, swizzle);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        writer.setOutput(new MemoryCacheImageOutputStream(os));

        ImageWriteParam param = writer.getDefaultWriteParam();
        if(param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f);
        }

        writer.write(null, new IIOImage(output, null, null), param);
        writer.dispose();

        return new ByteArrayInputStream(os.toByteArray());
    }

    /**
     * Swizzles (swaps color channels) an image.
     * If arg is defined, swaps an image's color channels in accordance to arg's value in {@link #swizzles}.
     * Else returns an image with 2x height and 3x width, containing images of all color channel swaps defined in swizzles.
     * @param imgStream The image.
     * @param arg The number of the swizzle that should be applied.
     * @return A BufferedImage with the swizzled image.
     * @throws IOException if an error occurs during reading.
     * Blame {@link ImageIO#read(InputStream)} or {@link ImageWriter#write(IIOMetadata, IIOImage, ImageWriteParam)}.
     */
    public BufferedImage swizzleToImage(BufferedImage img, int swizzle) throws IOException {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] imgData = new int[width * height];
        int[] work = new int[width * height];
        img.getRGB(0, 0, width, height, imgData, 0, width);
        BufferedImage swizzled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage output = swizzled; // Swizzle 0 = no swizzling
        if(swizzle == ALL_OLD_SWIZZLES)
            output = createSixSwizzles(width, height, imgData, work, swizzled);
        else if(swizzle == TWENTY_NINE_SWIZZLES)
            output = createTwentyNineSwizzles(width, height, imgData, work, swizzled);
        else if(swizzle > 0)
            createSwizzledImage(width, height, imgData, work, swizzled, swizzles[swizzle - 1], 0, 0);
        return output;
    }

    private void createSwizzledImage(int width, int height, int[] input, int[] work, BufferedImage swizzled, String swizzle, int offsetX, int offsetY) {
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

    private int getChannelIndexByChar(char c) {
        if(c == 'r') return 1;
        else if(c == 'g') return 2;
        else if(c == 'b') return 3;
        else if(c == '0') return 4;
        else return 0;
    }

    private BufferedImage createSixSwizzles(int width, int height, int[] imgData, int[] work, BufferedImage swizzled) {
        BufferedImage output = new BufferedImage(width * 3, height * 2, BufferedImage.TYPE_INT_ARGB);
        int scansize = width * 3;
        createSwizzledImage(width, height, imgData, work, output, swizzles[0], 0, 0);
        createSwizzledImage(width, height, imgData, work, output, swizzles[1], width, 0);
        createSwizzledImage(width, height, imgData, work, output, swizzles[2], width * 2, 0);
        createSwizzledImage(width, height, imgData, work, output, swizzles[3], 0, height);
        createSwizzledImage(width, height, imgData, work, output, swizzles[4], width, height);
        createSwizzledImage(width, height, imgData, work, output, swizzles[5], width * 2, height);

        return output;
    }

    private BufferedImage createTwentyNineSwizzles(int width, int height, int[] imgData, int[] work, BufferedImage swizzled) {
        final BufferedImage output = new BufferedImage(width * 6, height * 5, BufferedImage.TYPE_INT_ARGB);
        final int collage_width = 6;
        final int collage_height = 5;
        final int scansize = width * collage_width;
        final int swizzle_count = 29;
        int swizzle = 0;
        for(int y = 0; y < collage_height; y++)
            for(int x = 0; x < collage_width && swizzle < swizzle_count; x++, swizzle++) {
                int slide = 0;
                int swizzleIndex = swizzle - 1;
                if(y == collage_height - 1)
                    slide = width / 2;
                System.out.println("x="+x+" y="+y+" width="+width+" height="+height+" slide="+slide);
                if(swizzleIndex >= 0)
                    createSwizzledImage(width, height, imgData, work, output, swizzles[swizzleIndex], width * x + slide, height * y);
                else
                    output.setRGB(width * x + slide, height * y, width, height, imgData, 0, width);
            }
        return output;
    }
}
