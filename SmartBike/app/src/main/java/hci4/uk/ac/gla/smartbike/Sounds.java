package hci4.uk.ac.gla.smartbike;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Created by Velizar on 23.11.2014 г..
 */
public class Sounds {

    private MediaPlayer beepLeft;
    private MediaPlayer left5m;
    private MediaPlayer left10m;
    private MediaPlayer left50m;
    private MediaPlayer reachedDestination;

    private boolean hasReachedDestination;
    private boolean isWithin50m;
    private boolean isWithin10m;
    private boolean isWithin5m;

    public Sounds(Context context) {
        beepLeft = MediaPlayer.create(context, R.raw.beep_left);
        left5m = MediaPlayer.create(context, R.raw.left_5m);
        left10m = MediaPlayer.create(context, R.raw.left_10m);
        left50m = MediaPlayer.create(context, R.raw.left_50m);
        reachedDestination = MediaPlayer.create(context, R.raw.reached_destination);

        hasReachedDestination = false;
        isWithin50m = false;
        isWithin10m = false;
        isWithin5m = false;
    }

    public void playBeepLeft() {
        beepLeft.start();
    }

    public void playLeft5m() {
        if(!isWithin5m) {
            left5m.start();
            isWithin5m = true;
            isWithin10m = false;
            isWithin50m = false;
        }
    }

    public void playLeft10m() {
        if(!isWithin10m) {
            left10m.start();
            isWithin10m = true;
            isWithin5m = false;
            isWithin5m = false;
        }
    }

    public void playLeft50m() {
        if(!isWithin50m) {
            left50m.start();
            isWithin50m = true;
            isWithin10m = false;
            isWithin5m = false;
        }
    }

    public void playReachedDestination() {
        if(!hasReachedDestination) {
            reachedDestination.start();
            hasReachedDestination = true;
        }
    }

}
