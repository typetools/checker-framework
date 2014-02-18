import checkers.nullness.quals.*;

public class InitThrows {
    private final Object o;
    
    {
        try {
            o = new Object();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
