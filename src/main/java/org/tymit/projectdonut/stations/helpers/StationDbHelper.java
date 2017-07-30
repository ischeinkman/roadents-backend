package org.tymit.projectdonut.stations.helpers;

import org.tymit.projectdonut.model.location.LocationPoint;
import org.tymit.projectdonut.model.location.TransChain;
import org.tymit.projectdonut.model.location.TransStation;
import org.tymit.projectdonut.model.time.SchedulePoint;
import org.tymit.projectdonut.model.time.TimeDelta;
import org.tymit.projectdonut.model.time.TimePoint;
import org.tymit.projectdonut.stations.interfaces.StationDbInstance;
import org.tymit.projectdonut.stations.postgresql.PostgresqlStationDbCache;
import org.tymit.projectdonut.stations.test.TestStationDb;
import org.tymit.projectdonut.utils.StreamUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by ilan on 7/7/16.
 */
public class StationDbHelper {

    private static StationDbHelper instance;
    private static boolean isTest = false;
    private StationDbInstance[] allDatabases = null;
    private Map<String, List<StationDbInstance.DonutDb>> nameToDbs = new HashMap<>();

    private StationDbHelper() {
        initializeDbList();
    }

    private void initializeDbList() {

        if (allDatabases != null) {
            Arrays.stream(allDatabases).forEach(StationDbInstance::close);
        }

        if (isTest) {
            allDatabases = new StationDbInstance[]{new TestStationDb()};
            return;
        }
        allDatabases = new StationDbInstance[] {
                new PostgresqlStationDbCache(PostgresqlStationDbCache.DB_URLS[0])
        };
    }

    public static StationDbHelper getHelper() {
        if (instance == null) instance = new StationDbHelper();
        return instance;
    }

    public static void setTestMode(boolean testMode) {
        if (isTest == testMode) return;
        isTest = testMode;
        instance = null;
        TestStationDb.setTestStations(null);
    }

    public List<TransStation> queryStations(double[] center, double range, TimePoint startTime, TimeDelta maxDelta, TransChain chain) {
        return Arrays.stream(allDatabases)
                .filter(StationDbInstance::isUp)
                .filter(db -> db instanceof StationDbInstance.ComboDb)
                .flatMap(db -> ((StationDbInstance.ComboDb) db).queryStations(center, range, startTime, maxDelta, chain)
                        .stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<TransStation> queryStrippedStations(double[] center, double range, int limit) {
        if (center == null || range <= 0 || limit == 0) return Collections.emptyList();
        return Arrays.stream(allDatabases)
                .filter(StationDbInstance::isUp)
                .filter(db -> db instanceof StationDbInstance.ComboDb)
                .findAny()
                .map(db -> ((StationDbInstance.ComboDb) db).queryStrippedStations(center, range, limit))
                .orElse(Collections.emptyList());
    }

    public boolean putStations(List<TransStation> stations) {
        //We create a boolean set and then check if any are true
        //to guarantee that all instances are attempted.
        return Arrays.stream(allDatabases).parallel()
                .map(db -> db.putStations(stations))
                .collect(Collectors.toSet())
                .contains(true);
    }

    public void closeAllDatabases() {
        Arrays.stream(allDatabases).forEach(StationDbInstance::close);
    }

    public List<TransStation> getStationsInArea(LocationPoint center, double range) {
        Supplier<List<TransStation>> orElseSupplier = () ->
                doComboQuery(db -> db.queryStrippedStations(center.getCoordinates(), range, 10000), Collections::emptyList);

        return doDonutQuery(db -> getStationsInArea(center, range), orElseSupplier);
    }

    public Map<LocationPoint, List<TransStation>> getStationsInArea(Map<LocationPoint, Double> ranges) {
        Supplier<Map<LocationPoint, List<TransStation>>> orElseSupplier = () -> ranges.entrySet().stream()
                .collect(StreamUtils.collectWithMapping(
                        Map.Entry::getKey,
                        entry -> doComboQuery(
                                db -> db.queryStrippedStations(entry.getKey()
                                        .getCoordinates(), entry.getValue(), 10000),
                                Collections::emptyList
                        )
                ));

        return doDonutQuery(db -> db.getStationsInArea(ranges), orElseSupplier);

    }

    private <T> T doDonutQuery(Function<StationDbInstance.DonutDb, T> toGet, Supplier<T> orElse) {
        return nameToDbs.values().stream()
                .flatMap(Collection::stream)
                .filter(StationDbInstance::isUp)
                .map(toGet)
                .findAny()
                .orElseGet(orElse);
    }

    private <T> T doComboQuery(Function<StationDbInstance.ComboDb, T> toGet, Supplier<T> orElse) {
        return Arrays.stream(allDatabases)
                .filter(db -> db instanceof StationDbInstance.ComboDb)
                .filter(StationDbInstance::isUp)
                .map(db -> (StationDbInstance.ComboDb) db)
                .map(toGet)
                .findAny()
                .orElseGet(orElse);
    }

    public Map<TransChain, List<SchedulePoint>> getChainsForStation(TransStation station) {
        return doDonutQuery(db -> db.getChainsForStation(station), Collections::emptyMap);
    }

    public Map<TransStation, Map<TransChain, List<SchedulePoint>>> getChainsForStations(List<TransStation> stations) {
        return doDonutQuery(db -> db.getChainsForStations(stations), Collections::emptyMap);
    }

    public Map<TransStation, TimeDelta> getArrivableStations(TransChain chain, TimePoint startTime, TimeDelta maxDelta) {
        return doDonutQuery(db -> getArrivableStations(chain, startTime, maxDelta), Collections::emptyMap);
    }

    public Map<TransChain, Map<TransStation, TimeDelta>> getArrivableStations(List<TransChain> chains, TimePoint startTime, TimeDelta maxDelta) {
        return doDonutQuery(db -> db.getArrivableStations(chains, startTime, maxDelta), Collections::emptyMap);
    }

    public Map<TransChain, Map<TransStation, TimeDelta>> getArrivableStations(Map<TransChain, TimeDelta> chainsAndExtras, TimePoint generalStart, TimeDelta maxDelta) {
        return doDonutQuery(db -> db.getArrivableStations(chainsAndExtras, generalStart, maxDelta), Collections::emptyMap);
    }
}
