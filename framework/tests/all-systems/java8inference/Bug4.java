package inference.guava;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

@SuppressWarnings("") // Just check for crashes.
public class Bug4 {
    Type resolveInternal(TypeVariable<?> var, Type[] types) {
        return method(var.getGenericDeclaration(), var.getName(), types);
    }

    static <D extends GenericDeclaration> TypeVariable<D> method(D d, String n, Type... bounds) {
        throw new RuntimeException();
    }
}
