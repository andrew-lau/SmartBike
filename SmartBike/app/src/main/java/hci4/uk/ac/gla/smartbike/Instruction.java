package hci4.uk.ac.gla.smartbike;

/**
 * Created by andrewlau on 21/11/2014.
 */
public class Instruction {

    private Proximity proximity;
    private Maneuver maneuver;
    private double distance;

    public Instruction(Proximity proximity, Maneuver maneuver, double distance) {
        this.proximity = proximity;
        this.maneuver = maneuver;
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public Proximity getProximity() {
        return proximity;
    }

    public Maneuver getManeuver() {
        return maneuver;
    }

    @Override
    public String toString() {
        return proximity + " " + maneuver + " "  + distance;
    }

}
