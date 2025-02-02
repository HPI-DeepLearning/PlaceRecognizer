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

import network.CNNdroid;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

class ImageClassifier {
    private CNNdroid conv = null;
    RenderScript rs = null;
    private String[] labels;
    final private String rootpath = Environment.getExternalStorageDirectory().getPath()+"/Download/berlin_sights_data/";

    Classification classifyImage(Bitmap bmp) {
        float[][] output = classifyImage_imp(bmp);

        return getBestMatch(output[0]);
    }

    private float[][] classifyImage_imp(Bitmap bmp) {
        int imageWidth = 80;
        int imageHeight = 80;

        Bitmap bmp1 = Bitmap.createScaledBitmap(bmp, imageWidth, imageHeight, false);

        //uncomment this if you want to use a mean file
        /*ParamUnpacker pu = new ParamUnpacker();
        float[][][] mean = (float[][][]) pu.unpackerFunction(rootpath + "/mean.msg", float[][][].class);*/

        float[][][][] inputBatch = new float[1][3][imageHeight][imageWidth];

        for (int j = 0; j < imageHeight; ++j) {
            for (int k = 0; k < imageWidth; ++k) {
                int color = bmp1.getPixel(j, k);
                inputBatch[0][0][k][j] = (float) (red(color));// - mean[0][k][j];
                inputBatch[0][1][k][j] = (float) (green(color));// - mean[1][k][j];
                inputBatch[0][2][k][j] = (float) (blue(color));// - mean[2][k][j];
            }
        }

        return (float[][]) conv.compute(inputBatch);
    }

    Classification[] classifyImage(Bitmap bmp, int topK) {
        float[][] output = classifyImage_imp(bmp);

        return getTopKresults(output[0], topK);
    }

    private void readLabels() {
        List<String> labelsList = new ArrayList<>();
        File f = new File(rootpath + "labels.txt");
        Scanner s;

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
                conv = new CNNdroid(rs, rootpath + "BerlinSights_def.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
            readLabels();
            System.out.println("DONE");
            return conv;
        }
    }

    private Classification getBestMatch(float[] input_matrix) {
        int bestMatch = 0;
        for(int i = 0; i < labels.length; i++) {
            if(input_matrix[i] > input_matrix[bestMatch])
                bestMatch = i;
        }
        return new Classification(bestMatch, labels[bestMatch], input_matrix[bestMatch]);
    }

    private Classification[] getTopKresults(float[] input_matrix, int k) {
        float[] sorted_values = input_matrix.clone();
        Arrays.sort(sorted_values);

        Classification[] topK = new Classification[k];
        List<Float> input_list = new ArrayList<>();
        for (float f: input_matrix) {
            input_list.add(f);
        }

        if (BuildConfig.DEBUG && sorted_values.length < k) {
            throw new RuntimeException("Too few predicted values for getting topK results!");
        }

        for (int i = 0; i < topK.length; ++i) {
            int classId = input_list.indexOf(sorted_values[sorted_values.length - i - 1]);
            topK[i] = new Classification(classId, labels[classId], input_matrix[classId]);
        }
        return topK;
    }
}
