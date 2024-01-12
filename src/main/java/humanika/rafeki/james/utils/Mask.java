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

import java.util.Collections;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Mask {
    /** Direction kernel for obtaining the 8 nearest neighbors, beginning with "N" and
     * moving clockwise (since the frame data starts in the top-left and moves L->R). */
    private final static int[][] STEP = {
        {0, -1}, { 1, -1}, { 1, 0}, { 1,  1},
        {0,  1}, {-1,  1}, {-1, 0}, {-1, -1},
    };
    private final static double[] SCALE = { 1., 1. / Math.sqrt(2.) };

    final List<List<DoubleVertex>> outlines;
    final double radius;

    List<List<DoubleVertex>> getOutlines() {
        return outlines;
    }

    public double getRadius() {
        return radius;
    }

    // Construct a mask from the alpha channel of an RGBA-formatted image.
    private Mask(BufferedImage image) {
	double radius = 0;

	List<List<DoubleVertex>> raw = new ArrayList<>();
        trace(image, raw);
	if(raw.size() == 0) {
            this.outlines = Collections.unmodifiableList(raw);
            this.radius = 0;
            return;
        }

        DoubleVertex size = new DoubleVertex(image.getWidth(), image.getHeight());
        ArrayList<List<DoubleVertex>> outlines = new ArrayList<List<DoubleVertex>>();
	outlines.ensureCapacity(raw.size());

	for(List<DoubleVertex> edge : raw) {
            smoothAndCenter(edge, size);

            List<DoubleVertex> outline = simplify(edge);
            // Skip any outlines that have no area.
            if(outline.size() <= 2)
                continue;

            radius = Math.max(radius, computeRadius(outline));
            outlines.add(Collections.unmodifiableList(outline));
	}
        this.outlines = Collections.unmodifiableList(outlines);
        this.radius = radius;
    }

    // Find the radius of the object.
    double computeRadius(List<DoubleVertex> outline) {
        double radius = 0.;
        for(DoubleVertex p : outline)
            radius = Math.max(radius, p.lengthSquared());
        return Math.sqrt(radius);
    }

    private void trace(BufferedImage img, List<List<DoubleVertex>> raw) {
        final int width = img.getWidth();
        final int height = img.getHeight();
        final int numPixels = width * height;
        final float[] alpha = alphaFraction(img);
        final boolean[] hasOutline = new boolean[numPixels];
        Arrays.fill(hasOutline, false);

        // Convert from a direction index to the desired pixel.
        final int off[] = {
            -width, -width + 1,  1,  width + 1,
            width,  width - 1, -1, -width - 1,
        };

        // The image pixel being inspected, in XY coords.
        IntVertex next = new IntVertex();
        IntVertex p = new IntVertex();

        final ArrayList<Integer> directions = new ArrayList<>();
        ArrayList<DoubleVertex> points = new ArrayList<>();
        for(int start = 0; start < numPixels;) {
            directions.clear();
            points.clear();

            // Find a pixel with some renderable color data (i.e. a non-zero alpha component).
            for(;start < numPixels; start++)
                if(alpha[start] != 0) {
                    // If this pixel is not part of an existing outline, trace it.
                    if(!hasOutline[start])
                        break;
                    // Otherwise, advance to the next transparent pixel.
                    // (any non-transparent pixels will belong to the existing outline).
                    for(start++; start < numPixels; start++)
                        if(alpha[start] == 0)
                            break;
                }
            if(start >= numPixels)
                // All pixels were transparent.
                return;

            // Loop until we come back to the start, recording the directions
            // that outline each pixel (rather than the actual pixel itself).
            int d = 7;

            // The current image pixel, in index coordinates.
            int pos = start;

            // The current image pixel, in (X, Y) coordinates.
            p.assign(pos % width, pos / width);

            do {
                hasOutline[pos] = true;
                int firstD = d;
                // The image pixel being inspected, in XY coords.
                next.assign(p);
                boolean isAlone = false;
                while(true) {
                    next.shift(STEP[d][0], STEP[d][1]);
                    // First, ensure an offset in this direction would access a valid pixel index.
                    if(next.x >= 0 && next.x < width && next.y >= 0 && next.y < height)
                        // If that pixel has color data, then add it to the outline.
                        if(alpha[pos + off[d]] != 0)
                            break;
                    // Otherwise, advance to the next direction.
                    d = (d + 1) & 7;
                    // If this point is alone, bail out.
                    if(d == firstD) {
                        isAlone = true;
                        break;
                    }
                }
                if(isAlone)
                    break;

                // Advance the pixels and store the direction traveled.
                p.assign(next);
                pos += off[d];
                directions.add(Integer.valueOf(d));

                // Rotate the direction backward ninety degrees.
                d = (d + 6) & 7;

                // Loop until we are back where we started.
            } while(pos != start);

            // At least 4 points are needed to outline a non-transparent pixel.
            if(directions.size() < 4)
                continue;

            // Interpolate outline points from directions and alpha values, rather than just the pixel's XY.
            points.ensureCapacity(directions.size());
            pos = start;
            p.assign(pos % width, pos / width);
            int prev = directions.get(directions.size() - 1);
            for(Integer nextInteger : directions) {
                int inext = nextInteger.intValue();
                // Face outside by rotating direction backward ninety degrees.
                int out0 = (prev + 6) & 7;
                int out1 = (inext + 6) & 7;

                // Determine the subpixel shift, where higher alphas will shift the estimate outward.
                // (MAYBE: use an actual alpha gradient for dir & magnitude, or remove altogether.)
                double scale = alpha[pos] - 0.5;
                points.add(new DoubleVertex(STEP[out0][0] * SCALE[out0 & 1] + STEP[out1][0] * SCALE[out1 & 1],
                                            STEP[out0][1] * SCALE[out0 & 1] + STEP[out1][1] * SCALE[out1 & 1])
                           .normalize().scale(scale).shift(p.x, p.y));
                //shift *= ((begin[pos] & on) >> 24) * (1. / 255.) - .5;
                p.shift(STEP[inext][0], STEP[inext][1]);
                pos += off[inext];
                prev = inext;
            }
            raw.add(points);
        }
        return;
    }

    static private void smoothAndCenter(List<DoubleVertex> raw, DoubleVertex size) {
        // Smooth out the outline by averaging neighboring points.
        DoubleVertex prev = raw.get(raw.size() - 1);
        for(DoubleVertex p : raw)
            prev.shift(p).shiftBack(size).swap(p);
    }

    // Distance from a point to a line, squared.
    static private double distanceSquared(DoubleVertex p, DoubleVertex a, DoubleVertex b) {
        // Convert to a coordinate system where a is the origin.
        p.shiftBack(a);
        b.shiftBack(b);
        double length = b.lengthSquared();
        if(length > 0) {
            // Find out how far along the line the tangent to p intersects.
            double u = b.dot(p) / length;
            // If it is beyond one of the endpoints, use that endpoint.
            p.shiftBack(b, Math.max(0., Math.min(1., u)));
        }
        return p.lengthSquared();
    }

    static private void simplify(List<DoubleVertex> p, int first, int last, List<DoubleVertex> result) {
        // Find the most divergent point.
        double dmax = 0.;
        int imax = 0;

        for(int i = first + 1; true; ++i) {
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
        simplify(p, first, imax, result);
        result.add(p.get(imax));
        simplify(p, imax, last, result);
    }

    private static List<DoubleVertex> simplify(List<DoubleVertex> raw) {
        // Out of all the top-most and bottom-most pixels, find the ones that
        // are closest to the center of the image.
        int top = -1;
        int bottom = -1;
        for(int i = 0; i < raw.size(); ++i) {
            DoubleVertex rawi = raw.get(i);
            double ax = Math.abs(rawi.x);
            double y = rawi.y;
            if(top == -1)
                top = bottom = i;
            else if(y > raw.get(bottom).y || (y == raw.get(bottom).y && ax < Math.abs(raw.get(bottom).x)))
                bottom = i;
            else if(y < raw.get(top).y || (y == raw.get(top).y && ax < Math.abs(raw.get(top).x)))
                top = i;
        }

        List<DoubleVertex> result = new ArrayList<>();
        if(top != bottom) {
            result.add(raw.get(top));
            simplify(raw, top, bottom, result);
            result.add(raw.get(bottom));
            simplify(raw, bottom, top, result);
        }
        return result;
    }

    private static float[] alphaFraction(BufferedImage img) {
        final int width = img.getWidth();
        final int height = img.getHeight();
        final int numPixels = width * height;
        final int [] input = new int[numPixels];
        img.getRGB(0, 0, width, height, input, 0, width);

        final float[] hasPixels = new float[numPixels];
        for(int i = 0; i < numPixels;)
            hasPixels[i] = (input[i] >>> 24) / 256.0f;
        return hasPixels;
    }
}
