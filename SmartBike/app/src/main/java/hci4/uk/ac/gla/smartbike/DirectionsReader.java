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

    /* a queue of upcoming steps. first element in the queue should always be the current step */
    private Queue<Step> upcomingSteps;

    /* user's previous location */
    private LatLng previousLocation;

    /* user's current location */
    private LatLng currentLocation;

    /* the step the user is currently following */
    private Step currentStep;

    public DirectionsReader() {
        upcomingSteps = new ConcurrentLinkedQueue<Step>();
        previousLocation = null;
        currentLocation = null;

        // load json file containing directions
        JSONObject directionsJson = loadJSONFromAsset();

        // rest of the method just parses JSON directions
        // and adds to the upcomingSteps queue, only if a step has a maneuver
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

    /* loads JSON from a file stored on the device */
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

    /* returns a list of points that define the polyline of the entire route */
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

    /* return the next instruction that the user should follow */
    public synchronized Instruction getNextInstruction(LatLng currentLocation) {
        // update current location
        this.currentLocation = currentLocation;

        // if previousLocation is null - journey has just began
        // set previousLocation equal to currentLocation
        if(previousLocation == null) {
            previousLocation = currentLocation;
        }

        // remove steps from the queue which are no longer relevant
        discardPastInstructions();

        // upcomingSteps is empty - means that there are no more steps to follow
        // i.e. user has reached destination
        if(upcomingSteps.isEmpty()) {
            return null;
        }

        // generate a proximity to the end of the currentStep
        currentStep = upcomingSteps.peek();
        Proximity proximity = getProximityTo(currentStep);

        // get distance to destination
        double distance = distanceBetween(currentLocation, currentStep.getEnd());

        // create a new Instruction that will be displayed to the user
        Instruction instruction = new Instruction(proximity, currentStep.getManeuver(), distance);

        // update previousLocation to equal currentLocation
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

    /* returns the proximity of the currentLoation to the end goal for the currentStep */
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

    /* determines if given Step is the step that the user is currently following.
     *  this is determined based on the value of the user's currentLocation and previousLocation */
    private boolean isCurrentStep(Step step) {
        Step nextStep = getNextStep();

        // if distance is smaller than two meters - we've not moved
        if(distanceBetween(currentLocation, previousLocation) <= 2) {
            return true;
        }

        // getting closer to current step
        if(distanceBetween(currentLocation, step.getEnd()) < distanceBetween(previousLocation, step.getEnd())) {
            return true;
        }

        // no next step and we're within 2m of goal
        if(nextStep == null && distanceBetween(currentLocation, step.getEnd()) <= 2) {
            return false;
        }

        // getting further away from current step
        // AND
        // getting further away from next step
        if(distanceBetween(currentLocation, step.getEnd()) > distanceBetween(previousLocation, step.getEnd()) &&
           distanceBetween(currentLocation, nextStep.getEnd()) > distanceBetween(previousLocation, nextStep.getEnd())) {
            return true;
        }

        // getting further away from current step
        // AND
        // getting closer to next step
        if(distanceBetween(currentLocation, step.getEnd()) > distanceBetween(previousLocation, step.getEnd()) &&
            distanceBetween(currentLocation, nextStep.getEnd()) < distanceBetween(previousLocation, nextStep.getEnd())) {
            return false;
        }

        return false;
    }

    private Step getNextStep() {
        Object[] steps = upcomingSteps.toArray();
        if(steps.length < 2)
            return null;

        return (Step) steps[1];
    }

    /* returns the distance (in meters) between two points */
    private double distanceBetween(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0];
    }

}
