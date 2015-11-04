package tests.defaulting;

import com.sun.source.tree.Tree.Kind;
import org.checkerframework.framework.qual.*;

import java.lang.annotation.*;

public class FieldQual {

    @DefaultQualifierInHierarchy
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({})
    @Target({ElementType.TYPE_USE})
    public static @interface F_TOP {
    }


    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(F_TOP.class)
    @ImplicitFor(trees = Kind.MEMBER_SELECT)
    @Target({ElementType.TYPE_USE})
    /**
     * This annotation should not be applied implicitly
     * because the type of a member select should
     * the type of the member.
     */
    public static @interface F_MEMBER_SELECT {
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf(F_TOP.class)
    @DefaultFor(DefaultLocation.FIELD)
    @Target({ElementType.TYPE_USE})
    public static @interface F_FIELD {
    }


    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @SubtypeOf({F_FIELD.class, F_MEMBER_SELECT.class})
    @Target({ElementType.TYPE_USE})
    public static @interface F_BOTTOM {
    }

}
