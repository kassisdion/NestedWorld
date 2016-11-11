package com.nestedworld.nestedworld.ui.mainMenu.tabs.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.helpers.log.LogHelper;
import com.nestedworld.nestedworld.helpers.map.NestedWorldMap;
import com.nestedworld.nestedworld.helpers.permission.PermissionUtils;
import com.nestedworld.nestedworld.ui.base.BaseFragment;
import com.rey.material.widget.ProgressView;

import java.util.Arrays;
import java.util.Calendar;

import butterknife.BindView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MapFragment extends BaseFragment implements LocationListener {

    public final static String FRAGMENT_NAME = MapFragment.class.getSimpleName();

    @BindView(R.id.mapView)
    MapView mMapView;
    @BindView(R.id.progressView)
    ProgressView progressView;

    private NestedWorldMap mMap = null;
    private final static int MIN_TIME = 1000; //Minimum time between 2 update (in millisecond)
    private final static int MIN_DIST = 1; //Minimum distance between 2 update (in meter)
    /*
    ** Public method
     */
    public static void load(@NonNull final FragmentManager fragmentManager) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                .addToBackStack(FRAGMENT_NAME)
                .commit();
    }

    /*
    ** Life cycle
     */
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_tab_map;
    }

    @Override
    protected void init(@NonNull View rootView, @Nullable Bundle savedInstanceState) {

        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        //Start loading animation
        progressView.start();

        //init MapView
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();//We display the map immediately

        //retrieve GoogleMap from mapView
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {

                //Check if fragment hasn't been detach
                if (mContext == null) {
                    return;
                }

                //Init the mapView
                mMap = new NestedWorldMap(googleMap);
                initMap();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //check for REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS cause we've asked for 2 permission
        if (requestCode != PermissionUtils.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
            LogHelper.d(TAG, "Wrong request code");
            return;
        }

        //check if the 2 permission were granted
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                LogHelper.d(TAG, "Permission not granted");
                return;
            }
        }
        initMap();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        if (mMap != null && mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        if (mMap != null && mMapView != null) {
            mMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        if (mMap != null && mMapView != null) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        if (mMap != null && mMapView != null) {
            mMapView.onLowMemory();
        }
    }

    /*
    ** Location listener Implementation
     */
    @Override
    public void onLocationChanged(@NonNull final Location location) {
        //Check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }
        
        LogHelper.d(TAG, "set location to: " + location.getLatitude() + ", " + location.getLongitude());
        mMap.moveCamera(location.getLatitude(), location.getLongitude(), 12);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LogHelper.d(TAG, "status changed: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        LogHelper.d(TAG, "provider enable: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        LogHelper.d(TAG, "provider disable:" + provider);
    }

    /*
    ** private method
     */
    private void initMap() {
        //check if fragment hasn't been detach
        if (mContext == null) {
            return;
        }

        /*We check if we are allowed to retrieve the userPosition*/
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LogHelper.d(TAG, "ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission needed");

            //We ask for the permission (it we'll call onRequestPermissionsResult who will call initMap())
            PermissionUtils.askForPermissionsFromFragment(mContext, this, Arrays.asList(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION));
        } else {

            // Acquire a reference to the system Location Manager
            LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            // Register the listener with the Location Manager to receive location updates
            for (String provider : locationManager.getProviders(false)) {

                if (locationManager.isProviderEnabled(provider)) {
                    //onProviderEnabled() will not be called if the provider is already enable so we call it
                    onProviderEnabled(LocationManager.GPS_PROVIDER);

                    //get location fix
                    Location lastLocation = locationManager.getLastKnownLocation(provider);
                    if (lastLocation != null) {
                        //Update the NestedWorldMap
                        onLocationChanged(lastLocation);
                    }
                }

                locationManager.requestLocationUpdates(provider, MIN_TIME, MIN_DIST, this);
            }

            //Center the googleMap map on the userLocation
            mMap.getGoogleMap().setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.getGoogleMap().setMyLocationEnabled(true);
        }
    }
}
