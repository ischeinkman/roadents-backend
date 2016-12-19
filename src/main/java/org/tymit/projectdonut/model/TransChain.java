package org.tymit.projectdonut.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ilan on 7/7/16.
 */
public class TransChain {

    private final String name;
    private final List<TransStation> stations;

    public TransChain(String name) {
        this.name = name;
        this.stations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<TransStation> getStations() {
        return stations;
    }

    public void addStation(TransStation station) {
        int oldIndex = stations.indexOf(station);
        if (oldIndex < 0) {
            stations.add(station);
            return;
        }
        TransStation oldStation = stations.get(oldIndex);
        if (oldStation.getSchedule() == null) stations.set(oldIndex, station);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransChain that = (TransChain) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public String toString() {
        return "TransChain{" +
                "name='" + name + '\'' +
                '}';
    }
}
