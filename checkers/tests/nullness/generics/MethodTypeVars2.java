import checkers.nullness.quals.*;

public class MethodTypeVars2 {
    
    class GeoSegment {}
    class StreetSegment extends GeoSegment {}
    
    interface Path<N, P extends Path<N, P>> {}

    class StreetSegmentPath implements
    Path<StreetSegment, StreetSegmentPath> {}

    private static <N extends GeoSegment, P extends Path<N, P>>
    @Nullable Object pathToRoute(Path<N, P> path)
      { return null; }
    
    void call(StreetSegmentPath p) {
        Object r = pathToRoute(p);
    }

    /*
    static class WorkingWithOne {
        interface GPath<P extends GPath<P>> {}

        class GStreetSegmentPath implements GPath<GStreetSegmentPath> {}

        private static <P extends GPath<P>>
        @Nullable Object pathToRoute(GPath<P> path)
        { return null; }
        
        void call(GStreetSegmentPath p) {
            Object r = pathToRoute(p);
        }
    }*/
}