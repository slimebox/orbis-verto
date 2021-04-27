package org.femtobit.smooth;

public class GaussianSmooth {

    // Get the value at x,y in the Gaussian distribution
    public static double gaussianDiscrete2D(double theta, int x, int y) {
        double g = 0;
        for(double ySub = y - 0.5; ySub < y + 0.55; ySub += 0.1) {
            for(double xSub = x - 0.5; xSub < x + 0.55; xSub += 0.1) {
                g = g + ((1 / (2 * Math.PI * theta * theta))
                        * Math.pow(Math.E, -(xSub * xSub + ySub * ySub)
                                    / (2 * theta * theta)));
            }
        }
        return g / 121;
    }

    // Generate a Gaussian kernel of given size
    public static double[][] gaussian2D(double theta, int size) {
        double[][] kernel = new double[size][size];

        for(int j = 0; j < size; ++j) {
            for(int i = 0; i < size; ++i) {
                kernel[i][j] = gaussianDiscrete2D(theta, i - (size / 2), j - (size / 2));
            }
        }
        return kernel;
    }

    // Generate a kernel and smooth an image.
    public static double[][] smooth(double[][] input, int width, int height, int kernelSize, double theta) {
        double[][] gaussianKernel = gaussian2D(theta, kernelSize);
        return Convolution.convolution2DPadded(input, width, height, gaussianKernel, kernelSize, kernelSize);
    }

    // You know the drill by now. Calculate kernel, something something convolution.
    public static int[] smooth_image(int[] input, int width, int height, int kernelSize, double theta) {
        double[][] input2D = new double[width][height];
        double[] output1D = new double[width * height];
        double[][] output2D;
        int[] output = new int[width * height];

        for(int j = 0; j < height; ++j) {
            for(int i = 0; i < width; ++i) {
                int color = input[j * width + i];
                int colorRed = (color & 0x00FF0000) >> 16;
                input2D[i][j] = colorRed;
            }
        }

        output2D = smooth(input2D, width, height, kernelSize, theta);

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                output1D[j * width + i] = output2D[i][j];
            }
        }

        for (int i = 0; i < output1D.length; i++) {
            int grey = (int) Math.round(output1D[i]);
            if(grey > 255)
                grey = 255;
            if(grey < 0)
                grey = 0;

            output[i] = (grey << 16 | grey << 8 | grey);
        }

        return output;
    }
}
