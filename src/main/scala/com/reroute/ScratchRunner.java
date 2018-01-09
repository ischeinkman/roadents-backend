package com.reroute;

import com.reroute.backend.model.location.InputLocation;
import com.reroute.backend.stations.gtfs.GtfsPostgresLoader;
import com.reroute.backend.stations.transitland.TransitlandApi;
import com.reroute.displayers.restcontroller.SparkHandler;
import scala.util.Try;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ilan on 1/5/17.
 */
public class ScratchRunner {


    public static void main(String[] args) {

        try {
            for (String arg : args) {
                if ("--spark".equals(arg)) {
                    runSpark(args);
                    return;
                }
                if ("--urls".equals(arg)) {
                    listUrls(args);
                    return;
                }
                if ("--loadNew".equals(arg)) {
                    loadZipsNew(args);
                    return;
                }
                if ("--loadArea".equals(arg)) {
                    loadInArea(args);
                    return;
                }
            }
            System.out.println("ARGUMENT NOT FOUND, EXITING...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSpark(String[] args) {
        SparkHandler.main(args);
    }

    private static void listUrls(String[] args) {
        Double range = 1.0;
        Double lat = null;
        Double lng = null;


        for (int i = 0; i < args.length; i++) {
            if ("-lat".equals(args[i]) && args.length > i + 1) {
                lat = Double.parseDouble(args[i + 1]);
            } else if ("-lng".equals(args[i]) && args.length > i + 1) {
                lng = Double.parseDouble(args[i + 1]);
            } else if ("-d".equals(args[i]) && args.length > i + 1) {
                range = Double.parseDouble(args[i + 1]);
            }
        }

        if (lat == null || lng == null) {
            System.out.println("Coords not passed correctly.");
            return;
        }

        InputLocation center = new InputLocation(lat, lng);
        TransitlandApi apidb = new TransitlandApi();
        Map<String, String> skipBad = new HashMap<>();
        skipBad.put("license_use_without_attribution", "no");
        apidb.getFeedsInArea(center, range, null, skipBad).forEach(System.out::println);
    }

    private static void loadZipsNew(String[] args) {
        String zipurl = null;
        String db = null;
        for (int i = 0; i < args.length; i++) {
            if ("--zip".equals(args[i])) {
                zipurl = args[i + 1];
            } else if ("--db".equals(args[i])) {
                db = args[i + 1];
            }
        }

        if (zipurl == null || db == null) {
            System.out.printf("Got null args:\ndb = %s\nzip = %s\n", db, zipurl);
            return;
        }

        URL url;
        try {
            url = new URL(zipurl);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("BAD URL: " + zipurl);
            return;
        }
        GtfsPostgresLoader loader = new GtfsPostgresLoader(url);
        Try<Object> res = loader.load(db, "donut", "donutpass");
        if (res.isFailure()) {
            System.out.printf("Got args:\ndb = %s\nzip = %s\n", db, zipurl);
            res.toEither().left().get().printStackTrace();
        }
        System.out.println("Build complete.");
    }

    private static void loadInArea(String[] args) {
        Double range = -1.0;
        Double lat = null;
        Double lng = null;
        String dbt = null;


        for (int i = 0; i < args.length; i++) {
            if ("--lat".equals(args[i]) && args.length > i + 1) {
                lat = Double.parseDouble(args[i + 1]);
            } else if ("--lng".equals(args[i]) && args.length > i + 1) {
                lng = Double.parseDouble(args[i + 1]);
            } else if ("--dist".equals(args[i]) && args.length > i + 1) {
                range = Double.parseDouble(args[i + 1]);
            } else if ("--db".equals(args[i])) {
                dbt = args[i + 1];
            }
        }

        if (lat == null || lng == null || dbt == null) {
            System.out.println("Args not passed correctly.");
            return;
        }

        InputLocation center = new InputLocation(lat, lng);
        TransitlandApi apidb = new TransitlandApi();
        Map<String, String> skipBad = new HashMap<>();
        final String db = dbt;
        List<URL> urls = apidb.getFeedsInArea(center, range, null, skipBad);
        urls.parallelStream().forEach(url -> System.out.printf("Got url: %s", url.toString()));
        urls.parallelStream()
                .peek(url -> System.out.printf("Loading url: %s", url.toString()))
                .forEach(zipurl -> {
                    GtfsPostgresLoader loader = new GtfsPostgresLoader(zipurl);
                    Try<Object> res = loader.load(db, "donut", "donutpass");
                    if (res.isFailure()) {
                        System.out.printf("Got args:\ndb = %s\nzip = %s\n", db, zipurl);
                        res.toEither().left().get().printStackTrace();
                    }
                    System.out.println("Build complete.");
                });
    }
}

