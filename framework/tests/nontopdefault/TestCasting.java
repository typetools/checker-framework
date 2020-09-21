import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;

@SuppressWarnings("inconsistent.constructor.type") // Not the point of this test
public class TestCasting {
    void repro(@NTDMiddle long startTime) {
        try {
            System.out.println("Inside try");
            return;
        } catch (Exception ex) {
            long timeTaken = startTime;
            @NTDMiddle double dblTimeTaken = timeTaken;

            throw new IllegalArgumentException();
        } finally {
            long timeTaken2 = startTime;
            // This assignment used to fail.
            @NTDMiddle double dblTimeTaken2 = timeTaken2;
        }
    }
}
