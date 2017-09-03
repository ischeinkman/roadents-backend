package com.reroute.backend.costs;

import com.reroute.backend.costs.arguments.BulkCostArgs;
import com.reroute.backend.costs.arguments.CostArgs;
import com.reroute.backend.costs.helpers.CostProviderHelper;

import java.util.Map;

/**
 * Created by ilan on 7/7/16.
 */
public class CostCalculator {
    public static Object getCostValue(CostArgs args) {
        return CostProviderHelper.getHelper().getCostValue(args);
    }

    public static boolean isWithinCosts(CostArgs args) {
        return CostProviderHelper.getHelper().isWithinCosts(args);
    }

    public static Map<Object, Object> getCostValue(BulkCostArgs args) {
        return CostProviderHelper.getHelper().getCostValue(args);
    }

    public static Map<Object, Boolean> isWithinCosts(BulkCostArgs args) {
        return CostProviderHelper.getHelper().isWithinCosts(args);
    }
}