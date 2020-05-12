// Copied from the Object Construction Checker.

public class Parens {
    public synchronized void incrementPushed(long[] pushed, int operationType) {
        ++(pushed[operationType]);
    }
}
