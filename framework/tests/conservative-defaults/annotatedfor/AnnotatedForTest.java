import org.checkerframework.framework.qual.AnnotatedFor;
import tests.util.SubQual;
import tests.util.SuperQual;

class AnnotatedForTest
{
/*   
     Test a mix of @SuppressWarnings with @AnnotatedFor. @SuppressWarnings should win, but only within the kinds of warnings it promises to suppress. It should win because
     it is a specific intent of suppressing warnings, whereas NOT suppressing warnings using AnnotatedFor is a default behavior, and SW is a user-specified behavior.
*/

     @AnnotatedFor("subtyping") // This method is @AnnotatedFor("subtyping") so it can cause errors to be issued by calling other methods.
     void method1() {
        // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since @SuperQual is annotated with @DefaultQualifierInHierarchy.
        @SuperQual Object o1 = annotatedMethod(new Object());
        //:: error: (assignment.type.incompatible)
        @SubQual Object o2 = annotatedMethod(new Object());

        // When calling unannotatedMethod, we expect the conservative defaults.
        Object o3 = unannotatedMethod(o2);
        //:: error: (argument.type.incompatible)
        Object o4 = unannotatedMethod(o1);
        
        // Testing that @AnnotatedFor({}) behaves the same way as not putting an @AnnotatedFor annotation.
        Object o5 = unannotatedMethod(o2);
        //:: error: (argument.type.incompatible)
        Object o6 = unannotatedMethod(o1);

        // Testing that @AnnotatedFor(a different typesystem) behaves the same way @AnnotatedFor({})
        Object o7 = unannotatedMethod(o2);
        //:: error: (argument.type.incompatible)
        Object o8 = unannotatedMethod(o1);
    }
    
     @SuppressWarnings("all")
     @AnnotatedFor("subtyping") // Same as method1, but the @SuppressWarnings overrides the @AnnotatedFor.
     void method2() {
        // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since @SuperQual is annotated with @DefaultQualifierInHierarchy.
        @SuperQual Object o1 = annotatedMethod(new Object());
        @SubQual Object o2 = annotatedMethod(new Object());

        // When calling unannotatedMethod, we expect the conservative defaults.
        Object o3 = unannotatedMethod(o2);
        Object o4 = unannotatedMethod(o1);

        // Testing that @AnnotatedFor({}) behaves the same way as not putting an @AnnotatedFor annotation.
        Object o5 = unannotatedMethod(o2);
        Object o6 = unannotatedMethod(o1);

        // Testing that @AnnotatedFor(a different typesystem) behaves the same way @AnnotatedFor({})
        Object o7 = unannotatedMethod(o2);
        Object o8 = unannotatedMethod(o1);
    }

     @SuppressWarnings("nullness")
     @AnnotatedFor("subtyping") // Similar to method1. The @SuppressWarnings does not override the @AnnotatedFor because it suppressing warnings for a different typesystem.
     void method3() {
        // When calling annotatedMethod, we expect the usual (non-conservative) defaults, since @SuperQual is annotated with @DefaultQualifierInHierarchy.
        @SuperQual Object o1 = annotatedMethod(new Object());
        //:: error: (assignment.type.incompatible)
        @SubQual Object o2 = annotatedMethod(new Object());
    }
    
    @AnnotatedFor("subtyping")
    Object annotatedMethod(Object p) {
        return new Object();
    }

    Object unannotatedMethod(Object p) {
        return new Object();
    }

    @AnnotatedFor({})
    Object unannotatedMethod2(Object p) {
        return new Object();
    }

    @AnnotatedFor("nullness")
    Object annotatedForADifferentTypeSystemMethod(Object p) {
        return new Object();
    }

    @AnnotatedFor({"nullness", "subtyping"}) // Annotated for more than one type system
    void method4() {
       //:: error: (assignment.type.incompatible)
       @SubQual Object o2 = new @SuperQual Object();
    }

    @AnnotatedFor("SubtypingChecker") // Different way of writing the checker name
    void method5() {
       //:: error: (assignment.type.incompatible)
       @SubQual Object o2 = new @SuperQual Object();
    }

    @AnnotatedFor("org.checkerframework.common.subtyping.SubtypingChecker") // Different way of writing the checker name
    void method6() {
       //:: error: (assignment.type.incompatible)
       @SubQual Object o2 = new @SuperQual Object();
    }

    @AnnotatedFor("subtyping")
    class annotatedClass { // Every method in this class should issue warnings for subtyping even if it's not marked with AnnotatedFor, unless it's marked with SuppressWarnings.
        void method1() {
           //:: error: (assignment.type.incompatible)
           @SubQual Object o2 = new @SuperQual Object();
        }

        @SuppressWarnings("all")
        void method2() {
           @SubQual Object o2 = new @SuperQual Object();
        }
    }

    @SuppressWarnings("all")
    @AnnotatedFor("subtyping")
    class annotatedAndWarningsSuppressedClass { // The @SuppressWarnings("all") overrides the @AnnotatedFor.
        void method1() {
           @SubQual Object o2 = new @SuperQual Object();
        }
    }
    
}