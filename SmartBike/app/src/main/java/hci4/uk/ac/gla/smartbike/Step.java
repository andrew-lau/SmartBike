package hci4.uk.ac.gla.smartbike;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by andrewlau on 21/11/2014.
 */
public class Step {

    private Maneuver maneuver;
    private LatLng start;
    private LatLng end;

    public Step(Maneuver maneuver, LatLng start, LatLng end) {
        this.maneuver = maneuver;
        this.start = start;
        this.end = end;
    }

    public Maneuver getManeuver() {
        return maneuver;
    }

    public LatLng getStart() {
        return start;
    }

    public LatLng getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return maneuver + " " + start + " " + end;
    }

}
