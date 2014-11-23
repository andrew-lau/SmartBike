package hci4.uk.ac.gla.smartbike;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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
    private LatLng location;
    private DirectionsReader directionsReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        // configure location
        locationClient = new LocationClient(this, this, this);
        locationRequest = LocationRequest.create();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(100);

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

        marker = googleMap.addMarker(new MarkerOptions().position(location));

        // configure sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // get directions reader
        directionsReader = new DirectionsReader();

        // draw route
        List<LatLng> points = directionsReader.getPoints();
        PolylineOptions lineOptions = new PolylineOptions();
        for(LatLng point : points) {
            lineOptions.add(point);
        }
        googleMap.addPolyline(lineOptions);
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

            Instruction instruction = directionsReader.getNextInstruction(location);
            FragmentManager fragmentManager = getFragmentManager();
            TextView debug = (TextView) findViewById(R.id.debug);
            if(instruction != null)
                debug.setText(instruction.toString());
            else
                debug.setText("Reached destination!!!!!!!!!");

            updateCamera();
            marker.setPosition(location);
        }
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
