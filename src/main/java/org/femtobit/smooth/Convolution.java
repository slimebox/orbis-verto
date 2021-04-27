package org.femtobit.smooth;

public class Convolution {

    // image and kernel goes in, convolution comes out
    public static double singlePixelConvolution(double[][] input, int x, int y, double[][] kernel, int kernelWidth, int kernelHeight) {
        double output = 0;

        for (int i = 0; i < kernelWidth; i++) {
            for (int j = 0; j < kernelHeight; j++) {
                output = output + (input[x + i][y + j] * kernel[i][j]);
            }
        }
        return output;
    }

    // Convolute a range of pixels
    public static double[][] convolution2D(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        double[][] output = new double[smallWidth][smallHeight];

        for (int i = 0; i < smallWidth; i++) {
            for (int j = 0; j < smallHeight; j++) {
                output[i][j] = singlePixelConvolution(input, i, j, kernel, kernelWidth, kernelHeight);
            }
        }

        return output;
    }

    // Whatever, these all do the same thing.
    public static double[][] convolution2DPadded(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        int top = kernelHeight / 2;
        int left = kernelWidth / 2;

        double[][] small;
        small = convolution2D(input, width, height, kernel, kernelWidth, kernelHeight);

        double[][] large = new double[width][height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                large[i][j] = 0;
            }
        }

        for (int i = 0; i < smallHeight; i++) {
            if (smallWidth >= 0) System.arraycopy(small[i], 0, large[i + left], top, smallWidth);
        }

        return large;
    }
}
