package io.beedev.placerecognizer;

//Import other classes

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener,
        GoogleApiClient.OnConnectionFailedListener {

    //Navigation tab bar
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    TabHost tabHost;

    //Text to speech
    TextToSpeech t1;

    //Google API variables
    private static final String TAG = "PlacesAPIActivity";
    private static final String TTS = "Text to speeh";
    private static final String WIK = "WikiActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private static final int REQUEST_LOCATION = 0;
    private GoogleApiClient mGoogleApiClient;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String title = "Brandenburg_Gate";
    private static String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + title;

    private static MainActivity mInstance;

    //Firebase logout
    private final static String FIR = "firebase";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    DatabaseReference database = FirebaseDatabase.getInstance().getReference();

//CNNDROIDS
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


        //Enregistrer l'utilisateur dans la BDD
        FirebaseAuth user = FirebaseAuth.getInstance();
        final String userId = user.getCurrentUser().getUid();
        User User = new User(user.getCurrentUser().getDisplayName(), user.getCurrentUser().getEmail());
        database.child("users").child(userId).setValue(User);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .build();

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (firebaseAuth.getCurrentUser() == null)
                {
                    startActivity(new Intent(MainActivity.this,SignUp.class));
                }

            }
        };

        //Ask permission for Current place
        ;

        //Guess current place
        //findPlace();


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkConnection();
                findPlace();
                getCurrentPlace();

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

                //database.child("users").child(userId).child("places").setValue(description);
                Log.i(TAG, "value = " + mGoogleApiClient.isConnected());
                Log.i(TAG, "value2 = " + mGoogleApiClient.getContext());
                Log.i(WIK, url);
                Snackbar.make(view, "Mon bébé bumb d'amour", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Log.i("INFO", "Mon bébé bumb");
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

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //Function to get current place
    private void getCurrentPlace() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Log.i(TAG, String.format("Place '%s' has likelihood: '%g' and adresse '%s' and url is '%s' and latlon is '%s'",
                                placeLikelihood.getPlace().getName(),
                                placeLikelihood.getLikelihood(),
                                placeLikelihood.getPlace().getAddress(),
                                placeLikelihood.getPlace().getWebsiteUri(),
                                placeLikelihood.getPlace().getLatLng()));
                    }
                    likelyPlaces.release();
                    Log.i(TAG, String.valueOf(likelyPlaces.getStatus()));
                }
            });
            Log.i(TAG, "Permission Granted");
        }

    }

    //Trouver les infos d'un endroit via getPlaceByID
    private void findPlace() {

         final String placeId = "ChIJiQnyVcZRqEcRY0xnhE77uyY"; // Branderburg tor
         Log.i(TAG,placeId);
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (places.getStatus().isSuccess() && places.getCount() > 0) {
                            final Place myPlace = places.get(0);
                            Log.i(TAG, "Place found: " + myPlace.getName());
                            Log.i(TAG, "Place found: " + myPlace.getAddress());
                            Log.i(TAG, "Place found: " + myPlace.getPhoneNumber());
                            Log.i(TAG, "Place found: " + myPlace.getLocale());
                            Log.i(TAG, "Place found: " + myPlace.getWebsiteUri());
                            Log.i(TAG, "Place found: " + myPlace.getRating());
                        } else {
                            Log.e(TAG, "Place not found");
                        }
                        places.release();
                    }
                });

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
        mGoogleApiClient.connect();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connection status listener
        MainActivity.getInstance().setConnectivityListener(this);
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
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

        if (id == R.id.logOut){
            mAuth.signOut();
        }

        return super.onOptionsItemSelected(item);
    }

    //Check connection for Google API
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Google Places API connection failed with error code:" +
                        connectionResult.getErrorCode(),
                Toast.LENGTH_LONG).show();
    }

    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }


}






