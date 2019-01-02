package ca.sajal.g2_g_app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MatchedGeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.RoadElement;
import com.here.android.mpa.guidance.LaneInformation;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;
import com.here.android.mpa.guidance.VoiceSkin;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.positioning.StatusListener;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.prefetcher.MapDataPrefetcher;

import ca.sajal.g2_g_app.Functions;

import static com.here.android.mpa.guidance.NavigationManager.NaturalGuidanceMode.JUNCTION;


public class MainActivity extends FragmentActivity implements PositioningManager.OnPositionChangedListener, Map.OnTransformListener {
    /*Permission Ask & Grant*/
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

    /*Variable Declaration*/
    public Map map = null;
    Functions func = new Functions(this);

    private SupportMapFragment mapFragment = null;
    private PositioningManager mPositioningManager;
    private LocationDataSourceHERE mHereLocation;
    private boolean mTransforming;
    private Runnable mPendingUpdate;
    public NavigationManager m_navigationManager;
    private GeoBoundingBox m_geoBoundingBox;
    private Button m_naviControlButton;
    private Route m_route = null;
    FirebaseDatabase db =FirebaseDatabase.getInstance();
    DatabaseReference myRef = db.getReference("cities");
    private boolean fetchingDataInProgress = false;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }
    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    /*Turn by Turn Navi & Positioning*/
    @Override
    public void onMapTransformStart() {
        mTransforming = true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mPositioningManager != null) {
            mPositioningManager.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPositioningManager != null) {
            mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR);
        }
    }

    @Override
    public void onPositionUpdated(final PositioningManager.LocationMethod locationMethod, final GeoPosition geoPosition, final boolean mapMatched) {
        final GeoCoordinate coordinate = geoPosition.getCoordinate();

        if (mTransforming) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    onPositionUpdated(locationMethod, geoPosition, mapMatched);

                }
            };
        } else {
            map.setCenter(coordinate, Map.Animation.BOW);
            func.updateLocationInfo(locationMethod, geoPosition);

        }
        if (PositioningManager.getInstance().getRoadElement() == null && !fetchingDataInProgress) {
            GeoBoundingBox areaAround = new GeoBoundingBox(geoPosition.getCoordinate(), 500, 500);
            MapDataPrefetcher.getInstance().fetchMapData(areaAround);
            fetchingDataInProgress = true;
        }
        if (geoPosition.isValid() && geoPosition instanceof MatchedGeoPosition) {

            MatchedGeoPosition mgp = (MatchedGeoPosition) geoPosition;
            int currentSpeedLimitTransformed = 0;

            if (mgp.getRoadElement() != null) {
                double currentSpeedLimit = mgp.getRoadElement().getSpeedLimit();
                changetoCurrentspeedLimit((int) (currentSpeedLimit));
                changetoCurrentspeed(geoPosition.getSpeed(),(int) (currentSpeedLimit));
            }else{
                changetoCurrentspeedLimit((int) (currentSpeedLimitTransformed));
            }
        }
    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
        // ignored
    }
    @Override
    public void onMapTransformEnd(MapState mapState) {
        mTransforming = false;
        if (mPendingUpdate != null) {
            mPendingUpdate.run();
            mPendingUpdate = null;
        }
    }



    /*Map Initialized*/
    private void initialize() {

            setContentView(R.layout.activity_main);

            // Search for the map fragment to finish setup by calling init().
            mapFragment = getMapFragment();

            // Set up disk cache path for the map service for this application
            boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                    getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                    "ca.sajal.mapService");

            if (!success) {
                Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.", Toast.LENGTH_LONG);
            } else {
                mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        if (error == OnEngineInitListener.Error.NONE) {
                            // retrieve a reference of the map from the map fragment
                            map = mapFragment.getMap();
                            // Set the map center to the Vancouver region (no animation)
                            map.setCenter(new GeoCoordinate(43.677724, -79.715789, 0.0),
                                    Map.Animation.NONE);
                            // Set the zoom level to the average between min and max
                            map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                            map.addTransformListener(MainActivity.this);
                            mPositioningManager = PositioningManager.getInstance();
                            mHereLocation = LocationDataSourceHERE.getInstance(
                                    new StatusListener() {
                                        @Override
                                        public void onOfflineModeChanged(boolean offline) {
                                            // called when offline mode changes
                                        }

                                        @Override
                                        public void onAirplaneModeEnabled() {
                                            // called when airplane mode is enabled
                                        }

                                        @Override
                                        public void onWifiScansDisabled() {
                                            // called when Wi-Fi scans are disabled
                                        }

                                        @Override
                                        public void onBluetoothDisabled() {
                                            // called when Bluetooth is disabled
                                        }

                                        @Override
                                        public void onCellDisabled() {
                                            // called when Cell radios are switch off
                                        }

                                        @Override
                                        public void onGnssLocationDisabled() {
                                            // called when GPS positioning is disabled
                                        }

                                        @Override
                                        public void onNetworkLocationDisabled() {
                                            // called when network positioning is disabled
                                        }

                                        @Override
                                        public void onServiceError(ServiceError serviceError) {
                                            // called on HERE service error
                                        }

                                        @Override
                                        public void onPositioningError(PositioningError positioningError) {
                                            // called when positioning fails
                                        }

                                        @Override
                                        public void onWifiIndoorPositioningNotAvailable() {
                                            // called when running on Android 9.0 (Pie) or newer
                                        }
                                    });
                            if (mHereLocation == null) {
                                Toast.makeText(MainActivity.this, "LocationDataSourceHERE.getInstance(): failed, exiting", Toast.LENGTH_LONG).show();
                                finish();
                            }
                            mPositioningManager.setDataSource(mHereLocation);
                            mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(
                                    MainActivity.this));
                            // start position updates, accepting GPS, network or indoor positions
                            if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR)) {
                                mapFragment.getPositionIndicator().setVisible(true);
                            } else {
                                Toast.makeText(MainActivity.this, "PositioningManager.start: failed, exiting", Toast.LENGTH_LONG).show();
                                finish();
                            }
                            processing();
                        } else {
                            System.out.println("ERROR: Cannot initialize Map Fragment");
                        }
                    }
                });
            }
        }





    /*Where everything happens*/
    private void processing (){

        m_navigationManager = NavigationManager.getInstance();
        setNavOptions(m_navigationManager);
        final List<GeoCoordinate> Points = new ArrayList<GeoCoordinate>();
        checkDb(new fbCallback() {
            @Override
            public void onCallback(List<GeoCoordinate> coordinates) {
                Points.addAll(coordinates);
                func.createRoute(Points, map);
                final List <GeoCoordinate> arr = Points;
                m_naviControlButton = (Button) findViewById(R.id.naviCtrlButton);
                m_naviControlButton.setText("start");
                m_naviControlButton.setOnClickListener(new View.OnClickListener() {
                    @Override

                    public void onClick(View v) {

                        if (m_route == null) {
                            m_route= func.createRoute(arr, map);
                            func.startNavigation(map, m_naviControlButton, m_navigationManager, MainActivity.this);
                        } else {
                            m_navigationManager.stop();
                            /*
                             * Restore the map orientation to show entire route on screen
                             */
                            m_naviControlButton.setText("start");
                            m_route = null;
                        }
                    }
                });
            }
        });




    }
    private void downloadVoice(final long skin_id) {
        // kick off the download for a voice skin from the backend

        VoiceCatalog.getInstance().downloadVoice(skin_id, new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(getApplicationContext(), "Failed downloading voice skin", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Voice skin downloaded and activated", Toast.LENGTH_LONG).show();

                    // set usage of downloaded voice
                    NavigationManager.getInstance().getVoiceGuidanceOptions().setVoiceSkin(VoiceCatalog.getInstance().getLocalVoiceSkin(skin_id));
                    Log.d("DEBUGA", "Voice set");
                    downloadCat();

                }
            }
        });


    }
    private void downloadCat (){
        VoiceCatalog.getInstance().downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(getApplicationContext(), "Failed downloading voice skin", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Here", Toast.LENGTH_LONG).show();
                    List<VoicePackage> vp = VoiceCatalog.getInstance().getCatalogList();
                    String info;
                    for (int i=0; i<vp.size();i++){

                        info = vp.get(i).getName() + "-------   " +  (int) vp.get(i).getId() +"---------   " + vp.get(i).getQuality() +"------  " + vp.get(i).getGender();
                        if (info.toLowerCase().indexOf("english") != -1) {
                            Log.d("DEBUGA", info);

                        }
                    }
                }
            }
        });
    }
    private void setNavOptions(NavigationManager m_navigationManager){
        ArrayList<NavigationManager.NaturalGuidanceMode> arrayList = new ArrayList<NavigationManager.NaturalGuidanceMode>();
        arrayList.add(NavigationManager.NaturalGuidanceMode.JUNCTION);
        arrayList.add(NavigationManager.NaturalGuidanceMode.STOP_SIGN);
        arrayList.add(NavigationManager.NaturalGuidanceMode.TRAFFIC_LIGHT);
        EnumSet<NavigationManager.NaturalGuidanceMode> enumSet = EnumSet.copyOf(arrayList);
        m_navigationManager.setNaturalGuidanceMode(enumSet);
        downloadVoice(10101);
        m_navigationManager.setRealisticViewMode(NavigationManager.RealisticViewMode.NIGHT);
        Log.d("DEBUGA", "nav options set");


    }
    public void checkDb(final fbCallback fbCallback){ //route and city options
        Intent retrieve = getIntent(); //Retrieves the string city and route elements from firebasedata class
        String selectedRoute =  retrieve.getStringExtra("routeCallback"); //saves the route to the selectedRoute variable as a dynamic variable
        String selectedItem =  retrieve.getStringExtra("cityCallback"); //saves the city to the selectedItem as a dynamic variable

        final String city = selectedItem;
        final String route  = selectedRoute;


            myRef.child(city).child(route).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<GeoCoordinate> coordinates = new ArrayList<GeoCoordinate>();
                    String[] lat = dataSnapshot.child("Lat").getValue().toString().split(",");
                    String[] lng = dataSnapshot.child("Lng").getValue().toString().split(",");
                    for (int i = 0; i < lat.length; i++) {
                        coordinates.add(new GeoCoordinate(Double.parseDouble(lat[i]), Double.parseDouble(lng[i]))); //saves Lat & Long as 2D Array
                    }

                    fbCallback.onCallback(coordinates);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });



    }
    public interface fbCallback{
        void onCallback(List<GeoCoordinate> list);

    }
    public void changetoCurrentspeed(double speed, int speedLimit){
        DecimalFormat df = new DecimalFormat("###.##");
        TextView tv = (TextView) findViewById(R.id.currentSpeed);
        String s = df.format(speed*3.6) + " km/h";
        tv.setText(s);
        speed = (int) Math.round(speed * 3.6);
        speedLimit = (int) Math.round(speedLimit*.36)*10;
        int difference = Math.abs(((int) (speed - speedLimit)));
        int severity = 0;
        if (difference>5){
            severity = difference*25;
            if (difference>10){
                severity = 255;
            }
            tv.setBackgroundColor(Color.rgb(severity, 0, 0));
        }else{
            tv.setBackgroundColor(Color.rgb(0, 0, 0));
        }
    }
    public void changetoCurrentspeedLimit(double speed){
        DecimalFormat df = new DecimalFormat("###");
        TextView tv = (TextView) findViewById(R.id.speedLimit);
        speed = Math.round(speed*.36)*10;
        String s = df.format(speed) + " km/h";
        tv.setText(s);

    }
    MapDataPrefetcher.Adapter prefetcherListener = new MapDataPrefetcher.Adapter() {
        @Override
        public void onStatus(int i, PrefetchStatus prefetchStatus) {
            super.onStatus(i, prefetchStatus);
        }
    };


}
