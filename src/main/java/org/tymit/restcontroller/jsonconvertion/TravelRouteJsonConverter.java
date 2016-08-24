package org.tymit.restcontroller.jsonconvertion;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tymit.projectdonut.model.DestinationLocation;
import org.tymit.projectdonut.model.StartPoint;
import org.tymit.projectdonut.model.TimeModel;
import org.tymit.projectdonut.model.TransChain;
import org.tymit.projectdonut.model.TravelRoute;
import org.tymit.projectdonut.model.TravelRouteNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ilan on 7/16/16.
 */
public class TravelRouteJsonConverter implements JsonConverter<TravelRoute> {

    private static final String START_TAG = "start";
    private static final String START_TIME_TAG = "starttime";
    private static final String END_TAG = "dest";
    private static final String ROUTE_TAG = "route";
    private static final String STATION_LAT_TAG = "latitude";
    private static final String STATION_LONG_TAG = "longitude";
    private static final String STATION_NAME_TAG = "stationName";
    private static final String STATION_CHAIN_TAG = "trainBusName";

    private StartPointJsonConverter startConverter = new StartPointJsonConverter();
    private DestinationJsonConverter destConverter = new DestinationJsonConverter();

    @Override
    public String toJson(TravelRoute input) {
        JSONObject obj = new JSONObject();
        obj.put(START_TIME_TAG, input.getStartTime().getUnixTime()); //Store seconds from unix epoch
        obj.put(START_TAG, new JSONObject(startConverter.toJson(input.getStart())));
        obj.put(ROUTE_TAG, convertRoute(input));
        if (input.getDestination() != null)
            obj.put(END_TAG, new JSONObject(destConverter.toJson(input.getDestination())));
        return obj.toString();
    }

    private JSONArray convertRoute(TravelRoute input) {

        TravelRouteNodeJsonConverter conv = new TravelRouteNodeJsonConverter();

        List<TravelRouteNode> route = input.getRoute();

        JSONArray routeJson = new JSONArray();

        for (TravelRouteNode node : route) {
            routeJson.put(new JSONObject(conv.toJson(node)));
        }

        return routeJson;
    }

    @Override
    public TravelRoute fromJson(String json) {

        JSONObject jsonObject = new JSONObject(json);

        TimeModel startTime = TimeModel.fromUnixTime(jsonObject.getLong(START_TIME_TAG));

        Map<String, TransChain> storedChains = new ConcurrentHashMap<>();

        List<TravelRouteNode> nodes = new ArrayList<>();
        TravelRouteNode startNode = null;
        TravelRouteNode endNode = null;

        TravelRouteNodeJsonConverter conv = new TravelRouteNodeJsonConverter(storedChains);

        JSONArray routeList = jsonObject.getJSONArray(ROUTE_TAG);
        for (int i = 0; i < routeList.length(); i++) {
            JSONObject nodeObj = routeList.getJSONObject(i);
            TravelRouteNode node = conv.fromJson(nodeObj.toString());
            if (node.getPt() instanceof StartPoint) startNode = node;
            else if (node.getPt() instanceof DestinationLocation) endNode = node;
            else nodes.add(node);
        }

        TravelRoute route = new TravelRoute((StartPoint) startNode.getPt(), startTime);
        nodes.forEach(route::addNode);
        route.setDestinationNode(endNode);

        return route;
    }
}
