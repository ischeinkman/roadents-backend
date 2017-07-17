package org.tymit.projectdonut.logic.logiccores;

import com.google.common.collect.Lists;
import org.tymit.projectdonut.model.location.LocationPoint;
import org.tymit.projectdonut.model.location.StartPoint;
import org.tymit.projectdonut.model.routing.TravelRoute;
import org.tymit.projectdonut.model.routing.TravelRouteNode;
import org.tymit.projectdonut.model.time.TimeDelta;
import org.tymit.projectdonut.model.time.TimePoint;
import org.tymit.projectdonut.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ilan on 7/16/17.
 */
public class DonutRangeLogicCore implements LogicCore {

    public static final String START_TIME_TAG = "starttime";
    public static final String LAT_TAG = "latitude";
    public static final String LONG_TAG = "longitude";
    public static final String TYPE_TAG = "type";
    public static final String TIME_DELTA_TAG = "timedelta";
    private static final String TAG = "DONUTRANGE";
    private static final String AREA_MAP = "area_map";

    @Override
    public Map<String, List<Object>> performLogic(Map<String, Object> args) {

        //Get the args
        long startUnixTime = (long) args.get(START_TIME_TAG);
        TimePoint startTime = new TimePoint(startUnixTime, "America/Los_Angeles");
        double startLat = (double) args.get(LAT_TAG);
        double startLong = (double) args.get(LONG_TAG);
        long maxUnixTimeDelta = (long) args.get(TIME_DELTA_TAG);
        TimeDelta maxTimeDelta = new TimeDelta(maxUnixTimeDelta);

        //Run the core
        Map<LocationPoint, TimeDelta> ranges = runRangeFinder(
                new StartPoint(new double[] { startLat, startLong }),
                startTime,
                maxTimeDelta
        );

        //Build the output
        Map<String, List<Object>> output = new HashMap<>();
        if (LoggingUtils.hasErrors()) {
            List<Object> errs = new ArrayList<>(LoggingUtils.getErrors());
            output.put("ERRORS", errs);
        }
        output.put(AREA_MAP, Lists.newArrayList(ranges));
        return output;
    }

    private static Map<LocationPoint, TimeDelta> runRangeFinder(StartPoint start, TimePoint startTime, TimeDelta maxDelta) {
        Set<TravelRoute> stationRoutes = DonutLogicSupport.buildStationRouteList(start, startTime, maxDelta);

        Map<LocationPoint, TimeDelta> rval = new HashMap<>();
        for (TravelRoute route : stationRoutes) {
            for (TravelRouteNode node : route.getRoute()) {
                rval.put(node.getPt(), maxDelta.minus(route.getTotalTimeAtNode(node)));
            }
        }

        return rval;
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
