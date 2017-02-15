package io.beedev.placerecognizer;

import android.os.AsyncTask;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

import io.beedev.placerecognizer.MainActivity.*;

import io.beedev.placerecognizer.MainActivity;


/**
 * Created by romaingranger on 24/01/2017.
 */

//Json parser for wikipedia
public class GetWiki extends AsyncTask<String, Object, LocationDetails> {

    DatabaseReference database = FirebaseDatabase.getInstance().getReference();

    private static final String WIK = "WikiActivity";
    //private static final String title = "Brandenburg_Gate";
    //private static final String title = "Fernsehturm_Berlin";


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.i(WIK,"Début async");
    }

    @Override
    protected LocationDetails doInBackground(String... strings) {
        String urlTitle = strings[0];
        String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + urlTitle;
        FirebaseAuth user = FirebaseAuth.getInstance();
        final String userId = user.getCurrentUser().getUid();
        HttpHandler sh = new HttpHandler();

        // Making a request to url and getting response
        String jsonStr = sh.makeServiceCall(url);

        Log.e(WIK, "Response from url: " + jsonStr);


        if (jsonStr != null) {
                try {

                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject query = jsonObj.getJSONObject("query");
                    JSONObject pages = query.getJSONObject("pages");
                    Iterator<String> keys = pages.keys();
                    String pageId= keys.next();
                    JSONObject page = pages.getJSONObject(pageId);

                    String title = page.getString("title");
                    String extract = page.getString("extract");
                    Calendar now = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String date = sdf.format(new Date());



                   // String pageid = pages.getString("pages");


                    String key = database.child("users").child(userId).child("places").push().getKey();
                    LocationDetails location = new LocationDetails(userId,title,extract,date);
                    Map<String, Object> postValues = location.toMap();

                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/users/" + userId + "/places/" + key, postValues);

                    database.updateChildren(childUpdates);

                    //database.child("users").child(userId).child("places").push(id).setValue(location);


                    Log.e(WIK, "Response from url: " + query);
                    Log.e(WIK, "Response from url: " + pages.put("pageid",0));
                    Log.e(WIK, "Response from url: " + page);
                    Log.e(WIK, "Response from url: " + title);
                    Log.e(WIK, "Response from url: " + extract);
                    Log.e(WIK, "Response from url: " + location);


                    /*String pageid = pages.getString("pageid");
                    String title = pages.getString("title");
                     */
                   // String summary = pages.getString("extract");

                   //Log.i(WIK,summary);


                return location;
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

        return null;
    }

    @Override
    protected void onPostExecute(LocationDetails result) {
        super.onPostExecute(result);
        Log.i(WIK,"good");
    }



}

