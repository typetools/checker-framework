import java.util.List;

import org.checkerframework.checker.guieffect.qual.*;

class ThrowsTest {
    boolean flag = true;
    // Type var test
    <E extends @UI PolyUIException> void throwTypeVarUI1(E ex1, @UI E ex2) throws PolyUIException {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw ex1;
        }
        //:: error: (throw.type.invalid)
            throw ex2;
    }

    <@UI E extends @UI PolyUIException> void throwTypeVarUI2(E ex1) throws PolyUIException {
        //:: error: (throw.type.invalid)
        throw ex1;
    }

    <E extends @AlwaysSafe PolyUIException> void throwTypeVarAlwaysSafe1(E ex1, @AlwaysSafe E ex2) throws PolyUIException {
        if (flag) {
            throw ex1;
        }
            throw ex2;
    }
    <@AlwaysSafe E extends  PolyUIException> void throwTypeVarAlwaysSafe2(E ex1, @AlwaysSafe E ex2) throws PolyUIException {
        if (flag) {
            throw ex1;
        }
            throw ex2;
    }

    <@AlwaysSafe E extends @UI PolyUIException> void throwTypeVarMixed(E ex1, @AlwaysSafe E ex2) throws PolyUIException {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw ex1;
        }
            throw ex2;
    }

    // Wildcards
    //:: error: (type.argument.type.incompatible)
    void throwWildcard(List<? extends @UI PolyUIException> ui, // Default type of List's type parameter is below @UI so this is type.argument.incompatible
           List<? extends @AlwaysSafe PolyUIException> alwaysSafe) throws PolyUIException {
        if (flag) {
            //:: error: (throw.type.invalid)
            throw ui.get(0);
        }
            throw alwaysSafe.get(0);
    }

   void throwNull() {
       throw null;
   }
   // Declared
    @UI PolyUIException ui = new PolyUIException();
    @AlwaysSafe PolyUIException alwaysSafe = new PolyUIException();

    void throwDeclared() {
        try {
            //:: error: (throw.type.invalid)
            throw ui;
        } catch (@UI PolyUIException UIParam) {

        }
        try {
            throw alwaysSafe;
        } catch (@AlwaysSafe PolyUIException alwaysSafeParam) {

        }
    }

    // Test Exception parameters
    void unionTypes() {
// GuiEffectChecker throws an exception on this code.  When issue 384 is fixed, uncomment these lines.
//https://github.com/typetools/checker-framework/issues/384
//        try {
//        } catch (@AlwaysSafe NullPointerPolyUIException | @AlwaysSafe ArrayStorePolyUIException unionParam) {
//
//        }
//        try {
//        } catch (@UI NullPointerPolyUIException | @UI ArrayStorePolyUIException unionParam) {
//
//        }
    }

    void defaults() {
        try {
            throw new PolyUIException();
        } catch (PolyUIException e) {

        }
    }

@PolyUIType
class PolyUIException extends Exception {
}
@PolyUIType
class NullPointerPolyUIException extends NullPointerException {
}
@PolyUIType
class ArrayStorePolyUIException extends ArrayStoreException {
}
}
