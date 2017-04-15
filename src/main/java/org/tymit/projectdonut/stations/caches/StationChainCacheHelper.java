package org.tymit.projectdonut.stations.caches;

import org.tymit.projectdonut.model.TimeDelta;
import org.tymit.projectdonut.model.TimePoint;
import org.tymit.projectdonut.model.TransChain;
import org.tymit.projectdonut.model.TransStation;
import org.tymit.projectdonut.utils.LocationUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ilan on 8/31/16.
 */
public class StationChainCacheHelper {

    private static final StationCacheInstance[] allStationInstances = initializeStationInstanceList();
    private static final StationChainCacheHelper instance = new StationChainCacheHelper();

    private StationChainCacheHelper() {

    }

    private static StationCacheInstance[] initializeStationInstanceList() {
        return Arrays.stream(PostgresqlExternalCache.DB_URLS)
                .map(PostgresqlExternalCache::new)
                .collect(Collectors.toList())
                .toArray(new StationCacheInstance[0]);
    }

    public static StationChainCacheHelper getHelper() {
        return instance;
    }

    public List<TransStation> getCachedStations(double[] center, double range, TimePoint startTime, TimeDelta maxDelta, TransChain chain) {
        return Arrays.stream(allStationInstances)
                .parallel()
                .map(cache -> cache.getCachedStations(center, range, startTime, maxDelta, chain))
                .filter(Objects::nonNull)
                .flatMap(Collection::parallelStream)
                .collect(Collectors.toList());
    }

    public boolean cacheStations(double[] center, double range, TimePoint startTime, TimeDelta maxDelta, Stream<List<TransStation>> stations) {
        AtomicInteger a = new AtomicInteger(0);
        return !stations
                .map(src -> cacheStations(center, range, startTime, maxDelta, src))
                .distinct()
                .anyMatch(b -> !b);
    }

    public boolean cacheStations(double[] center, double range, TimePoint startTime, TimeDelta maxDelta, List<TransStation> stations) {

        //Since physical range is easily calculable even without being given it,
        //we do so for possible efficiencies in the future.
        //However, the same is not true for temporal range.
        if (center == null || range < 0) {
            center = new double[] { 0, 0 };
            int size = 0;

            for (TransStation stat : stations) {
                center[0] += stat.getCoordinates()[0];
                center[1] += stat.getCoordinates()[1];
                size++;
            }

            center[0] = center[0] / size;
            center[1] = center[1] / size;

            for (TransStation stat : stations) {
                double curange = LocationUtils.distanceBetween(center, stat.getCoordinates(), true);
                if (curange > range) range = curange;
            }
        }

        //Java is BS sometimes
        double[] finalCenter = center;
        double finalRange = range;


        return Arrays.stream(allStationInstances)
                .parallel()
                .map(cache -> cache.cacheStations(finalCenter, finalRange, startTime, maxDelta, stations))
                .anyMatch(Boolean::booleanValue);

    }
}