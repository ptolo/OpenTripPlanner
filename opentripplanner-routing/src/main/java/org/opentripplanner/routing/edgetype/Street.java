package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class Street extends AbstractPayload {

    private static final long serialVersionUID = -3215764532108343102L;

    private static final String[] DIRECTIONS = { "north", "northeast", "east", "southeast",
            "south", "southwest", "west", "northwest" };

    String id;

    String name;

    LineString geometry;

    double length;

    public Street(double length) {
        this.length = length;
    }

    public Street(String id, String name, double length) {
        this.id = id;
        this.name = name;
        this.length = length;
    }

    public void setGeometry(LineString g) {
        geometry = g;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        double weight = this.length / wo.speed;
        // it takes time to walk/bike along a street, so update state accordingly
        s1.incrementTimeInSeconds((int) weight);
        return new TraverseResult(weight, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        double weight = this.length / wo.speed;
        // time moves *backwards* when traversing an edge in the opposite direction
        s1.incrementTimeInSeconds(-(int) weight);
        return new TraverseResult(weight, s1);
    }

    public String toString() {
        if (this.name != null) {
            return "Street(" + this.id + ", " + this.name + ", " + this.length + ")";
        } else {
            return "Street(" + this.length + ")";
        }
    }

    public String getDirection() {
        Coordinate[] coordinates = geometry.getCoordinates();
        return getDirection(coordinates[0], coordinates[coordinates.length - 1]);
    }

    private static String getDirection(Coordinate a, Coordinate b) {
        double run = b.x - a.x;
        double rise = b.y - a.y;
        double direction = Math.atan2(run, rise);
        int octant = (int) (8 + Math.round(direction * 8 / (Math.PI * 2))) % 8;

        return DIRECTIONS[octant];
    }

    public static String computeDirection(Point startPoint, Point endPoint) {
        return getDirection(startPoint.getCoordinate(), endPoint.getCoordinate());
    }

    public double getDistance() {
        return length;
    }

    public String getEnd() {
        // TODO Auto-generated method stub
        return null;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public TransportationMode getMode() {
        // this is actually WALK or BICYCLE depending on the TraverseOptions
        return TransportationMode.WALK;
    }

    public String getName() {
        return name;
    }

    public String getStart() {
        // TODO Auto-generated method stub
        return null;
    }

}