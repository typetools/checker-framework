// @skip-test

import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeyFor_DirectionsFinder {

    class GeoPoint {}

    class StreetSegment {}

    class Graph {
        public void addEdge(StreetSegment endSeg, StreetSegment beginSeg) {}
    }

    public void buildGraph(List<StreetSegment> segs) {
        Map<GeoPoint, Set<StreetSegment>> endMap = new HashMap<>();
        Map<@KeyFor("endMap") GeoPoint, Set<StreetSegment>> beginMap = new HashMap<>();
        Graph graph = new Graph();

        for (StreetSegment seg : segs) {
            GeoPoint p1 = new GeoPoint();

            if (!(beginMap.containsKey(p1))) {
                endMap.put(p1, new HashSet<StreetSegment>());
                beginMap.put(p1, new HashSet<StreetSegment>());
            }
            endMap.get(p1).add(seg);
            beginMap.get(p1).add(seg);
        }

        for (@KeyFor("endMap") GeoPoint p : beginMap.keySet()) {
            for (StreetSegment beginSeg : beginMap.get(p)) {
                for (StreetSegment endSeg : endMap.get(p)) {
                    graph.addEdge(endSeg, beginSeg); // endSeg and beginSeg are @NonNull
                }
            }
        }
    }
}
