package hci4.uk.ac.gla.smartbike;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener {

    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private GoogleMap googleMap;
    private Marker marker;
    private int bearing; // value in radians
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magneticField;

    /* current location of the user */
    private LatLng location;
    private DirectionsReader directionsReader;
    /* current Instruction user is following */
    private Instruction currentInstruction;

    private Sounds sounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        // configure location
        locationClient = new LocationClient(this, this, this);
        locationRequest = LocationRequest.create();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0);

        locationClient.connect();

        // configure map
        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.locationMap);

        MapsInitializer.initialize(this);

        googleMap = mapFragment.getMap();

        location = new LatLng(55.8554602, -4.2324586);
        bearing = 90;
        updateCamera();
        googleMap.setBuildingsEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);

        marker = googleMap.addMarker(new MarkerOptions().position(location));

        // configure sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // get directions reader
        directionsReader = new DirectionsReader();

        // draw route
        List<LatLng> points = directionsReader.getPoints();
        PolylineOptions lineOptions = new PolylineOptions()
                .color(Color.CYAN);
        for(LatLng point : points) {
            lineOptions.add(point);
        }
        googleMap.addPolyline(lineOptions);

        // load sounds
        sounds = new Sounds(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy(){
        locationClient.disconnect();
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, this);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onLocationChanged(Location loc) {

        if (loc != null){
            location = new LatLng(loc.getLatitude(), loc.getLongitude());
            currentInstruction = directionsReader.getNextInstruction(location);

            provideDirectionFeedback();

            updateCamera();
            marker.setPosition(location);
        }
    }

    private void provideDirectionFeedback() {
        ImageView theArrow = (ImageView) findViewById(R.id.arrow);
        TextView theDistance = (TextView) findViewById(R.id.distance);

        if(currentInstruction == null) {
            sounds.playReachedDestination();
            theArrow.setImageResource(R.drawable.done);
            theDistance.setText("");
            return;
        }

        double distance= currentInstruction.getDistance();

        if(currentInstruction.getDistance() > 55) {
            theArrow.setImageResource(R.drawable.forward);
        } else if(currentInstruction.getManeuver() == Maneuver.LEFT) {
            if(distance <= 55 && distance >= 45) {
                sounds.playLeft50m();
            } else if(distance <= 15 && distance >= 5) {
                sounds.playLeft10m();
            } else if(distance <= 5) {
                sounds.playLeft5m();
            }
            theArrow.setImageResource(R.drawable.leftnew);
        } else {
            theArrow.setImageResource(R.drawable.rightnew);
        }
        theDistance.setText(((int)Math.round(distance)) + "m");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    float[] mGravity;
    float[] mGeomagnetic;
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                bearing = 90 + (int) Math.round(Math.toDegrees(orientation[0]));
                //updateCamera();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not needed
    }

    private void updateCamera() {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)
                .zoom(19)                   // Sets the zoom
                .bearing(bearing)           // Sets the orientation of the camera to east
                .tilt(70)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}
