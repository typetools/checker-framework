import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java16-jdk-skip-test

public class NestedRecordTest {

    static @NonNull String nn = "foo";
    static @Nullable String nble = null;
    static @NonNull String nn2 = "foo";
    static @Nullable String nble2 = null;

    public static class Nested {
        public record NPerson(String familyName, @Nullable String maidenName) {}

        void nclient() {
            Nested.NPerson np1 = new Nested.NPerson(nn, nn);
            Nested.NPerson np2 = new Nested.NPerson(nn, nble);
            // :: error: (argument.type.incompatible)
            Nested.NPerson np3 = new Nested.NPerson(nble, nn);
            // :: error: (argument.type.incompatible)
            Nested.NPerson np4 = new Nested.NPerson(nble, nble);
            Inner.IPerson ip1 = new Inner.IPerson(nn, nn);
            Inner.IPerson ip2 = new Inner.IPerson(nn, nble);
            // :: error: (argument.type.incompatible)
            Inner.IPerson ip3 = new Inner.IPerson(nble, nn);
            // :: error: (argument.type.incompatible)
            Inner.IPerson ip4 = new Inner.IPerson(nble, nble);

            nn2 = np2.familyName();
            nble2 = np2.familyName();
            // :: error: (assignment.type.incompatible)
            nn2 = np2.maidenName();
            nble2 = np2.maidenName();
            nn2 = ip2.familyName();
            nble2 = ip2.familyName();
            // :: error: (assignment.type.incompatible)
            nn2 = ip2.maidenName();
            nble2 = ip2.maidenName();
        }
    }

    public class Inner {
        public record IPerson(String familyName, @Nullable String maidenName) {}

        void iclient() {
            Nested.NPerson np1 = new Nested.NPerson(nn, nn);
            Nested.NPerson np2 = new Nested.NPerson(nn, nble);
            // :: error: (argument.type.incompatible)
            Nested.NPerson np3 = new Nested.NPerson(nble, nn);
            // :: error: (argument.type.incompatible)
            Nested.NPerson np4 = new Nested.NPerson(nble, nble);
            Inner.IPerson ip1 = new Inner.IPerson(nn, nn);
            Inner.IPerson ip2 = new Inner.IPerson(nn, nble);
            // :: error: (argument.type.incompatible)
            Inner.IPerson ip3 = new Inner.IPerson(nble, nn);
            // :: error: (argument.type.incompatible)
            Inner.IPerson ip4 = new Inner.IPerson(nble, nble);

            nn2 = np2.familyName();
            nble2 = np2.familyName();
            // :: error: (assignment.type.incompatible)
            nn2 = np2.maidenName();
            nble2 = np2.maidenName();
            nn2 = ip2.familyName();
            nble2 = ip2.familyName();
            // :: error: (assignment.type.incompatible)
            nn2 = ip2.maidenName();
            nble2 = ip2.maidenName();
        }
    }

    void client() {
        Nested.NPerson np1 = new Nested.NPerson(nn, nn);
        Nested.NPerson np2 = new Nested.NPerson(nn, nble);
        // :: error: (argument.type.incompatible)
        Nested.NPerson np3 = new Nested.NPerson(nble, nn);
        // :: error: (argument.type.incompatible)
        Nested.NPerson np4 = new Nested.NPerson(nble, nble);
        Inner.IPerson ip1 = new Inner.IPerson(nn, nn);
        Inner.IPerson ip2 = new Inner.IPerson(nn, nble);
        // :: error: (argument.type.incompatible)
        Inner.IPerson ip3 = new Inner.IPerson(nble, nn);
        // :: error: (argument.type.incompatible)
        Inner.IPerson ip4 = new Inner.IPerson(nble, nble);

        nn2 = np2.familyName();
        nble2 = np2.familyName();
        // :: error: (assignment.type.incompatible)
        nn2 = np2.maidenName();
        nble2 = np2.maidenName();
        nn2 = ip2.familyName();
        nble2 = ip2.familyName();
        // :: error: (assignment.type.incompatible)
        nn2 = ip2.maidenName();
        nble2 = ip2.maidenName();
    }
}
