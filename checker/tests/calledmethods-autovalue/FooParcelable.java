import android.os.Parcelable;
import com.google.auto.value.AutoValue;

/**
 * Test for support of AutoValue Parcel extension. This test currently passes, but only because we
 * ignore cases where we cannot find a matching setter for a method we think corresponds to an
 * AutoValue property. See https://github.com/kelloggm/object-construction-checker/issues/110
 */
@AutoValue
abstract class FooParcelable implements Parcelable {
    abstract String name();

    static Builder builder() {
        return new AutoValue_FooParcelable.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder setName(String value);

        abstract FooParcelable build();
    }

    public static void buildSomethingWrong() {
        Builder b = builder();
        // :: error: finalizer.invocation.invalid
        b.build();
    }

    public static void buildSomethingRight() {
        Builder b = builder();
        b.setName("Frank");
        b.build();
    }
}
