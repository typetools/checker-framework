// skip-idempotent
import java.util.List;

public class CheckerFrameworkBuilder {

    /**
     * Most of this test was copied from
     * https://raw.githubusercontent.com/projectlombok/lombok/master/test/transform/resource/after-delombok/CheckerFrameworkBuilder.java
     * with the exception of the following lines until the next long comment. I have made one change
     * outside the scope of these comments: - I fixed the placement of the type annotations, which
     * were originally on scoping constructs. I think this is a bug in the delombok pretty-printer
     * used to generate this code, but I wasn't able to find the configuration options used to
     * reproduce it in the public release.
     *
     * <p>This test represents exactly the code that Lombok generates with the checkerframework =
     * True option in a lombok.config file, including the weird package names they use for the CF
     * and the {@code @NotCalledMethods} annotation that they use even though we don't (and never
     * have) supported such a thing.
     *
     * <p>The new code added in this block ensures that the Called Methods checker handles clients
     * of the copied code correctly.
     */
    public static void testOldCalledMethodsGood(
                    @org.checkerframework.checker.calledmethods.qual.CalledMethods({"y", "z"}) CheckerFrameworkBuilderBuilder pb) {
        pb.build();
    }

    public static void testOldCalledMethodsBad(
                    @org.checkerframework.checker.calledmethods.qual.CalledMethods({"y"}) CheckerFrameworkBuilderBuilder pb) {
        // :: error: finalizer.invocation.invalid
        pb.build(); // pb requires y, z
    }

    public static void testOldRRGood() {
        CheckerFrameworkBuilder b = CheckerFrameworkBuilder.builder().y(5).z(6).build();
    }

    public static void testOldRRBad() {
        CheckerFrameworkBuilder b =
                // :: error: finalizer.invocation.invalid
                CheckerFrameworkBuilder.builder().z(6).build(); // also needs to call y
    }

    /** End new, non-copied code. */
    int x;

    int y;
    int z;
    List<String> names;

    @java.lang.SuppressWarnings("all")
    private static int $default$x() {
        return 5;
    }

    @org.checkerframework.common.aliasing.qual.Unique @java.lang.SuppressWarnings("all")
    CheckerFrameworkBuilder(final int x, final int y, final int z, final List<String> names) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.names = names;
    }

    @java.lang.SuppressWarnings("all")
    public static class CheckerFrameworkBuilderBuilder {
        @java.lang.SuppressWarnings("all")
        private boolean x$set;

        @java.lang.SuppressWarnings("all")
        private int x$value;

        @java.lang.SuppressWarnings("all")
        private int y;

        @java.lang.SuppressWarnings("all")
        private int z;

        @java.lang.SuppressWarnings("all")
        private java.util.ArrayList<String> names;

        @org.checkerframework.common.aliasing.qual.Unique @java.lang.SuppressWarnings("all")
        CheckerFrameworkBuilderBuilder() {}

        @org.checkerframework.checker.builder.qual.ReturnsReceiver
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder x(
                CheckerFrameworkBuilder.@org.checkerframework.checker.builder.qual.NotCalledMethods(
                                "x")
                        CheckerFrameworkBuilderBuilder
                        this,
                final int x) {
            this.x$value = x;
            x$set = true;
            return this;
        }

        @org.checkerframework.checker.builder.qual.ReturnsReceiver
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder y(
                CheckerFrameworkBuilder.@org.checkerframework.checker.builder.qual.NotCalledMethods(
                                "y")
                        CheckerFrameworkBuilderBuilder
                        this,
                final int y) {
            this.y = y;
            return this;
        }

        @org.checkerframework.checker.builder.qual.ReturnsReceiver
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder z(
                CheckerFrameworkBuilder.@org.checkerframework.checker.builder.qual.NotCalledMethods(
                                "z")
                        CheckerFrameworkBuilderBuilder
                        this,
                final int z) {
            this.z = z;
            return this;
        }

        @org.checkerframework.checker.builder.qual.ReturnsReceiver
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder name(final String name) {
            if (this.names == null) {
                this.names = new java.util.ArrayList<String>();
            }
            this.names.add(name);
            return this;
        }

        @org.checkerframework.checker.builder.qual.ReturnsReceiver
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder names(
                final java.util.Collection<? extends String> names) {
            if (names == null) {
                throw new java.lang.NullPointerException("names cannot be null");
            }
            if (this.names == null) {
                this.names = new java.util.ArrayList<String>();
            }
            this.names.addAll(names);
            return this;
        }

        @org.checkerframework.checker.builder.qual.ReturnsReceiver
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder clearNames() {
            if (this.names != null) {
                this.names.clear();
            }
            return this;
        }

        @org.checkerframework.dataflow.qual.SideEffectFree
        @java.lang.SuppressWarnings("all")
        public CheckerFrameworkBuilder build(
                CheckerFrameworkBuilder.@org.checkerframework.checker.builder.qual.CalledMethods({
                            "y", "z"
                        })
                        CheckerFrameworkBuilderBuilder
                        this) {
            java.util.List<String> names;
            switch (this.names == null ? 0 : this.names.size()) {
                case 0:
                    names = java.util.Collections.emptyList();
                    break;
                case 1:
                    names = java.util.Collections.singletonList(this.names.get(0));
                    break;
                default:
                    names =
                            java.util.Collections.unmodifiableList(
                                    new java.util.ArrayList<String>(this.names));
            }
            int x$value = this.x$value;
            if (!this.x$set) {
                x$value = CheckerFrameworkBuilder.$default$x();
            }
            return new CheckerFrameworkBuilder(x$value, this.y, this.z, names);
        }

        @org.checkerframework.dataflow.qual.SideEffectFree
        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder(x$value="
                    + this.x$value
                    + ", y="
                    + this.y
                    + ", z="
                    + this.z
                    + ", names="
                    + this.names
                    + ")";
        }
    }

    @org.checkerframework.dataflow.qual.SideEffectFree
    @java.lang.SuppressWarnings("all")
    public static CheckerFrameworkBuilder.
            @org.checkerframework.common.aliasing.qual.Unique CheckerFrameworkBuilderBuilder builder() {
        return new CheckerFrameworkBuilder.CheckerFrameworkBuilderBuilder();
    }
}
