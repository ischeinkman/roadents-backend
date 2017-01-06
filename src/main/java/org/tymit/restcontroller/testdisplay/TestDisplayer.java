package org.tymit.restcontroller.testdisplay;

import org.tymit.projectdonut.model.TimeModel;
import org.tymit.projectdonut.model.TravelRoute;
import org.tymit.projectdonut.utils.LocationUtils;

import java.util.List;

/**
 * Created by ilan on 1/3/17.
 */
public class TestDisplayer {

    public static String buildDisplay(List<TravelRoute> routes) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>\n");
        builder.append(TestDisplayConstants.HEAD);
        builder.append("\n<body>\n");

        routes.stream()
                .map(TestDisplayer::routeToList)
                .forEach(builder::append);

        builder.append("<script>\n").append(TestDisplayConstants.JAVASCRIPT).append("</script>\n");
        return builder.toString();
    }

    public static String routeToList(TravelRoute route) {
        long totalMillis = route.getTotalTime();
        long hours = totalMillis / 3600000L;
        long mins = totalMillis / 60000L % 60;
        StringBuilder builder = new StringBuilder();

        builder.append(String.format(TestDisplayConstants.ROUTE_TITLE_FORMAT, route.getDestination()
                        .getName(), hours, mins,
                LocationUtils.distanceBetween(route.getStart().getCoordinates(), route.getDestination()
                        .getCoordinates(), true), route.getRoute().size() - 2)
        );

        route.getRoute().stream()
                .map(node -> String.format(TestDisplayConstants.ROUTE_ELEMENT_FORMAT,
                        node.getWalkTimeFromPrev(), node.getWaitTimeFromPrev(), node.getTravelTimeFromPrev(),
                        node.getPt().getName(), route.getTimeAtNode(node).get(TimeModel.HOUR), route.getTimeAtNode(node)
                                .get(TimeModel.MINUTE))
                )
                .forEach(builder::append);

        builder.append(TestDisplayConstants.ROUTE_FOOTER);

        return builder.toString();
    }
}
