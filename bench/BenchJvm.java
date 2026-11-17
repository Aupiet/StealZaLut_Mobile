package bench;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.stream.IntStream;

public class BenchJvm {

    // Simple NPY float32 reader for verifying parity
    public static float[] readNpyFloat32(String path, int expectedCount) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(path, "r");
             FileChannel channel = file.getChannel()) {
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buf.order(ByteOrder.LITTLE_ENDIAN);
            // Skip NPY header (find newline after header dict)
            byte b;
            while ((b = buf.get()) != 0x0A) {
                // scanning header
            }
            float[] data = new float[expectedCount];
            buf.asFloatBuffer().get(data);
            return data;
        }
    }

    // 1. Trilinear LUT Application (Single-threaded & Multi-threaded)
    public static void applyLutTrilinear(float[] img, int width, int height, float[] lut, int lutSize, float[] out, boolean parallel) {
        float scale = (float)(lutSize - 1);
        int n2 = lutSize * lutSize;
        int maxCoord = lutSize - 2;

        IntStream rowStream = IntStream.range(0, height);
        if (parallel) {
            rowStream = rowStream.parallel();
        }

        rowStream.forEach(y -> {
            int rowOffset = y * width * 3;
            for (int x = 0; x < width; x++) {
                int px = rowOffset + x * 3;
                float r = img[px] * scale;
                float g = img[px + 1] * scale;
                float b = img[px + 2] * scale;

                int x0 = (int)r;
                if (x0 < 0) x0 = 0; else if (x0 > maxCoord) x0 = maxCoord;
                float dx = r - x0;

                int y0 = (int)g;
                if (y0 < 0) y0 = 0; else if (y0 > maxCoord) y0 = maxCoord;
                float dy = g - y0;

                int z0 = (int)b;
                if (z0 < 0) z0 = 0; else if (z0 > maxCoord) z0 = maxCoord;
                float dz = b - z0;

                int base = (z0 * n2 + y0 * lutSize + x0) * 3;
                int basePlus1 = base + 3;
                int basePlusN = (z0 * n2 + (y0 + 1) * lutSize + x0) * 3;
                int basePlusN1 = basePlusN + 3;
                int basePlusN2 = ((z0 + 1) * n2 + y0 * lutSize + x0) * 3;
                int basePlusN21 = basePlusN2 + 3;
                int basePlusN2N = ((z0 + 1) * n2 + (y0 + 1) * lutSize + x0) * 3;
                int basePlusN2N1 = basePlusN2N + 3;

                float oneMinusDx = 1.0f - dx;
                float oneMinusDy = 1.0f - dy;
                float oneMinusDz = 1.0f - dz;

                for (int c = 0; c < 3; c++) {
                    float c00 = oneMinusDx * lut[base + c] + dx * lut[basePlus1 + c];
                    float c01 = oneMinusDx * lut[basePlusN + c] + dx * lut[basePlusN1 + c];
                    float c10 = oneMinusDx * lut[basePlusN2 + c] + dx * lut[basePlusN21 + c];
                    float c11 = oneMinusDx * lut[basePlusN2N + c] + dx * lut[basePlusN2N1 + c];

                    float c0 = oneMinusDy * c00 + dy * c01;
                    float c1 = oneMinusDy * c10 + dy * c11;

                    float val = oneMinusDz * c0 + dz * c1;
                    out[px + c] = val < 0f ? 0f : (val > 1f ? 1f : val);
                }
            }
        });
    }

    // 2. Separable Gaussian Blur (Reflect mode, matching SciPy)
    public static void gaussianBlur2D(float[] img, int width, int height, float sigma, float[] out, boolean parallel) {
        int radius = (int)Math.ceil(4.0f * sigma);
        int kSize = 2 * radius + 1;
        float[] kernel = new float[kSize];
        float sum = 0f;
        float twoSigmaSq = 2f * sigma * sigma;
        for (int i = 0; i < kSize; i++) {
            int d = i - radius;
            kernel[i] = (float)Math.exp(-(d * d) / twoSigmaSq);
            sum += kernel[i];
        }
        for (int i = 0; i < kSize; i++) {
            kernel[i] /= sum;
        }

        float[] temp = new float[img.length];

        // Horizontal pass (reflect border)
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowOffset = y * width * 3;
            for (int x = 0; x < width; x++) {
                int outIdx = rowOffset + x * 3;
                float rSum = 0f, gSum = 0f, bSum = 0f;
                for (int k = -radius; k <= radius; k++) {
                    int ix = x + k;
                    if (ix < 0) ix = -ix - 1;
                    else if (ix >= width) ix = 2 * width - ix - 1;
                    // Additional clamp if radius > width
                    if (ix < 0) ix = 0; else if (ix >= width) ix = width - 1;

                    int inIdx = rowOffset + ix * 3;
                    float wgt = kernel[k + radius];
                    rSum += img[inIdx] * wgt;
                    gSum += img[inIdx + 1] * wgt;
                    bSum += img[inIdx + 2] * wgt;
                }
                temp[outIdx] = rSum;
                temp[outIdx + 1] = gSum;
                temp[outIdx + 2] = bSum;
            }
        });

        // Vertical pass (reflect border)
        IntStream.range(0, width).parallel().forEach(x -> {
            for (int y = 0; y < height; y++) {
                int outIdx = (y * width + x) * 3;
                float rSum = 0f, gSum = 0f, bSum = 0f;
                for (int k = -radius; k <= radius; k++) {
                    int iy = y + k;
                    if (iy < 0) iy = -iy - 1;
                    else if (iy >= height) iy = 2 * height - iy - 1;
                    if (iy < 0) iy = 0; else if (iy >= height) iy = height - 1;

                    int inIdx = (iy * width + x) * 3;
                    float wgt = kernel[k + radius];
                    rSum += temp[inIdx] * wgt;
                    gSum += temp[inIdx + 1] * wgt;
                    bSum += temp[inIdx + 2] * wgt;
                }
                out[outIdx] = rSum;
                out[outIdx + 1] = gSum;
                out[outIdx + 2] = bSum;
            }
        });
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== BENCHMARK JVM / KOTLIN EQUIVALENT ===");

        // Load reference LUT
        int lutSize = 33;
        float[] lut = readNpyFloat32("bench/lut33.npy", lutSize * lutSize * lutSize * 3);

        // 1. Proxy 1024x768
        int pw = 1024, ph = 768;
        float[] proxyImg = new float[pw * ph * 3];
        for (int i = 0; i < proxyImg.length; i++) {
            proxyImg[i] = (float)Math.random();
        }
        float[] proxyOut = new float[proxyImg.length];

        // Warmup JIT
        for (int i = 0; i < 5; i++) {
            applyLutTrilinear(proxyImg, pw, ph, lut, lutSize, proxyOut, false);
        }

        // Measure Proxy ST & MT
        long t0 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            applyLutTrilinear(proxyImg, pw, ph, lut, lutSize, proxyOut, false);
        }
        double proxyStMs = (System.nanoTime() - t0) / 10.0 / 1e6;

        t0 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            applyLutTrilinear(proxyImg, pw, ph, lut, lutSize, proxyOut, true);
        }
        double proxyMtMs = (System.nanoTime() - t0) / 10.0 / 1e6;
        System.out.printf("Proxy 1024x768 LUT 33: Single-Thread=%.2f ms, Multi-Thread=%.2f ms\n", proxyStMs, proxyMtMs);

        // 2. 24 MP Image (6000x4000)
        int w = 6000, h = 4000;
        System.out.printf("\nAllocating 24 MP image (%dx%d, 288 MB)...\n", w, h);
        float[] img24 = new float[w * h * 3];
        IntStream.range(0, h).parallel().forEach(y -> {
            float vy = (float)y / (h - 1);
            int row = y * w * 3;
            for (int x = 0; x < w; x++) {
                float vx = (float)x / (w - 1);
                img24[row + x * 3] = vx;
                img24[row + x * 3 + 1] = vy;
                img24[row + x * 3 + 2] = 1.0f - vx * 0.5f - vy * 0.5f;
            }
        });

        float[] out24 = new float[w * h * 3];

        // Measure 24 MP LUT 33 Single-Thread
        t0 = System.nanoTime();
        applyLutTrilinear(img24, w, h, lut, lutSize, out24, false);
        double lutStMs = (System.nanoTime() - t0) / 1e6;

        // Measure 24 MP LUT 33 Multi-Thread
        t0 = System.nanoTime();
        applyLutTrilinear(img24, w, h, lut, lutSize, out24, true);
        double lutMtMs = (System.nanoTime() - t0) / 1e6;
        System.out.printf("24 MP LUT 33: Single-Thread=%.2f ms, Multi-Thread=%.2f ms\n", lutStMs, lutMtMs);

        // PARITY CHECK: compare crop (100x100) against Python reference
        float[] pyCrop = readNpyFloat32("bench/py_out_lut_crop.npy", 100 * 100 * 3);
        float maxDiff = 0f;
        double sumDiff = 0.0;
        int count = 100 * 100 * 3;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int idxFull = (y * w + x) * 3;
                int idxCrop = (y * 100 + x) * 3;
                for (int c = 0; c < 3; c++) {
                    float diff = Math.abs(out24[idxFull + c] - pyCrop[idxCrop + c]);
                    if (diff > maxDiff) maxDiff = diff;
                    sumDiff += diff;
                }
            }
        }
        System.out.printf("LUT 33 Numerical Parity vs Python: Max ΔE = %.6f, Mean ΔE = %.6f\n", maxDiff, sumDiff / count);

        // 3. 24 MP Gaussian Blur (sigma=3.0)
        t0 = System.nanoTime();
        gaussianBlur2D(img24, w, h, 3.0f, out24, true);
        double blurMtMs = (System.nanoTime() - t0) / 1e6;
        System.out.printf("24 MP Gaussian Blur (sigma=3.0, Multi-Thread): time=%.2f ms\n", blurMtMs);

        // PARITY CHECK on Blur
        float[] pyBlurCrop = readNpyFloat32("bench/py_out_blur_crop.npy", 100 * 100 * 3);
        float maxBlurDiff = 0f;
        double sumBlurDiff = 0.0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int idxFull = (y * w + x) * 3;
                int idxCrop = (y * 100 + x) * 3;
                for (int c = 0; c < 3; c++) {
                    float diff = Math.abs(out24[idxFull + c] - pyBlurCrop[idxCrop + c]);
                    if (diff > maxBlurDiff) maxBlurDiff = diff;
                    sumBlurDiff += diff;
                }
            }
        }
        System.out.printf("Gaussian Blur Parity vs SciPy: Max ΔE = %.6f, Mean ΔE = %.6f\n", maxBlurDiff, sumBlurDiff / count);

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        System.out.printf("JVM Peak Memory: ~%d MB\n", usedMb);
    }
}
