/*
This is a Java rewrite of Mask.cpp from the Endless Sky source
code. The original copyright is as follows:

Copyright (c) 2014 by Michael Zahniser

Endless Sky is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later version.

Endless Sky is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.
*/

package humanika.rafeki.james.utils;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import humanika.rafeki.james.utils.Mask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mask {
    private static final Logger LOGGER = LoggerFactory.getLogger(Mask.class);
    /** Direction kernel for obtaining the 8 nearest neighbors, beginning with "N" and
     * moving clockwise (since the frame data starts in the top-left and moves L->R). */
    private final static int[][] STEP = {
        {0, -1}, { 1, -1}, { 1, 0}, { 1,  1},
        {0,  1}, {-1,  1}, {-1, 0}, {-1, -1},
    };
    private final static double[] SCALE = { 1., 1. / Math.sqrt(2.) };

    class Countdown {
        private long remaining;
        Countdown(long remaining) {
            this.remaining = remaining;
        }
        long tick() throws MaskException {
            remaining--;
            if(remaining == 0)
                throw new MaskException("Infinite loop detected. Giving up.");
            return remaining;
        }
    }

    final List<List<DoubleVertex>> outlines;
    final double radius;

    public double getRadius() {
        return radius;
    }

    // Construct a mask from the alpha channel of an RGBA-formatted image.
    public Mask(BufferedImage image) throws MaskException {
	double radius = 0;
        Countdown countdown = new Countdown(((long)image.getWidth()) * ((long)image.getHeight()) * 30l);
	List<List<DoubleVertex>> raw = new ArrayList<>();
        trace(image, raw, countdown);
	if(raw.size() == 0) {
            this.outlines = Collections.unmodifiableList(raw);
            this.radius = 0;
            return;
        }

        DoubleVertex size = new DoubleVertex(image.getWidth(), image.getHeight());
        ArrayList<List<DoubleVertex>> outlines = new ArrayList<List<DoubleVertex>>();
	outlines.ensureCapacity(raw.size());

	for(List<DoubleVertex> edge : raw) {
            //smoothAndCenter(edge, size, countdown);

            List<DoubleVertex> outline = edge; // simplify(edge, countdown);
            // Skip any outlines that have no area.
            if(outline.size() <= 2)
                continue;

            radius = Math.max(radius, computeRadius(outline));
            outlines.add(Collections.unmodifiableList(outline));
	}
        this.outlines = Collections.unmodifiableList(outlines);
        this.radius = radius;
    }

    public void drawMask(BufferedImage image) {
        System.out.println("Mask:");
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(Color.MAGENTA);
            int[] x = new int[100];
            int[] y = new int[100];
            System.out.println("    Polygon:");
            for(List<DoubleVertex> polygon : outlines) {
                if(polygon.size() < 3) {
                    System.out.println("        Not a polygon! Has only " + polygon.size() + " sides!");
                    continue;
                }
                if(x.length < polygon.size()) {
                    x = new int[polygon.size() * 5 / 4];
                    y = new int[x.length];
                }
                for(int i = 0; i < polygon.size(); i++) {
                    DoubleVertex vertex = polygon.get(i);
                    x[i] = (int)Math.round(vertex.x);
                    y[i] = (int)Math.round(vertex.y);
                    System.out.println("        Vertex: " + x[i] + " " + y[i]);
                }
                g2d.drawPolygon(x, y, polygon.size());
            }
        } finally {
            g2d.dispose();
        }
        System.out.println("End mask.");
    }

    // Find the radius of the object.
    private double computeRadius(List<DoubleVertex> outline) {
        double radius = 0.;
        for(DoubleVertex p : outline)
            radius = Math.max(radius, p.lengthSquared());
        return Math.sqrt(radius);
    }

    private void trace(BufferedImage img, List<List<DoubleVertex>> raw, Countdown countdown) throws MaskException {
        final int width = img.getWidth();
        final int height = img.getHeight();
        final int numPixels = width * height;
        final int[] alpha = alphaChannel(img);
        final boolean[] hasOutline = new boolean[numPixels];
        Arrays.fill(hasOutline, false);

        // Convert from a direction index to the desired pixel.
        final int off[] = {
            -width, -width + 1,  1,  width + 1,
             width,  width - 1, -1, -width - 1,
        };

        // The image pixel being inspected, in XY coords.
        IntVertex p = new IntVertex();

        for(int x = 0; x < width; x++) {
            alpha[x] = 0;
            alpha[numPixels - x - 1] = 0;
            hasOutline[x] = true;
            hasOutline[numPixels - x - 1] = true;
        }

        for(int y = 1; y < height; y++) {
            alpha[y * width] = 0;
            alpha[y * width - 1] = 0;
            hasOutline[y * width] = true;
            hasOutline[y * width - 1] = true;
        }

        for(int y = 1; y < height - 1; y++)
            for(int x = 1; x < width - 1; x++) {
                int start = y * width + x;
                boolean startOn = alpha[start] != 0;
                boolean allSame = true;
                for(int d = 0; d < off.length; d++) {
                    boolean dOn = alpha[start + off[d]] != 0;
                    if(startOn != dOn) {
                        allSame = false;
                        break;
                    }
                }
                if(allSame)
                    hasOutline[start] = true;
            }

        final ArrayList<Integer> directions = new ArrayList<>();
        for(int start = 0; start < numPixels; start++) {
            countdown.tick();
            directions.clear();
            while(start < numPixels && hasOutline[start] && alpha[start] == 0) {
                countdown.tick();
                start++;
            }

            if(start >= numPixels)
                continue;

            // Loop until we come back to the start, recording the directions
            // that outline each pixel (rather than the actual pixel itself).
            int d = 7;

            // The current image pixel, in index coordinates.
            int pos = start;
            do {
//                LOGGER.info("pos = " + pos + "," + pos + " start = " + start + "," + start);
                countdown.tick();
                // The image pixel being inspected, in XY coords.
                boolean isAlone = false;
                int next = pos;
                int g = 0;
                for(g = 0; g < 8; g++, d = (d + 1) & 7) {
                    countdown.tick();
                    next = pos + off[d];
                    if(!hasOutline[next] && alpha[next] != 0)
                        break;
                }
                if(g == 8) {
                    // Lone point.
                    System.out.println("LONE POINT " + (pos % width) + "," + (pos / width));
                    break;
                }

                // Advance the pixels and store the direction traveled.
                pos = next;
                directions.add(Integer.valueOf(d));

                // Rotate the direction backward ninety degrees.
                d = (d + 6) & 7;

                // Loop until we are back where we started.
            } while(pos != start);

            // At least 4 points are needed to outline a non-transparent pixel.
            if(directions.size() < 4)
                continue;

            // Interpolate outline points from directions and alpha values, rather than just the pixel's XY.
            ArrayList<DoubleVertex> points = new ArrayList<>(directions.size());
            pos = start;
            p.assign(pos % width, pos / width);
            int prev = directions.get(directions.size() - 1).intValue();
            for(Integer nextInteger : directions) {
                countdown.tick();
                hasOutline[pos] = true;
                int inext = nextInteger.intValue();
                // Face outside by rotating direction backward ninety degrees.
                int out0 = (prev + 6) & 7;
                int out1 = (inext + 6) & 7;

                // Determine the subpixel shift, where higher alphas will shift the estimate outward.
                // (MAYBE: use an actual alpha gradient for dir & magnitude, or remove altogether.)
                // shift *= ((begin[pos] & on) >> 24) * (1. / 255.) - .5;
                double shiftScale = alpha[pos] * (1.0 / 255.0) - 0.5;
                DoubleVertex dv = new DoubleVertex(STEP[out0][0] * SCALE[out0 & 1] + STEP[out1][0] * SCALE[out1 & 1],
                                                   STEP[out0][1] * SCALE[out0 & 1] + STEP[out1][1] * SCALE[out1 & 1])
                    .normalize().scale(shiftScale);
//                System.out.println("DV before shift " + dv.x + "," + dv.y);
                dv.shift(p.x, p.y);
//                System.out.println("DV shift by     " + p.x + "," + p.y);
//                System.out.println("DV after  shift " + dv.x + "," + dv.y);
                points.add(dv);
                //shift *= ((begin[pos] & on) >> 24) * (1. / 255.) - .5;
                p.shift(STEP[inext][0], STEP[inext][1]);
                pos += off[inext];
                prev = inext;
            }
            raw.add(points);
        }
        return;
    }

    static private void smoothAndCenter(List<DoubleVertex> raw, DoubleVertex size, Countdown countdown) throws MaskException {
        if(raw.size() < 3)
            return;
        // Smooth out the outline by averaging neighboring points.
        DoubleVertex prev = raw.get(raw.size() - 1);
        for(DoubleVertex p : raw) {
            countdown.tick();
            prev.shift(p).shiftBack(size).swap(p);
        }
    }

    // Distance from a point to a line, squared.
    static private double distanceSquared(DoubleVertex p, DoubleVertex a, DoubleVertex b) {
        // Convert to a coordinate system where a is the origin.
        p.shiftBack(a);
        b.shiftBack(a);
        double length = b.lengthSquared();
        if(length > 0) {
            // Find out how far along the line the tangent to p intersects.
            double u = b.dot(p) / length;
            // If it is beyond one of the endpoints, use that endpoint.
            p.shiftBack(b, Math.max(0., Math.min(1., u)));
        }
        return p.lengthSquared();
    }

    static private void simplify(List<DoubleVertex> p, int first, int last, List<DoubleVertex> result, Countdown countdown) throws MaskException {
        // Find the most divergent point.
        double dmax = 0.;
        int imax = 0;

        for(int i = first + 1; true; ++i) {
            countdown.tick();
            if(i == p.size())
                i = 0;
            if(i == last)
                break;

            double d = distanceSquared(p.get(i), p.get(first), p.get(last));
            // Enforce symmetry by using y position as a tiebreaker rather than
            // just the order in the list.
            if(d > dmax || (d == dmax && p.get(i).y > p.get(imax).y)) {
                dmax = d;
                imax = i;
            }
        }

        // If the most divergent point is close enough to the outline, stop.
        if(dmax < 1.)
            return;

        // Recursively simplify the lines to both sides of that point.
        simplify(p, first, imax, result, countdown);
        result.add(p.get(imax));
        simplify(p, imax, last, result, countdown);
    }

    private static List<DoubleVertex> simplify(List<DoubleVertex> raw, Countdown countdown) throws MaskException {
        // Out of all the top-most and bottom-most pixels, find the ones that
        // are closest to the center of the image.
        int top = -1;
        int bottom = -1;
        for(int i = 0; i < raw.size(); ++i) {
            countdown.tick();
            DoubleVertex rawi = raw.get(i);
            double ax = Math.abs(rawi.x);
            double y = rawi.y;
            if(top == -1) {
                bottom = i;
                top = bottom;
            } else if(y > raw.get(bottom).y || (y == raw.get(bottom).y && ax < Math.abs(raw.get(bottom).x)))
                bottom = i;
            else if(y < raw.get(top).y || (y == raw.get(top).y && ax < Math.abs(raw.get(top).x)))
                top = i;
        }

        List<DoubleVertex> result = new ArrayList<>();
        if(top != bottom) {
            result.add(raw.get(top));
            simplify(raw, top, bottom, result, countdown);
            result.add(raw.get(bottom));
            simplify(raw, bottom, top, result, countdown);
        }
        return result;
    }

    private static int[] alphaChannel(BufferedImage img) {
        final int width = img.getWidth();
        final int height = img.getHeight();
        final int numPixels = width * height;
        final int [] alpha = new int[numPixels];
        img.getRGB(0, 0, width, height, alpha, 0, width);

        for(int i = 0; i < numPixels; i++)
            alpha[i] = (alpha[i] >>> 24) & 255;
        return alpha;
    }
}
