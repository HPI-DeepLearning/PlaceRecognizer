package layers;

import android.util.Log;

import layers.LayerInterface;
import messagepack.ParamUnpacker;
import numdroid.MyNum;


public class BatchNorm implements LayerInterface {

    private String name;
    private MyNum myNum;
    private boolean parallel;
    private ParamUnpacker paramUnpacker;

    private static final float EPS = 1e-5f;

    private float[] fixedMean;
    private float[] fixedVariance;
    private float[] gamma;
    private float[] beta;

    public BatchNorm(String name, String paramFilePath, boolean parallel) {
        this.name = name;
        this.parallel = parallel;
        this.myNum = new MyNum();
        this.paramUnpacker = new ParamUnpacker();

        // loadParams
        long loadTime = System.currentTimeMillis();
        Object[] objects = paramUnpacker.unpackerFunction(
                paramFilePath,
                new Class[]{float[].class, float[].class, float[].class, float[].class}
        );

        this.gamma = (float[]) objects[0];
        this.beta = (float[]) objects[1];
        this.fixedMean = (float[]) objects[2];
        this.fixedVariance = (float[]) objects[3];

        loadTime = System.currentTimeMillis() - loadTime;
        Log.d("CNNDroid", "layers." + name + ": Parameters Load Time in Constructor = " + String.valueOf(loadTime));
    }

    @Override
    public Object compute(Object input) {
        Object output;
        long runTime = System.currentTimeMillis();

        output = batchNormSeq((float[][][][]) input);

        runTime = System.currentTimeMillis() - runTime;
        Log.d("CNNdroid", "layers." + name + ": Computation Run Time = " + String.valueOf(runTime));

        return output;
    }

    private float[][][][] batchNormSeq(float[][][][] inputBlob4) {
        int batchSize = inputBlob4.length;
        int numChannels = inputBlob4[0].length;
        int height = inputBlob4[0][0].length;
        int width = inputBlob4[0][0][0].length;

        float[][][][] data = new float[batchSize][numChannels][height][width];

        for (int n = 0; n < batchSize; ++n) {
            for (int c = 0; c < numChannels; ++c) {
                for (int h = 0; h < height; ++h) {
                    for (int w = 0; w < width; ++w) {
                        float x_hat = (float) ((inputBlob4[n][c][h][w] - fixedMean[c]) / Math.sqrt(fixedVariance[c] + EPS));
                        data[n][c][h][w] = gamma[c] * x_hat + beta[c];
                    }
                }
            }
        }
        return data;
    }
}
