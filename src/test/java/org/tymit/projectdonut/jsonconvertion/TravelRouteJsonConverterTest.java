package org.tymit.projectdonut.jsonconvertion;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tymit.projectdonut.jsonconvertion.routing.TravelRouteJsonConverter;
import org.tymit.projectdonut.locations.LocationRetriever;
import org.tymit.projectdonut.model.location.DestinationLocation;
import org.tymit.projectdonut.model.location.LocationType;
import org.tymit.projectdonut.model.location.StartPoint;
import org.tymit.projectdonut.model.location.TransStation;
import org.tymit.projectdonut.model.routing.TravelRoute;
import org.tymit.projectdonut.model.routing.TravelRouteNode;
import org.tymit.projectdonut.model.time.TimePoint;
import org.tymit.projectdonut.stations.StationRetriever;
import org.tymit.projectdonut.stations.updates.StationDbUpdater;
import org.tymit.projectdonut.utils.LoggingUtils;

import java.util.List;
import java.util.Random;

/**
 * Created by ilan on 7/17/16.
 */
public class TravelRouteJsonConverterTest {

    private TravelRoute testRoute;

    @Before
    public void setUpTestRoute() {

        LocationRetriever.setTestMode(true);
        StationRetriever.setTestMode(true);
        StationDbUpdater.getUpdater().updateStationsSync(); //Force a test db population
        LoggingUtils.setPrintImmediate(true);

        final double latitude = 37.358658;
        final double longitude = -122.008763;
        StartPoint startPoint = new StartPoint(new double[]{latitude, longitude});
        TimePoint startTime = new TimePoint(System.currentTimeMillis(), "America/Los_Angeles");
        testRoute = new TravelRoute(startPoint, startTime);
        Random rng = new Random();

        final double range = 50;
        for (int i = 0; i < 5; i++) {
            TransStation currentStation = new TransStation("t" + i, new double[] { latitude + (i + 1) * 10 * rng
                    .nextDouble(), longitude + (i + 1) * 10 * rng.nextDouble() });
            TravelRouteNode stationNode = new TravelRouteNode.Builder().setPoint(currentStation).setTravelTime(1).setWaitTime(1).build();
            if (testRoute.isInRoute(stationNode.getPt())) {
                testRoute.addNode(stationNode);
            } else continue;
            List<TransStation> stationsInChain = StationRetriever.getStations(null, 0, currentStation.getChain(), null);
            TransStation chainStation = stationsInChain.get(i % stationsInChain.size());
            TravelRouteNode chainStationNode = new TravelRouteNode.Builder().setPoint(chainStation).setTravelTime(1).setWaitTime(1).build();
            testRoute.addNode(chainStationNode);
        }

        LocationType testType = new LocationType("food", "food");
        List<DestinationLocation> locations = LocationRetriever.getLocations(testRoute.getCurrentEnd().getCoordinates(), range, testType, null);
        testRoute.setDestinationNode(new TravelRouteNode.Builder().setWalkTime(1).setPoint(locations.get(0)).build());
    }

    @Test
    public void testBackAndForth() {
        String routeJson = new TravelRouteJsonConverter()
                .toJson(testRoute);
        TravelRoute fromJson = new TravelRouteJsonConverter()
                .fromJson(routeJson);
        Assert.assertEquals(testRoute, fromJson);
    }

    @After
    public void cleanUp() {
        LocationRetriever.setTestMode(false);
        StationRetriever.setTestMode(false);
        LoggingUtils.setPrintImmediate(false);
    }

}