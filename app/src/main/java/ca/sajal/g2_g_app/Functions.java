package ca.sajal.g2_g_app;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.RoadElement;
import com.here.android.mpa.guidance.LaneInformation;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.prefetcher.MapDataPrefetcher;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;
import ca.sajal.g2_g_app.MainActivity;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;



public class Functions extends FragmentActivity {


    // text view instance for showing location information
    private TextView mLocationInfo;
    private MapRoute mapRoute;
    private GeoBoundingBox m_geoBoundingBox;
    private Route m_route;
    private boolean m_foregroundServiceStarted;
    private Context context;
    private NavigationManager manNavi;
    private Map map2;
    private int speedLimit = 0;
    private int speed;




    public Functions(Context context){
        this.context = context;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /*Route Creation*/
    public Route createRoute(List Points, final Map map) {

        /* Initialize a CoreRouter */
        CoreRouter coreRouter = new CoreRouter();

        /* Initialize a RoutePlan */
        RoutePlan routePlan = new RoutePlan();

        /*
         * Initialize a RouteOption.HERE SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        RouteOptions routeOptions = new RouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(RouteOptions.Type.SHORTEST);
        /* Calculate 1 route. */
        routeOptions.setRouteCount(1);
        /* Finally set the route option */
        routePlan.setRouteOptions(routeOptions);
        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
        List<GeoCoordinate> Point = Points;
        for (int i = 0; i < Points.size(); i++) {
            routePlan.addWaypoint(new RouteWaypoint(Point.get(i)));
            MapMarker mapMarker = new MapMarker(Point.get(i));
            map.addMapObject(mapMarker);
        }
        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter.calculateRoute(routePlan,
                new Router.Listener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onProgress(int i) {
                        /* The calculation progress can be retrieved in this callback. */
                    }

                    @Override
                    public void onCalculateRouteFinished(List<RouteResult> routeResults,
                                                         RoutingError routingError) {
                        /* Calculation is done. Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            if (routeResults.get(0).getRoute() != null) {
                                /* Create a MapRoute so that it can be placed on the map */
                                mapRoute = new MapRoute(routeResults.get(0).getRoute());

                                /* Show the maneuver number on top of the route */
                                mapRoute.setManeuverNumberVisible(true);

                                /* Add the MapRoute to the map */
                                map.addMapObject(mapRoute);

                                /*
                                 * We may also want to make sure the map view is orientated properly
                                 * so the entire route can be easily seen.
                                 */
                                GeoBoundingBox gbb = routeResults.get(0).getRoute()
                                        .getBoundingBox();
                                map.zoomTo(gbb, Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION);
                                m_route = routeResults.get(0).getRoute();

                            } else {
                                Toast.makeText(context,
                                        "Error:route results returned is not valid",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(context,
                                    "Error:route calculation returned error code: " + routingError,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
        return m_route;
    }

    /*Location Update*/
    public void updateLocationInfo(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition) {
        if (mLocationInfo == null) {
            return;
        }
        final StringBuffer sb = new StringBuffer();
        final GeoCoordinate coord = geoPosition.getCoordinate();
        sb.append("Type: ").append(String.format(Locale.US, "%s\n", locationMethod.name()));
        sb.append("Coordinate:").append(String.format(Locale.US, "%.6f, %.6f\n", coord.getLatitude(), coord.getLongitude()));
        if (coord.getAltitude() != GeoCoordinate.UNKNOWN_ALTITUDE) {
            sb.append("Altitude:").append(String.format(Locale.US, "%.2fm\n", coord.getAltitude()));
        }
        if (geoPosition.getHeading() != GeoPosition.UNKNOWN) {
            sb.append("Heading:").append(String.format(Locale.US, "%.2f\n", geoPosition.getHeading()));
        }
        if (geoPosition.getSpeed() != GeoPosition.UNKNOWN) {
            sb.append("Speed:").append(String.format(Locale.US, "%.2fm/s\n", geoPosition.getSpeed()));
        }
        if (geoPosition.getBuildingName() != null) {
            sb.append("Building: ").append(geoPosition.getBuildingName());
            if (geoPosition.getBuildingId() != null) {
                sb.append(" (").append(geoPosition.getBuildingId()).append(")\n");
            } else {
                sb.append("\n");
            }
        }
        if (geoPosition.getFloorId() != null) {
            sb.append("Floor: ").append(geoPosition.getFloorId()).append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        mLocationInfo.setText(sb.toString());
    }

    /*Turn by Turn Stuff*/
    private void startForegroundService() {
        if (!m_foregroundServiceStarted) {
            m_foregroundServiceStarted = true;
            Intent startIntent = new Intent(context, ForegroundService.class);
            startIntent.setAction(ForegroundService.START_ACTION);
            context.startService(startIntent);
        }
    }
    private void stopForegroundService() {
        if (m_foregroundServiceStarted) {
            m_foregroundServiceStarted = false;
            map2.setMapScheme(Map.Scheme.NORMAL_DAY);
            Intent stopIntent = new Intent(context, ForegroundService.class);
            stopIntent.setAction(ForegroundService.STOP_ACTION);
            context.startService(stopIntent);
        }
    }
    public void startNavigation(final Map map, Button m_naviControlButton, final NavigationManager m_navigationManager, Context context) {
        m_naviControlButton.setText("stop");
        /* Configure Navigation manager to launch navigation on current map */
        m_navigationManager.setMap(map);
        map.setMapScheme(Map.Scheme.CARNAV_NIGHT);
        map2 = map;
        /*
         * Start the turn-by-turn navigation.Please note if the transport mode of the passed-in
         * route is pedestrian, the NavigationManager automatically triggers the guidance which is
         * suitable for walking. Simulation and tracking modes can also be launched at this moment
         * by calling either simulate() or startTracking()
         */

        /* Choose navigation modes between real time navigation and simulation */
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Navigation");
        alertDialogBuilder.setMessage("Choose Mode");
        alertDialogBuilder.setNegativeButton("Navigation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                m_navigationManager.startNavigation(m_route);
                map.setTilt(60);
                startForegroundService();
            };
        });
        alertDialogBuilder.setPositiveButton("Simulation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                m_navigationManager.simulate(m_route,200);//Simualtion speed is set to 60 m/s
                map.setTilt(60);
                startForegroundService();
            };
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        /*
         * Set the map update mode to ROADVIEW.This will enable the automatic map movement based on
         * the current location.If user gestures are expected during the navigation, it's
         * recommended to set the map update mode to NONE first. Other supported update mode can be
         * found in HERE Android SDK API doc
         */
        m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);



        /*
         * NavigationManager contains a number of listeners which we can use to monitor the
         * navigation status and getting relevant instructions.In this example, we will add 2
         * listeners for demo purpose,please refer to HERE Android SDK API documentation for details
         */
        manNavi = m_navigationManager;
        addNavigationListeners(m_navigationManager);
    }

    /*Navi Listener & TTS*/
    private void addNavigationListeners(NavigationManager m_navigationManager) {

        /*
         * Register a NavigationManagerEventListener to monitor the status change on
         * NavigationManager
         */
        m_navigationManager.addNavigationManagerEventListener(
                new WeakReference<NavigationManager.NavigationManagerEventListener>(
                        m_navigationManagerEventListener));

        /* Register a PositionListener to monitor the position updates */
        m_navigationManager.addPositionListener(
                new WeakReference<NavigationManager.PositionListener>(m_positionListener));

        m_navigationManager.addNewInstructionEventListener(
                new WeakReference<NavigationManager.NewInstructionEventListener>(instructListener));

        m_navigationManager.addLaneInformationListener(new WeakReference<NavigationManager.LaneInformationListener>(laneInfo));


    }
    private NavigationManager.NewInstructionEventListener instructListener
            = new NavigationManager.NewInstructionEventListener() {

        @Override
        public void onNewInstructionEvent() {
            // Interpret and present the Maneuver object as it contains
            // turn by turn navigation instructions for the user.
            manNavi.getNextManeuver();
        }
    };
    private NavigationManager.PositionListener m_positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {

            geoPosition.getCoordinate();
            geoPosition.getHeading();
            manNavi.getTta(Route.TrafficPenaltyMode.DISABLED, true);
            manNavi.getDestinationDistance();




        }
    };

    private NavigationManager.NavigationManagerEventListener m_navigationManagerEventListener = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onRunningStateChanged() {
            Toast.makeText(context, "Running state changed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNavigationModeChanged() {
            Toast.makeText(context, "Navigation mode changed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnded(NavigationManager.NavigationMode navigationMode) {
            Toast.makeText(context, navigationMode + " was ended", Toast.LENGTH_SHORT).show();
            stopForegroundService();
        }

        @Override
        public void onMapUpdateModeChanged(NavigationManager.MapUpdateMode mapUpdateMode) {
            Toast.makeText(context, "Map update mode is changed to " + mapUpdateMode,
                    Toast.LENGTH_SHORT).show();
        }



    };


    private NavigationManager.LaneInformationListener laneInfo = new NavigationManager.LaneInformationListener() {
        @Override
        public void onLaneInformation(List<LaneInformation> list, RoadElement roadElement) {
            DecimalFormat df = new DecimalFormat("###.##");
            String speedLimit = (int) roadElement.getSpeedLimit()+ " km/h";
            TextView tv = (TextView) findViewById(R.id.speedLimit);
            tv.setText(speedLimit);
            Log.d("DEBUGA", list.toString());


        }
    };





}

