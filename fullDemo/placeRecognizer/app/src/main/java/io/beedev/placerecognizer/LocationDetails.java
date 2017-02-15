package io.beedev.placerecognizer;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by romaingranger on 24/01/2017.
 */
public class LocationDetails {

    public String uid;
    public String title;
    public String descritpion;
    public String date;

    public LocationDetails() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public LocationDetails(String uid, String title, String descritpion, String date) {
        this.uid = uid;
        this.title = title;
        this.descritpion = descritpion;
        this.date = date;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("title", title);
        result.put("description", descritpion);
        result.put("date", date);

        return result;
    }


}
