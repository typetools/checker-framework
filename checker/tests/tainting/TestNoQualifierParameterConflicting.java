import java.util.*;
import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.framework.qual.*;

@HasQualifierParameter(Tainted.class)
@NoQualifierParameter(Tainted.class)
// :: error: (conflicting.qual.param)
public class TestNoQualifierParameterConflicting {}
