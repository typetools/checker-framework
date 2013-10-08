import checkers.quals.*;
import checkers.fenum.quals.*;
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
@interface RestrictionType {}

