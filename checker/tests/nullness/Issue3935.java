import android.annotation.Nullable;

public class Issue3935 {
    // Note: Nullable is a declaration annotation and applies to the array, not the array element.
    private @Nullable byte[] data;

    // Declaration annotations on primitives are ignored, but this should issue
    // a nullness.on.primitive error.
    @Nullable byte b;
}
