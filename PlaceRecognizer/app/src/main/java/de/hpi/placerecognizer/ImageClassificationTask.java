package de.hpi.placerecognizer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.nio.ByteBuffer;


public class ImageClassificationTask implements Runnable {

    private final Image mImage;
    private final ImageClassifier mClassifier;
    private final MainActivity mActivity;

    public ImageClassificationTask(Image image, MainActivity activity) {
        mImage = image;
        mClassifier = MainActivity.ic;
        mActivity = activity;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            final Classification[] results = mClassifier.classifyImage(bitmap, 3);
            String[] resultStrings = new String[results.length];

            for (int i = 0; i < results.length; ++i) {
                Classification result = results[i];
                resultStrings[i] = String.format("%s, %.3f", result.get_label(), result.get_probability());
            }
            final String resultString = TextUtils.join("\r\n", resultStrings);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) mActivity.findViewById(R.id.classDisplay);
                    textView.setText(resultString);
                }
            });
        } finally {
            mImage.close();
        }
    }
}
