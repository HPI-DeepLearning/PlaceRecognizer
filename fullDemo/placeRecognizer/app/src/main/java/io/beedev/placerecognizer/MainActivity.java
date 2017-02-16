package io.beedev.placerecognizer;

//Import other classes

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener {

    //Text to speech
    TextToSpeech t1;

    //Google API variables
    private static final String TAG = "PlacesAPIActivity";
    private static final String TTS = "Text to speech";
    private static final String WIK = "WikiActivity";
    private static final int REQUEST_LOCATION = 0;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String title = "Brandenburg_Gate";
    private static String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;

    private static MainActivity mInstance;

//CNNDROID
    // RenderScript rs = RenderScript.create(this);
    // CNNdroid conv = new CNNdroid(rs,netDefDir);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Menu layout

        //Text to speech variable declaration
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        //Guess current place
        //findPlace();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkConnection();

                String toSpeak = null;
                try {
                    toSpeak = new GetWiki().execute("Brandenburg_Gate").get().descritpion;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null , "test");
                Log.d(TTS, String.valueOf(t1.isSpeaking()));

                Log.i(WIK, url);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission granted");

                } else {
                    Log.i(TAG, "Permission denied");
                }
                return;
            }
        }
    }

    //Check internet connection
    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        if (isConnected){
            Log.i("connectionCheck", "Is connected");
        }
        else{
            Log.i("connectionCheck", "Is not connected");
        }
    }


    public static synchronized MainActivity getInstance() {
        return mInstance;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.connectivityReceiverListener = listener;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connection status listener
        setConnectivityListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }



    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            startActivity(new Intent(MainActivity.this,SettingsActivity.class));
        }

        if (id == R.id.tabbed)
        {
            startActivity(new Intent(MainActivity.this, Home.class));
        }

        return super.onOptionsItemSelected(item);
    }

    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }
}






