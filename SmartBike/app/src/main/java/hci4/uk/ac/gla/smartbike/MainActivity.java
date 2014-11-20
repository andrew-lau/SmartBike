package hci4.uk.ac.gla.smartbike;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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

import org.apache.commons.logging.Log;


public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private GoogleMap googleMap;
    private Marker marker;



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
        FragmentManager parentFragmentManager = getFragmentManager();
        Fragment wrapper = parentFragmentManager.findFragmentById(R.id.mapwrapper);
        FragmentManager childFragmentManager = wrapper.getChildFragmentManager();

        MapFragment mapFragment = (MapFragment) childFragmentManager.findFragmentById(R.id.map22);

        MapsInitializer.initialize(this);

        googleMap = mapFragment.getMap();
        if (googleMap == null){
            System.out.println("yo");
        }


        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(55.8554602,-4.2324586))
                .zoom(19)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(70)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.setBuildingsEnabled(true);

        marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(55.8554602,-4.2324586)));


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
    public void onLocationChanged(Location location) {
        if (location != null){
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
            marker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
