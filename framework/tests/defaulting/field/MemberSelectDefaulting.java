package defaulting.field;

import tests.defaulting.FieldQual.*;

import java.lang.Comparable;
import java.lang.Object;
import java.lang.Override;

public class MemberSelectDefaulting {

    // Sanity check.
    @F_FIELD Short max = Short.MAX_VALUE;

    // The type of Short.class and short.class should be identical
    // short.class use to have the type @F_MEMBER_SELECT Class<@F_TOP Short>
    // because the ImplicitsTreeAnnotator was used to add annotations.
    @F_FIELD Class<@F_TOP Short> o1 = Short.class;
    @F_FIELD Class<@F_TOP Short> o2 = short.class;
    @F_FIELD Short s = new  java.lang. @F_FIELD Short("2");
}
