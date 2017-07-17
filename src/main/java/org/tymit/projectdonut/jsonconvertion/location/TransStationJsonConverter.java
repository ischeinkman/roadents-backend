package org.tymit.projectdonut.jsonconvertion.location;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tymit.projectdonut.jsonconvertion.JsonConverter;
import org.tymit.projectdonut.jsonconvertion.time.SchedulePointJsonConverter;
import org.tymit.projectdonut.model.location.TransChain;
import org.tymit.projectdonut.model.location.TransStation;
import org.tymit.projectdonut.model.time.SchedulePoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ilan on 6/3/17.
 */
public class TransStationJsonConverter implements JsonConverter<TransStation> {

    private static final String NAME_TAG = "name";
    private static final String CHAIN_TAG = "chain";
    private static final String LAT_TAG = "latitude";
    private static final String LNG_TAG = "longitude";
    private static final String SCHEDULE_TAG = "schedule";

    @Override
    public String toJson(TransStation input) {
        JSONObject obj = new JSONObject();
        obj.put(NAME_TAG, input.getName());
        obj.put(CHAIN_TAG, input.getChain().getName());
        obj.put(LAT_TAG, input.getCoordinates()[0]);
        obj.put(LNG_TAG, input.getCoordinates()[1]);

        JSONArray arr = new JSONArray(new SchedulePointJsonConverter().toJson(input.getSchedule()));
        obj.put(SCHEDULE_TAG, arr);

        return obj.toString();
    }

    @Override
    public void fromJson(String json, Collection<TransStation> output) {
        Map<String, TransChain> chainMap = new ConcurrentHashMap<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject objJson = array.getJSONObject(i);
            String objString = objJson.toString();
            TransStation obj = fromJson(objString, chainMap);
            output.add(obj);
        }
    }

    @Override
    public TransStation fromJson(String json) {
        return fromJson(json, new ConcurrentHashMap<>());
    }

    public TransStation fromJson(String json, Map<String, TransChain> chains) {
        JSONObject obj = new JSONObject(json);
        String name = obj.getString(NAME_TAG);
        double[] coords = new double[] { obj.getDouble(LAT_TAG), obj.getDouble(LNG_TAG) };

        String chainName = obj.getString(CHAIN_TAG);
        chains.putIfAbsent(chainName, new TransChain(chainName));
        TransChain chain = chains.get(chainName);

        List<SchedulePoint> schedule = new ArrayList<>();
        new SchedulePointJsonConverter().fromJson(obj.getJSONArray(SCHEDULE_TAG).toString(), schedule);

        return new TransStation(name, coords, schedule, chain);
    }
}
