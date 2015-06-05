import java.util.List;

import org.checkerframework.checker.igj.qual.*;

public class ThrowCatch {
    boolean flag = true;

    // Type var test
    <E extends @ReadOnly Exception> void throwTypeVarReadOnly1(E ex1,
            @ReadOnly E ex2) throws Exception {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw ex1;
        }
        //:: error: (throw.type.invalid)
        throw ex2;
    }

    <@ReadOnly E extends @ReadOnly Exception> void throwTypeVarReadOnly2(E ex1) throws Exception {
        //:: error: (throw.type.invalid)
        throw ex1;
    }

    <E extends @Mutable Exception> void throwTypeVarMutable1(E ex1,
            @Mutable E ex2) throws Exception {
        if (flag) {
            throw ex1;
        }
        throw ex2;
    }

    <@Mutable E extends Exception> void throwTypeVarMutable2(E ex1,
            @Mutable E ex2) throws Exception {
        if (flag) {
            throw ex1;
        }
        throw ex2;
    }

    <E extends @Immutable Exception> void throwTypeVarImmutable1(E ex1,
            @Immutable E ex2) throws Exception {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw ex1;
        }
        //:: error: (throw.type.invalid)
        throw ex2;
    }

    <@Immutable E extends @Immutable Exception> void throwTypeVarImmutable2(E ex1) throws Exception {
         //:: error: (throw.type.invalid)
         throw ex1;
    }

    <@Mutable E extends @ReadOnly Exception> void throwTypeVarMixed(E ex1,
            @Immutable E ex2) throws Exception {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw ex1;
        }
        //:: error: (throw.type.invalid)
        throw ex2;
    }

    // Wildcards
    void throwWildcardReadOnly1(List<? extends @ReadOnly Exception> readonly,
            List<? extends @Mutable Exception> imut,
            List<? extends @Immutable Exception> mut) throws Exception {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw readonly.get(0);
        }
        if (flag) {
            throw imut.get(0);
        }
        //:: error: (throw.type.invalid)
        throw mut.get(0);
    }

    void throwNull() {
        throw null;
    }

    // Declared
    @ReadOnly
    Exception readOnly = new Exception();
    @Mutable
    Exception mutable = new Exception();
    @Immutable
    Exception immutable = null;

    void throwDeclared() {
        try {
            //:: error: (throw.type.invalid)
            throw readOnly;
        } catch (@ReadOnly Exception readOnlyParam) {

        }
        try {
            throw mutable;
        } catch (@Mutable Exception mutableParam) {

        }
        try {
            //:: error: (throw.type.invalid)
            throw immutable;
            //:: error: (exception.parameter.invalid)
        } catch (@Immutable Exception immutableParam) {

        }
    }

    // Test exception parameters
    void unionTypes() {
        try {
        } catch (@Mutable NullPointerException | @Mutable ArrayStoreException unionParam) {

        }
        try {
        } catch (@ReadOnly NullPointerException | @ReadOnly ArrayStoreException unionParam) {

        }
        try {
        } catch (@Immutable NullPointerException
                //:: error: (exception.parameter.invalid)
                | @Immutable ArrayStoreException unionParam) {

        }
    }

    void defaults() {
        try {
            throw new Exception();
        } catch (Exception e) {

        }
    }
}
