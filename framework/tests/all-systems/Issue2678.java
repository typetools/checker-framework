public class Issue2678 {
    public synchronized void incrementPushed(long[] pushed, int operationType) {
        ++(pushed[operationType]);
    }
}
