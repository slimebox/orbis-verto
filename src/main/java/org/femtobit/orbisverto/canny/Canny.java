package org.femtobit.orbisverto.canny;

import org.femtobit.orbisverto.util.Vector2i;
import org.femtobit.smooth.GaussianSmooth;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Stack;

public class Canny {

    private int output_width;
    private int output_height;

    public int[] process(int[] source, int width, int height, int size, float theta, int thresholdLow, int thresholdHigh, float scale, float offset) {

        output_height = height;
        output_width = width;

        int[] output = new int[width * height];

        int[] temp1D;
        int[][] temp2D = new int[width][height];
        int[][] gaussianP = new int[width][height];
        int[][] gaussianQ = new int[width][height];
        int[][] gaussianM = new int[width][height];

        double[][] gaussianTheta = new double[width][height];

        int[][] suppression = new int[width][height];
        int[][] delta = new int[width][height];
        int[][] tracked;

        int result;

        // initialise to all-opaque
        Arrays.fill(output, 0xff000000);

        temp1D = GaussianSmooth.smooth_image(source, width, height, size, theta);

        for(int i = 0; i < temp1D.length; i++) {
            temp1D[i] = temp1D[i] & 0x000000ff;
        }

        for (int i = 0; i < output_width; i++) {
            for (int j = 0; j < output_height; j++) {
                temp2D[i][j] = temp1D[i + (j * output_width)];
            }
        }

        output_height--;
        output_width--;

        for (int i = 0; i < output_width; i++) {
            for (int j = 0; j < output_height; j++) {
                gaussianP[i][j] = (temp2D[i][j + 1] - temp2D[i][j] + temp2D[i + 1][j + 1] + temp2D[i + 1][j]) / 2;
                gaussianQ[i][j] = (temp2D[i][j] - temp2D[i + 1][j] + temp2D[i][j + 1] - temp2D[i + 1][j + 1]) / 2;
                gaussianM[i][j] = (int) Math.sqrt(Math.pow(gaussianP[i][j], 2) +
                                                    Math.pow(gaussianQ[i][j], 2));

                gaussianTheta[i][j] = Math.atan2(gaussianQ[i][j], gaussianP[i][j]);

                delta[i][j] = sector(gaussianTheta[i][j]);
            }
        }

        output_width--;
        output_height--;

        for (int i = 0; i < output_width; i++) {
            suppression[i][0] = 0;
            suppression[i][output_height] = 0;
        }
        for (int i = 0; i < output_height; i++) {
            suppression[0][i] = 0;
            suppression[output_width][i] = 0;
        }

        for (int i = 0; i < output_width; i++) {
            for (int j = 0; j < output_height; j++) {
                suppression[i][j] = suppress(gaussianM, delta[i][j], i, j, thresholdLow);
            }
        }

        output_height--;
        output_width--;

        tracked = trackAll(suppression, output_width, output_height, thresholdLow, thresholdHigh);

        for (int i = 0; i < output_width; i++) {
            for (int j = 0; j < output_height; j++) {
                result = (int) (tracked[i][j] * scale + offset);
                if(result > 255) result = 255;
                if(result < 0) result = 0;

                output[(i + (j * (output_width + 3)))] = 0xff000000 | result << 16 | result << 8 | result;
            }
        }

        output_width += 3;
        output_height += 3;

        return output;

    }

    // which quadrant?
    private int sector(double theta) {
        theta = Math.toDegrees(theta) + 270 % 360;

        if((theta >= 337.5) || (theta < 22.5) || ((theta >= 157.5) && (theta < 202.5))){
            return 0;
        }
        if(((theta >= 22.5) && (theta < 67.5)) || ((theta >= 202.5) && (theta < 247.5))){
            return 1;
        }
        if(((theta >= 67.5) && (theta < 112.5)) || ((theta >= 247.5) && (theta < 292.5))){
            return 2;
        }
        if(((theta >= 112.5) && (theta < 157.5)) || ((theta >= 292.5) && (theta < 337.5))){
            return 3;
        }
        return 0;
    }

    // apply non-maxima suppression
    private int suppress(int[][] gaussianM, int sector, int i, int j, int thresholdLow) {
        int temp = gaussianM[i][j];
        if(temp < thresholdLow) return 0;

        switch(sector) {
            case 0:
                return (gaussianM[i + 1][j]     >= temp || gaussianM[i - 1][j]     > temp) ? 0 : temp;
            case 1:
                return (gaussianM[i + 1][j + 1] >= temp || gaussianM[i - 1][j - 1] > temp) ? 0 : temp;
            case 2:
                return (gaussianM[i][j + 1]     >= temp || gaussianM[i][j - 1]     > temp) ? 0 : temp;
            case 3:
                return (gaussianM[i + 1][j - 1] >= temp || gaussianM[i - 1][j + 1] > temp) ? 0 : temp;
            default:
                System.out.println("Unknown sector in Canny - " + sector);
                return 0;
        }
    }

    // iterate the image and track every possible path
    private int[][] trackAll(int[][] input, int width, int height, int thresholdLow, int thresholdHigh) {
        output_height = height;
        output_width = width;

        int[][] marked = new int[width][height];
        int[][] tracked = new int[width][height];

        Stack<Vector2i> pathsToTrack = new Stack<>();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                marked[i][j] = 0;

                if((input[i][j] > thresholdHigh) && (marked[i][j] == 0))
                    marked = trackPath(input, marked, pathsToTrack, thresholdLow, i, j);

                if(marked[i][j] == 0)
                    tracked[i][j] = 0;
                else
                    tracked[i][j] = input[i][j];
            }
        }

        return tracked;
    }

    // track lines, navigating along pixels of suitable contrast until all routes are explored
    private int[][] trackPath(int[][] input, int[][] marked, Stack<Vector2i> pathsToTrack, int threshold, int i, int j) {
        boolean empty = false;
        int currentX, currentY;
        Vector2i currentPoint = new Vector2i(i, j);

        pathsToTrack.push(currentPoint);

        while(!empty) {
            try {
                currentPoint = pathsToTrack.pop();

                currentX = currentPoint.x;
                currentY = currentPoint.y;

                if(marked[currentX][currentY] == 0) {

                    if(currentX > 0 && currentY > 0) {
                        if(input[currentX - 1][currentY - 1] > threshold) {
                            currentPoint = new Vector2i(currentX - 1, currentY -1);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentY > 0) {
                        if(input[currentX][currentY - 1] > threshold) {
                            currentPoint = new Vector2i(currentX, currentY - 1);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentX < output_width - 1 && currentY > 0) {
                        if(input[currentX + 1][currentY - 1] > threshold) {
                            currentPoint = new Vector2i(currentX + 1, currentY - 1);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentX > 0) {
                        if(input[currentX - 1][currentY] > threshold) {
                            currentPoint = new Vector2i(currentX - 1, currentY);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentX < output_width - 1) {
                        if(input[currentX + 1][currentY] > threshold) {
                            currentPoint = new Vector2i(currentX + 1, currentY);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentX > 0 && currentY < (output_height - 1)) {
                        if(input[currentX - 1][currentY + 1] > threshold) {
                            currentPoint = new Vector2i(currentX - 1, currentY + 1);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentY < (output_height - 1)) {
                        if(input[currentX][currentY + 1] > threshold) {
                            currentPoint = new Vector2i(currentX, currentY + 1);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    if(currentX < output_width - 1 && currentY < output_height - 1) {
                        if(input[currentX + 1][currentY + 1] > threshold) {
                            currentPoint = new Vector2i(currentX + 1, currentY + 1);
                            pathsToTrack.push(currentPoint);
                        }
                    }

                    marked[currentX][currentY] = 1;
                }
            }

            catch(EmptyStackException e) {
                empty = true;
            }
        }

        return marked;
    }

}
