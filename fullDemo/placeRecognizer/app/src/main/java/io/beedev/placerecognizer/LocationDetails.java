package io.beedev.placerecognizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by romaingranger on 24/01/2017.
 */

public class LocationDetails {

    public String title;
    public String descritpion;
    public String date;

    public LocationDetails() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public LocationDetails(String title, String descritpion, String date) {
        this.title = title;
        this.descritpion = descritpion;
        this.date = date;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("description", descritpion);
        result.put("date", date);

        return result;
    }


}
