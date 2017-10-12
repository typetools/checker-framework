import java.util.List;
import org.checkerframework.common.aliasing.qual.Unique;

class ForbiddenUniqueTest {

    // :: error: (unique.location.forbidden)
    @Unique int field;

    void notAllowed() {
        // :: error: (unique.location.forbidden)
        @Unique Object[] arr;
        // :: error: (type.argument.type.incompatible) :: error: (unique.location.forbidden)
        List<@Unique Object> list;
    }

    void allowed() {
        Object @Unique [] ar;
        @Unique List<Object> l;
    }
}
