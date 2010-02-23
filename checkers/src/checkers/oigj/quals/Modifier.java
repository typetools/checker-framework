package checkers.oigj.quals;

import java.lang.annotation.*;

import checkers.quals.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target( { FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE })
@TypeQualifier
@SubtypeOf({ Dominator.class })
@DefaultQualifierInHierarchy
public @interface Modifier { }
