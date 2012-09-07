import checkers.quals.*;
import java.lang.annotation.*;

@TypeQualifier
@Target(ElementType.TYPE_USE)
@SubtypeOf(Unqualified.class)
public @interface Encrypted {}
