package hci4.uk.ac.gla.smartbike;

/**
 * Created by andrewlau on 21/11/2014.
 */
public class Instruction {

    private Proximity proximity;
    private Maneuver maneuver;
    private double distance;

    public double getDistance() {
        return distance;
    }

    public Instruction(Proximity proximity, Maneuver maneuver) {
        this.proximity = proximity;
        this.maneuver = maneuver;
    }

    public Proximity getProximity() {
        return proximity;
    }

    public Maneuver getManeuver() {
        return maneuver;
    }

    @Override
    public String toString() {
        return proximity + " " + maneuver;
    }

}
