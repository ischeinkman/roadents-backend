package com.reroute.backend.costs.helpers;

import com.google.common.collect.Sets;
import com.reroute.backend.costs.arguments.BulkCostArgs;
import com.reroute.backend.costs.arguments.CostArgs;
import com.reroute.backend.costs.interfaces.BulkCostProvider;
import com.reroute.backend.costs.interfaces.CostProvider;
import com.reroute.backend.costs.providers.routing.PathmakerBestRouteCostProvider;
import com.reroute.backend.costs.providers.routing.PathmakerMultiRouteCostProvider;
import com.reroute.backend.costs.providers.timing.GoogleTimeCostProvider;
import com.reroute.backend.costs.providers.timing.MapzenTimeCostProvider;
import com.reroute.backend.utils.LoggingUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ilan on 7/7/16.
 */
public class CostProviderHelper {

    private static final CostProviderHelper instance = new CostProviderHelper();

    private final Map<String, Set<CostProvider>> tagToProvider = new ConcurrentHashMap<>();
    private final Map<String, Set<BulkCostProvider>> tagToBulk = new ConcurrentHashMap<>();

    private CostProviderHelper() {
        initializeProviderMap();
    }

    private void initializeProviderMap() {
        initializeProvidersList().forEach(provider -> {
            if (provider instanceof BulkCostProvider) {
                tagToBulk.putIfAbsent(provider.getTag(), Sets.newConcurrentHashSet());
                tagToBulk.get(provider.getTag()).add((BulkCostProvider) provider);
            }
            tagToProvider.putIfAbsent(provider.getTag(), Sets.newConcurrentHashSet());
            tagToProvider.get(provider.getTag()).add(provider);
        });
    }

    private static Collection<? extends CostProvider> initializeProvidersList() {
        Set<CostProvider> rval = Sets.newConcurrentHashSet();
        rval.add(new PathmakerBestRouteCostProvider());
        rval.add(new PathmakerMultiRouteCostProvider());

        Arrays.stream(GoogleTimeCostProvider.API_KEYS)
                .map(GoogleTimeCostProvider::new)
                .forEach(rval::add);

        Arrays.stream(MapzenTimeCostProvider.API_KEYS)
                .map(MapzenTimeCostProvider::new)
                .forEach(rval::add);

        return rval;
    }

    public static CostProviderHelper getHelper() {
        return instance;
    }

    public Map<Object, Object> getCostValue(BulkCostArgs args) {
        if (args == null || args.getCostTag() == null || tagToProvider.get(args.getCostTag()) == null)
            return new ConcurrentHashMap<>();

        Optional<BulkCostProvider> foundProvider = tagToBulk.get(args.getCostTag()).stream()
                .filter(CostProvider::isUp)
                .findAny();
        if (foundProvider.isPresent()) return foundProvider.get().getCostValue(args);

        Map<Object, CostArgs> singleArgs = args.splitSingular();
        Map<Object, Object> rval = new ConcurrentHashMap<>();
        for (Object subj : singleArgs.keySet()) {
            CostArgs singArgs = singleArgs.get(subj);
            Object result = getCostValue(singArgs);
            if (result != null) rval.put(subj, result);  //Defaults to null anyway
        }
        return rval;
    }

    public Object getCostValue(CostArgs args) {
        if (args == null || args.getCostTag() == null || tagToProvider.get(args.getCostTag()) == null) return null;

        Optional<CostProvider> foundProvider = tagToProvider.get(args.getCostTag()).stream()
                .filter(CostProvider::isUp)
                .findAny();
        if (!foundProvider.isPresent()) {
            LoggingUtils.logError(getClass().getName(), "Could not find cost with tag: %s", args.getCostTag());
            return null;
        }

        return foundProvider.get().getCostValue(args);
    }

    public Map<Object, Boolean> isWithinCosts(BulkCostArgs args) {
        if (args == null || args.getCostTag() == null || tagToProvider.get(args.getCostTag()) == null)
            return new ConcurrentHashMap<>();

        Optional<BulkCostProvider> foundProvider = tagToBulk.get(args.getCostTag()).stream()
                .filter(CostProvider::isUp)
                .findAny();
        if (foundProvider.isPresent()) return foundProvider.get().isWithinCosts(args);

        Map<Object, CostArgs> singleArgs = args.splitSingular();
        Map<Object, Boolean> rval = new ConcurrentHashMap<>();
        for (Object subj : singleArgs.keySet()) {
            CostArgs singArgs = singleArgs.get(subj);
            boolean result = isWithinCosts(singArgs);
            rval.put(subj, result);
        }
        return rval;
    }

    public boolean isWithinCosts(CostArgs args) {
        //Default to true to not filter anything on error
        if (args == null || args.getCostTag() == null || tagToProvider.get(args.getCostTag()) == null) return true;

        Optional<CostProvider> foundProvider = tagToProvider.get(args.getCostTag()).stream()
                .filter(CostProvider::isUp)
                .findAny();
        if (!foundProvider.isPresent()) {
            LoggingUtils.logError(getClass().getName(), "Could not find cost with tag: %s", args.getCostTag());
            return true;
        }

        return foundProvider.get().isWithinCosts(args);

    }
}