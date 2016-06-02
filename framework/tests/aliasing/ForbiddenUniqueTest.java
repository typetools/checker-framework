import org.checkerframework.common.aliasing.qual.Unique;
import java.util.List;
class ForbiddenUniqueTest {

    //:: error: (unique.location.forbidden)
    @Unique int field;
    void notAllowed() {
        //:: error: (unique.location.forbidden)
        @Unique Object[] arr;
        //:: error: (unique.location.forbidden)
        List< @Unique Object> list;
    }

    void allowed() {
        Object @Unique [] ar;
        @Unique List<Object> l;
    }
}
