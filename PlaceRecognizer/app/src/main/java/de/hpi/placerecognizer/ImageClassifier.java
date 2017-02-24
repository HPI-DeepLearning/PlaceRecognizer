package de.hpi.placerecognizer;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.renderscript.RenderScript;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import messagepack.ParamUnpacker;
import network.CNNdroid;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

class ImageClassifier {
    private CNNdroid conv = null;
    RenderScript rs = null;
    private String[] labels;
    final private String rootpath = Environment.getExternalStorageDirectory().getPath()+"/Download/";

    private static final float GREYSCALE_RED = 0.299f;
    private static final float GREYSCALE_GREEN = 0.587f;
    private static final float GREYSCALE_BLUE = 0.114f;

    Classification classifyImage(Bitmap bmp) {
        int imageWidth = 80;
        int imageHeight = 80;

        Bitmap bmp1 = Bitmap.createScaledBitmap(bmp, imageHeight, imageWidth, false);
        ParamUnpacker pu = new ParamUnpacker();

        //float[][][] mean = (float[][][]) pu.unpackerFunction(rootpath + "Data_Cifar10/mean.msg", float[][][].class);
        //float[][][][] inputBatch = new float[1][3][imageHeight][imageWidth];
        float[][][][] inputBatch = new float[1][1][imageHeight][imageWidth];

        for (int j = 0; j < imageHeight; ++j) {
            for (int k = 0; k < imageWidth; ++k) {
                int color = bmp1.getPixel(k, j);
                inputBatch[0][0][j][k] = GREYSCALE_RED * red(color) + GREYSCALE_GREEN * green(color) + GREYSCALE_BLUE * blue(color);
//                inputBatch[0][0][j][k] = (float) (red(color));// - mean[0][k][j];
//                inputBatch[0][1][j][k] = (float) (green(color));// - mean[1][k][j];
//                inputBatch[0][2][j][k] = (float) (blue(color));// - mean[2][k][j];
            }
        }

        float[][] output = (float[][]) conv.compute(inputBatch);
        //String res = accuracy(output[0], labels, 3);
        float sum = 0;
        for(int i = 0; i < output.length; i++) {
            for(int j = 0; j < output[i].length; j++) {
                System.out.print(output[i][j] + ", ");
                sum += output[i][j];
            }
            System.out.println();
        }
        System.out.println("Sum: " + sum);
        return getBestMatch(output[0]);
    }

    private void readLabels() {
        List<String> labelsList = new ArrayList<String>();
        File f = new File(rootpath + "berlin_sights_data/labels.txt");
        Scanner s;
        int iter = 0;

        try {
            s = new Scanner(f);
            while (s.hasNextLine()) {
                String str = s.nextLine();
                labelsList.add(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        labels = labelsList.toArray(new String[0]);
    }

    class prepareModel extends AsyncTask<RenderScript, Void, CNNdroid> {
        @Override
        protected CNNdroid doInBackground(RenderScript... params) {
            try {
                conv = new CNNdroid(rs, rootpath + "berlin_sights_data/BerlinSights_def.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
            readLabels();
            System.out.println("DONE");
            return conv;
        }
    }

    private String accuracy(float[] input_matrix, String[] labels, int topk) {
        String result = "";
        int max_num[] = new int[labels.length];
        Arrays.fill(max_num, -1);
        float[] max = new float[topk];

        for (int k = 0; k < topk ; ++k) {
            for (int i = 0; i < labels.length; ++i) {
                if (input_matrix[i] > max[k]) {
                    boolean newVal = true;
                    for (int j = 0; j < topk; ++j)
                        if (i == max_num[j])
                            newVal = false;
                    if (newVal) {
                        max[k] = input_matrix[i];
                        max_num[k] = i;
                    }
                }
            }
        }

        for (int i = 0 ; i < topk ; i++)
            result += labels[max_num[i]]  + ", P = " + max[i] * 100 + " %\n\n";
        return result;
    }

    private Classification getBestMatch(float[] input_matrix) {
        int bestMatch = 0;
        for(int i = 0; i < labels.length; i++) {
            if(input_matrix[i] > input_matrix[bestMatch])
                bestMatch = i;
        }
        return new Classification(bestMatch, labels[bestMatch], input_matrix[bestMatch]);
    }
}
