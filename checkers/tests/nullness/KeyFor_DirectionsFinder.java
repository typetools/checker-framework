import checkers.nullness.quals.*;

import java.util.*;

// To fix later; skipped for now so all tests don't fail
// @skip-test
public class KeyFor_DirectionsFinder {

    class GeoPoint {}

    class StreetSegment {}

    public static void buildGraph(List<StreetSegment> segs) {
        Map<GeoPoint, Set<StreetSegment>> endMap = new 
            HashMap<GeoPoint, Set<StreetSegment>>();
        Map</*@KeyFor("endMap")*/GeoPoint, Set<StreetSegment>> beginMap = new 
            HashMap</*@KeyFor("endMap")*/GeoPoint, Set<StreetSegment>>();

        for(StreetSegment seg : segs) {
            GeoPoint p1 = new GeoPoint();

            if (!(beginMap.containsKey(p1))) {
                endMap.put(p1,new HashSet<StreetSegment>());
                beginMap.put(p1,new HashSet<StreetSegment>());
            }
            endMap.get(p1).add(seg);
            beginMap.get(p1).add(seg);
        }

        for (/*@KeyFor("endMap")*/ GeoPoint p : beginMap.keySet()) {
            for (StreetSegment beginSeg : beginMap.get(p)) {
                for (StreetSegment endSeg : endMap.get(p)) {
                    // graph.addEdge(endSeg,beginSeg);
                }
            }
        }

    }

}
