package org.tymit.projectdonut.locations;

import org.tymit.projectdonut.costs.CostArgs;
import org.tymit.projectdonut.costs.CostCalculator;
import org.tymit.projectdonut.locations.caches.LocationCacheHelper;
import org.tymit.projectdonut.locations.providers.LocationProviderHelper;
import org.tymit.projectdonut.model.LocationPoint;
import org.tymit.projectdonut.model.LocationType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ilan on 7/7/16.
 */
public class LocationRetriever {
    public static List<LocationPoint> getLocations(double[] center, double range, LocationType type, List<CostArgs> args) {
        List<LocationPoint> locations = LocationCacheHelper.getHelper().getCachedLocations(center, range, type);
        if (locations == null) locations = LocationProviderHelper.getHelper().getLocations(center, range, type);
        if (locations == null || locations.size() == 0) return new ArrayList<>(0);

        if (args == null || args.size() == 0) return locations;

        Iterator<LocationPoint> locationsIterator = locations.iterator();
        while (locationsIterator.hasNext()) {
            for (CostArgs arg : args) {
                arg.setSubject(locationsIterator.next());
                if (!CostCalculator.isWithinCosts(arg)) locationsIterator.remove();
            }
        }
        return locations;
    }
}
