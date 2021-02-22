public class Bug109_B extends Bug109_A {
    public Bug109_B() {
        // Accessing field one causes NPE
        // at org.checkerframework.checker.nullness.MapGetHeuristics.handle
        //   (MapGetHeuristics.java:91)

        int myone = one;

        int mytwo = two;
    }
}
