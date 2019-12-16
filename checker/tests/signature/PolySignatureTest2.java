import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.signature.qual.*;

public class PolySignatureTest2 {

    @DotSeparatedIdentifiers Name m1(TypeElement e) {
        return e.getQualifiedName();
    }

    @DotSeparatedIdentifiers String m2(@DotSeparatedIdentifiers Name n) {
        return n.toString();
    }
}
