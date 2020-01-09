public class Issue2030 {
    double roundIntermediate(double x) {
        if (x >= 0.0) {
            return x;
        } else {
            return (long) x - 1;
        }
    }
}
