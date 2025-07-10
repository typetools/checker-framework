import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface Bla {}

public class GenericAnnoBound<X extends @Bla Object> {
  GenericAnnoBound(GenericAnnoBound<X> n, X p) {}
}
