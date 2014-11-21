package hci4.uk.ac.gla.smartbike;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrewlau on 21/11/2014.
 */
public class DirectionsReader {

    private JSONObject loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = SmartBikeApp.getAppAssets().open("directions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        try {
            return new JSONObject(json);
        } catch(JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<LatLng> getPoints() {
        ArrayList<LatLng> points = new ArrayList<LatLng>();

        JSONObject directionsJson = loadJSONFromAsset();
        try {
            JSONArray routes = directionsJson.getJSONArray("routes");
            JSONObject route = routes.getJSONObject(0);
            JSONArray legs = route.getJSONArray("legs");
            for(int i=0; i < legs.length(); i++) {
                JSONObject leg = legs.getJSONObject(i);
                JSONArray steps = leg.getJSONArray("steps");
                for(int j=0; j < steps.length(); j++) {
                    JSONObject step = steps.getJSONObject(j);
                    JSONObject polyline = step.getJSONObject("polyline");
                    String polylineStr = polyline.getString("points");
                    points.addAll(PolyUtil.decode(polylineStr));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        System.out.println(points);
        return points;
    }

}
