package de.hpi.placerecognizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.RenderScript;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import messagepack.ParamUnpacker;
import network.CNNdroid;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

public class GPSLogger extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 200;
    private static boolean permission_granted = false;
    GPSTracker gps;
    RenderScript rs = null;
    CNNdroid conv = null;
    String[] labels;

    private boolean hasPermission(String permission) {
        int permissionStatus = ActivityCompat.checkSelfPermission(this, permission);
        return(permissionStatus == PackageManager.PERMISSION_GRANTED);
    }

    private void showCoordinates(View view) {
        gps = new GPSTracker(GPSLogger.this);
        if (gps.canGetLocation() && permission_granted) {
            double lng = gps.getLongitude();
            double lat = gps.getLatitude();
            Snackbar.make(view, "Lat: " + lat + "\nLong: " + lng, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            gps.showSettingsAlert();
        }
    }

    private void addImageViewToFlipper(final String pathToImg, final ViewFlipper viewFlipper) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getPath() + pathToImg));
        viewFlipper.addView(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                viewFlipper.showNext();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpslogger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQ_CODE);
        } else if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                  (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE);
        } else {
            permission_granted = true;
        }

        rs = RenderScript.create(this);
        new prepareModel().execute(rs);

        final String[] images = new String[] {
                "/Download/Holopainen_Image02.jpg",
                "/Download/BMW-2-series.jpg",
                "/Download/sea-gull-bird-sky-nature.jpg",
                "/Download/bird.jpg",
                "/Download/truck.jpg",
                "/Download/ship.jpg",
                "/Download/dog.jpg",
                "/Download/cat.jpg",
                "/Download/frog.jpg",
                "/Download/horse.jpg",
                "/Download/airplane.jpg",
                "/Download/airplane2.jpg",
                "/Download/horse2.jpg",
                "/Download/horse3.jpg"
        };

        final ViewFlipper viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        for(String img : images) {
            addImageViewToFlipper(img, viewFlipper);
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showCoordinates(view);
                int currentIdx = viewFlipper.getDisplayedChild();
                Bitmap bmp = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getPath() + images[currentIdx]);
                String imageClass = classifyImage(bmp);
                TextView textView = (TextView) findViewById(R.id.text);
                textView.setText(imageClass);
            }
        });
    }

    private String classifyImage(Bitmap bmp) {
        Bitmap bmp1 = Bitmap.createScaledBitmap(bmp, 32, 32, false);
        ParamUnpacker pu = new ParamUnpacker();
        float[][][] mean = (float[][][]) pu.unpackerFunction(Environment.getExternalStorageDirectory().getPath()+"/Download/Data_Cifar10/mean.msg", float[][][].class);
        float[][][][] inputBatch = new float[1][3][32][32];

        for (int j = 0; j < 32; ++j) {
            for (int k = 0; k < 32; ++k) {
                int color = bmp1.getPixel(j, k);
                inputBatch[0][0][k][j] = (float) (blue(color));// - mean[0][j][k];
                inputBatch[0][1][k][j] = (float) (green(color));// - mean[1][j][k];
                inputBatch[0][2][k][j] = (float) (red(color));// - mean[2][j][k];
            }
        }

        float[][] output = (float[][]) conv.compute(inputBatch);
        String res = accuracy(output[0], labels, 3);
        return res;
    }

    private void readLabels() {
        labels = new String[1000];
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/Data_Cifar10/labels.txt");
        Scanner s;
        int iter = 0;

        try {
            s = new Scanner(f);
            while (s.hasNextLine()) {
                String str = s.nextLine();
                labels[iter++] = str;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class prepareModel extends AsyncTask<RenderScript, Void, CNNdroid> {
        @Override
        protected CNNdroid doInBackground(RenderScript... params) {
            try {
                conv = new CNNdroid(rs, Environment.getExternalStorageDirectory().getPath() + "/Download/Data_Cifar10/Cifar10_def.txt");
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
        int[] max_num = {-1, -1, -1, -1, -1};
        float[] max = new float[topk];
        for (int k = 0; k < topk ; ++k) {
            for (int i = 0; i < 10; ++i) {
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
            result += labels[max_num[i]]  + " , P = " + max[i] * 100 + " %\n\n";
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSION_REQ_CODE: {
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission_granted = true;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gpslogger, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
