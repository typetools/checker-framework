import checkers.nullness.quals.*;

public class MethodTypeVars2 {

    class GeoSegment {}
    interface Path<N1, P1 extends Path<N1, P1>> {}

    private static <N2 extends GeoSegment, P2 extends Path<N2, P2>>
    @Nullable Object pathToRoute(Path<N2, P2> path)
      { return null; }

    class StreetSegment extends GeoSegment {}
    class StreetSegmentPath implements
        Path<StreetSegment, StreetSegmentPath> {}


    void call(StreetSegmentPath p) {
        Object r = pathToRoute(p);
    }

    static class WorkingWithOne {
        interface GPath<P extends GPath<P>> {}

        class GStreetSegmentPath implements GPath<GStreetSegmentPath> {}

        private static <P extends GPath<P>>
        @Nullable Object pathToRoute(GPath<P> path)
        { return null; }

        void call(GStreetSegmentPath p) {
            Object r = pathToRoute(p);
        }
    }
}