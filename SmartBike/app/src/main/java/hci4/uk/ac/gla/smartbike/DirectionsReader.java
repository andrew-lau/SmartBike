package hci4.uk.ac.gla.smartbike;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by andrewlau on 21/11/2014.
 */
public class DirectionsReader {

    private Queue<Step> upcomingSteps;

    private LatLng previousLocation;
    private LatLng currentLocation;
    private Step currentStep;

    public DirectionsReader() {
        upcomingSteps = new ConcurrentLinkedQueue<Step>();
        previousLocation = null;
        currentLocation = null;

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
                    // check if step has a maneuver
                    try {
                        String maneuverString = step.getString("maneuver");
                        Maneuver maneuver = null;
                        if(maneuverString.indexOf("left") != -1) {
                            maneuver = Maneuver.LEFT;
                        }
                        if(maneuverString.indexOf("right") != -1) {
                            maneuver = Maneuver.RIGHT;
                        }

                        if(maneuver != null) {
                            JSONObject startLocation = step.getJSONObject("start_location");
                            LatLng start = new LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng"));

                            JSONObject endLocation = step.getJSONObject("start_location");
                            LatLng end = new LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng"));

                            upcomingSteps.add(new Step(maneuver, start, end));
                        }

                    } catch(JSONException e) {
                        //e.printStackTrace();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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

        return points;
    }

    public synchronized Instruction getNextInstruction(LatLng currentLocation) {
        this.currentLocation = currentLocation;
        if(previousLocation == null) {
            previousLocation = currentLocation;
        }

        discardPastInstructions();
        if(upcomingSteps.isEmpty()) {
            return null;
        }

        currentStep = upcomingSteps.peek();
        Proximity proximity = getProximityTo(currentStep);
        Instruction instruction = new Instruction(proximity, currentStep.getManeuver());

        this.previousLocation = currentLocation;
        return instruction;
    }

    private void discardPastInstructions() {
        for(Step step : upcomingSteps) {
            if(!isCurrentStep(step)) {
                upcomingSteps.remove();
            }
        }
    }

    private Proximity getProximityTo(Step step) {
        double distance = distanceBetween(currentLocation, currentStep.getEnd());
        System.out.println("*************** " + distance);
        if(distance > 105) {
            System.out.println("*************** FAR");
            return Proximity.FAR;
        }else if(distance <= 105 && distance > 55) {
            System.out.println("*************** SEMI_FAR");
            return Proximity.SEMI_FAR;
        } else if(distance <= 55 && distance > 45) {
            System.out.println("*************** MEH");
            return Proximity.MEH;
        } else if(distance <= 45 && distance > 20) {
            System.out.println("*************** CLOSE");
            return Proximity.CLOSE;
        } else if(distance <= 20 && distance > 5) {
            System.out.println("*************** SUPER_CLOSE");
            return Proximity.SUPER_CLOSE;
        } else {
            System.out.println("*************** NOW");
            return Proximity.NOW;
        }
    }

    private boolean isCurrentStep(Step step) {
        if(distanceBetween(currentLocation, previousLocation) <= 10) {
            return true;
        }
        if(distanceBetween(currentLocation, step.getEnd()) < distanceBetween(previousLocation, step.getEnd())) {
            return true;
        }
        return false;
    }

    private double distanceBetween(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0];
    }

}
