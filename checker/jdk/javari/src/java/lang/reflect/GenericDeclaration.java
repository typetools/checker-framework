package java.lang.reflect;
import org.checkerframework.checker.javari.qual.*;

public @ReadOnly interface GenericDeclaration {
    public TypeVariable<?>[] getTypeParameters();
}
