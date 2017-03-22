package de.hpi.placerecognizer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


public class ImageClassificationTask implements Runnable {

    private final Image mImage;
    private final ImageClassifier mClassifier;

    public ImageClassificationTask(Image image, ImageClassifier classifier) {
        mImage = image;
        mClassifier = classifier;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            mClassifier.classifyImage(bitmap);
        } finally {
            mImage.close();
        }
    }
}
