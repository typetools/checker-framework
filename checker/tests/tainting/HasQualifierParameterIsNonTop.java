import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.framework.qual.HasQualifierParameter;

@HasQualifierParameter(Untainted.class)
// :: error: (invalid.qual.param)
class HasQualifierParameterIsNonTop {
  @PolyTainted String input;
}
