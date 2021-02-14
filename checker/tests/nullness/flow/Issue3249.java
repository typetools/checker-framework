// Test case for Issue 3249:
// https://github.com/typetools/checker-framework/issues/3249

public class Issue3249 {

    private final double field;

    Issue3249() {
        double local;
        while (true) {
            local = 1;
            break;
        }
        field = local;
    }

    Issue3249(int x) {
        double local;
        while (!false) {
            local = 1;
            break;
        }
        field = local;
    }

    Issue3249(float x) {
        double local;
        while (true || x > 0) {
            local = 1;
            break;
        }
        field = local;
    }

    Issue3249(double x) {
        double local;
        while (!false && true && !false) {
            local = 1;
            break;
        }
        field = local;
    }

    // Case for while conditions that contain final variables,
    // which are treated as constant.
    Issue3249(String x) {
        double local;
        final int i = 1;
        while ((i > 0) && !false) {
            local = 1;
            break;
        }
        field = local;
    }

    Issue3249(boolean x) {
        double local;
        while (6 > 4) {
            local = 1;
            break;
        }
        field = local;
    }
}
