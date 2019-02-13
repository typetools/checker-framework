import org.checkerframework.checker.signedness.qual.Unsigned;

public class AnnoBeforeModifier {

    // :: warning: (type.anno.before.modifiers)
    @Unsigned public int i;

    public @Unsigned int j;

    @SuppressWarnings("foobar")
    public int a;

    public @SuppressWarnings("foobar") int b;
}
