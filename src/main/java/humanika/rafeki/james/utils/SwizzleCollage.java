package humanika.rafeki.james.utils;

import java.util.BitSet;
import java.awt.Point;

public class SwizzleCollage {
    //private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
    private static final double TARGET_RATIO = 16 / 9.0;

    private final BitSet swizzles;
    private final int[] swizzleNumbers;
    private final int imageCount;
    private final int[] allowedBounds;
    private final Point image;
    private final Point tiles;
    private final int[][] collage;

    public SwizzleCollage(BitSet swizzles, Point imageSize, int[] allowedBounds) throws IllegalArgumentException {
        this.image = imageSize;
        this.allowedBounds = allowedBounds;

        if(image.x > allowedBounds[allowedBounds.length - 1] || image.y > allowedBounds[allowedBounds.length - 1])
            throw new IllegalArgumentException("Image is bigger than " + allowedBounds[allowedBounds.length - 1] + " pixels");

        this.swizzles = swizzles;
        int imageCount = 0;
        for(int i = 0; i < this.swizzles.size(); i++)
            if(this.swizzles.get(i))
                imageCount++;

        swizzleNumbers = new int[imageCount];
        int iswizzle = 0;
        for(int i = 0; i < this.swizzles.size(); i++)
            if(this.swizzles.get(i)) {
                System.out.println("swizzleNumbers[" + iswizzle + "] = " + i);
                swizzleNumbers[iswizzle] = i;
                iswizzle++;
            }
        this.imageCount = imageCount;
        this.tiles = findLeastBad();
        if(tiles == null)
            throw new IllegalArgumentException("Cannot fit an image " + imageSize.x + " by " + imageSize.y + " pixels with swizzle count of " + imageCount);

        collage = makeCollage();
    }

    public int swizzleCount() {
        return imageCount;
    }

    public int length(int y) {
        return collage[y].length;
    }

    public int get(int x, int y) {
        return collage[y][x];
    }

    public Point getTileCounts() {
        return tiles;
    }

    public Point getImageSize() {
        return image;
    }

    public String asTable() {
        StringBuffer buffer = new StringBuffer(20 * tiles.x * tiles.y);
        for(int y = 0; y < tiles.y; y++) {
            int[] row = collage[y];
            if(row.length < tiles.x)
                buffer.append("  ");
            for(int x = 0; x < row.length; x++)
                buffer.append(String.format(" %2d", row[x]));
            buffer.append("\n");
        }
        return buffer.toString();
    }

    private int makeAllowedBounds(int count) {
        for(int i = 1; i < allowedBounds.length; i++)
            if(allowedBounds[i] < count)
                return allowedBounds[i - 1];
        return allowedBounds[allowedBounds.length - 1];
    }

    private Point findLeastBad() {
        double bestBadness = 9e9;
        Point bestSize = null;
        for(int countX = 1; countX <= imageCount; countX++) {
            final int countY = (int)Math.ceil(imageCount / (double)countX);
            if(image.x*countX > 4096 || image.y*countY > 4096) {
                continue; // too wide or too tall
            }
            final int allowedX = makeAllowedBounds(countX);
            final int allowedY = makeAllowedBounds(countY);
            if(allowedX < image.x || allowedY < image.y) {
                continue; // image doesn't fit in allowed space
            }
            final int unused = countX * countY - imageCount;
            if(unused < 0 || unused >= countY) {
                continue; // doesn't fit in available spots 
            }
            final double badness = badnessOf(countX, countY);
            if(badness < bestBadness) {
                bestSize = new Point(countX, countY);
                bestBadness = badness;
            }
        }
        return bestSize;
    }

    private double badnessOf(int countX, int countY) {
        int unused = countX*countY - imageCount;
        double howNonGolden = Math.abs((image.x * countX) / (double)(image.y * countY) - TARGET_RATIO);
        double totalPixels = countX * image.y * countY * image.y;
        double unusedPixels = image.y * unused * image.y;
        double howBlank = unusedPixels / (double)totalPixels;
        return howNonGolden + howBlank;
    }

    private int[][] makeCollage() {
        final int[][] collage = new int[tiles.y][];
        final int unused = tiles.x * tiles.y - imageCount;
        final int wideStart = unused / 2;
        final int wideEnd = tiles.y - (unused + 1) / 2 - 1;
        int i = 0;
        for(int y = 0; y < tiles.y; y++) {
            int width;
            if(y >= wideStart && y <= wideEnd)
                width = tiles.x;
            else
                width = tiles.x - 1;
            int[] row = new int[width];
            for(int x = 0; x < width; x++, i++) {
                int ix = swizzleNumbers[i];
                row[x] = ix;
            }
            collage[y] = row;
        }
        return collage;
    }
}
