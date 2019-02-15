import org.checkerframework.checker.signedness.qual.Unsigned;

public class AnnoBeforeModifier {

    // :: warning: (type.anno.before.modifier)
    @Unsigned public int i;

    public @Unsigned int j;

    // :: warning: (type.anno.before.modifier)
    public @Unsigned final int k;

    @SuppressWarnings("foobar")
    @Unsigned public int l;

    public @SuppressWarnings("foobar") @Unsigned int m;

    @SuppressWarnings("foobar")
    @Unsigned public int n;

    // :: warning: (type.anno.before.modifier)
    public @SuppressWarnings("foobar") @Unsigned final int o;

    // :: warning: (type.anno.before.decl.anno)
    public @Unsigned @SuppressWarnings("foobar") final int p;

    public @SuppressWarnings("foobar") final @Unsigned int q;

    @SuppressWarnings("foobar")
    public int r;

    public @SuppressWarnings("foobar") int s;

    public @SuppressWarnings("foobar") final int t;
}
