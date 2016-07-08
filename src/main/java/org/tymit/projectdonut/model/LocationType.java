package org.tymit.projectdonut.model;

public class LocationType {
    private final String visibleName;
    private final String encodedName;

    public LocationType(String visibleName, String encodedName) {
        this.visibleName = visibleName;
        this.encodedName = encodedName;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public String getEncodedname() {
        return encodedName;
    }
}
