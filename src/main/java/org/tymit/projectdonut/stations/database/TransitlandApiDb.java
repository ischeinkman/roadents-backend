package org.tymit.projectdonut.stations.database;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tymit.projectdonut.model.TimeModel;
import org.tymit.projectdonut.model.TransChain;
import org.tymit.projectdonut.model.TransStation;
import org.tymit.projectdonut.utils.LoggingUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ilan on 1/22/17.
 */
public class TransitlandApiDb implements StationDbInstance {

    private static final int MAX_QUERY_SIZE = 100;

    private static final String BASE_SCHEDULE_URL = "http://transit.land/api/v1/schedule_stop_pairs";
    private static final String AREA_FORMAT = "bbox=%f,%f,%f,%f";
    private static final String ROUTE_FORMAT = "route_onestop_id=%s";
    private static final String TRIP_FORMAT = "trip=%s";
    private static final String STOP_ID_URL = "http://transit.land/api/v1/stops?per_page=" + MAX_QUERY_SIZE + "&onestop_id=%s";

    private static final double MILES_TO_LAT = 1.0 / 69.5;
    private static final double MILES_TO_LONG = 1 / 69.5;

    private OkHttpClient client;
    private boolean isUp;


    public TransitlandApiDb() {
        client = new OkHttpClient();
        isUp = true;
    }

    @Override
    public List<TransStation> queryStations(double[] center, double range, TransChain chain) {

        //Get scheduling info
        Request request = new Request.Builder()
                .url(buildScheduleUrl(center, range, chain))
                .build();

        Response response;
        JSONObject rawobj;
        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                isUp = false;
                return Collections.emptyList();
            }
            rawobj = new JSONObject(response.body().string());
        } catch (IOException e) {
            LoggingUtils.logError(e);
            isUp = false;
            return Collections.emptyList();
        }

        JSONArray schedStopArr = rawobj.getJSONArray("schedule_stop_pairs");
        int size = schedStopArr.length();

        //Get stop information
        AtomicInteger numOfStops = new AtomicInteger(0);
        StringJoiner ids = IntStream.range(0, size)
                .parallel().boxed()
                .map(schedStopArr::getJSONObject)
                .map(obj -> obj.getString("origin_onestop_id"))
                .distinct()
                .peek(obj -> numOfStops.getAndIncrement())
                .collect(() -> new StringJoiner(","), StringJoiner::add, StringJoiner::merge);
        String stopQuery = String.format(STOP_ID_URL, ids.toString());

        Request stopRequest = new Request.Builder().url(stopQuery).build();
        Response stopResponse;
        JSONObject rawStopObj;

        try {
            stopResponse = client.newCall(stopRequest).execute();
            if (!stopResponse.isSuccessful()) {
                isUp = false;
                return Collections.emptyList();
            }
            rawStopObj = new JSONObject(stopResponse.body().string());
        } catch (IOException e) {
            LoggingUtils.logError(e);
            isUp = false;
            return Collections.emptyList();
        }

        Map<String, String[]> idToStopInfo = new ConcurrentHashMap<>(size);
        JSONArray stopInfoArray = rawStopObj.getJSONArray("stops");
        IntStream.range(0, numOfStops.get())
                .boxed().parallel()
                .map(stopInfoArray::getJSONObject)
                .forEach(obj -> {
                    String id = obj.getString("onestop_id");
                    String name = obj.getString("name");
                    JSONArray coords = obj.getJSONObject("geometry")
                            .getJSONArray("coordinates");
                    idToStopInfo.put(id, new String[] { name, "" + coords.getDouble(1), "" + coords
                            .getDouble(0) });
                });

        //Match & return
        Map<String, TransChain> chainMap = new ConcurrentHashMap<>();
        if (chain != null) chainMap.put(chain.getName(), chain);
        List<TransStation> rval = IntStream.range(0, size)
                .boxed().parallel()
                .map(schedStopArr::getJSONObject)
                .map(obj -> mapToStation(obj, chainMap, idToStopInfo))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        ;
        return rval;
    }

    private static String buildScheduleUrl(double[] center, double range, TransChain chain) {
        StringBuilder builder = new StringBuilder("?per_page=").append(MAX_QUERY_SIZE)
                .append("&");
        if (center != null && range >= 0) {

            //Construct the bounding box
            double latVal1 = center[0] - range * MILES_TO_LAT;
            double latVal2 = center[0] + range * MILES_TO_LAT;

            double lngVal1 = center[1] - range * MILES_TO_LONG;
            double lngVal2 = center[1] + range * MILES_TO_LONG;

            builder.append(String.format(AREA_FORMAT, lngVal1, latVal1, lngVal2, latVal2));
            builder.append("&");
        }
        if (chain != null && !chain.getName().equals("")) {
            String[] routeTrip = chain.getName().split(" TripID: ");
            if (routeTrip.length <= 1)
                builder.append(String.format(TRIP_FORMAT, chain.getName()));
            else builder.append(String.format(ROUTE_FORMAT, routeTrip[0]))
                    .append("&")
                    .append(String.format(TRIP_FORMAT, routeTrip[1]));
        }
        return BASE_SCHEDULE_URL + builder.toString();
    }

    private TransStation mapToStation(JSONObject obj, Map<String, TransChain> chains, Map<String, String[]> stopInfo) {
        String stationId = obj.getString("origin_onestop_id");
        String[] stationInfo = stopInfo.get(stationId);
        String name = stationInfo[0];
        double[] coords = new double[] { Double.valueOf(stationInfo[1]), Double.valueOf(stationInfo[2]) };

        String chainName = obj.getString("route_onestop_id") + " TripID: " + obj
                .getString("trip");
        chains.putIfAbsent(chainName, new TransChain(chainName));
        TransChain chain = chains.get(chainName);

        String[] timeSchedule = obj.getString("origin_departure_time")
                .split(":");
        TimeModel departTime = TimeModel.empty()
                .set(TimeModel.HOUR, Integer.valueOf(timeSchedule[0]))
                .set(TimeModel.MINUTE, Integer.valueOf(timeSchedule[1]))
                .set(TimeModel.SECOND, (timeSchedule.length > 2) ? Integer.valueOf(timeSchedule[2]) : 0);

        return new TransStation(name, coords, Collections.singletonList(departTime), chain);
    }


    @Override
    public boolean putStations(List<TransStation> stations) {
        return true;
    }

    @Override
    public boolean isUp() {
        return isUp;
    }

    @Override
    public void close() {
    }

}
