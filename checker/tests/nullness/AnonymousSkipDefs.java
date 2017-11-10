import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class AnonymousSkipDefs {

    public static void main(String[] args) {
        call(
                new Runnable() {
                    @Override
                    public void run() {
                        @Nullable Object veryNull = null;
                        // :: error: (assignment.type.incompatible)
                        @NonNull Object notNull = veryNull;
                        notNull.toString();
                    }
                });
    }

    private static void call(Runnable r) {
        r.run();
    }
}
