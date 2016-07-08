package org.tymit.projectdonut.stations.database;

import org.tymit.projectdonut.model.TransChain;
import org.tymit.projectdonut.model.TransStation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ilan on 7/7/16.
 */
public class StationDbHelper {

    private static final StationDbInstance[] allDatabases = initializeDbList();
    private static final StationDbHelper instance = new StationDbHelper();

    public static StationDbHelper getHelper() {
        return instance;
    }

    private static StationDbInstance[] initializeDbList() {
        return new StationDbInstance[0];
    }

    public List<TransStation> queryStations(double[] center, double range, TransChain chain) {
        Set<TransStation> allStations = new HashSet<>();
        for (StationDbInstance dbInstance : allDatabases) {
            allStations.addAll(dbInstance.queryStations(center, range, chain));
        }
        return new ArrayList<>(allStations);
    }
}
