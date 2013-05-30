package checkers.quals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



    /**
     * A special annotation intended solely for indicating that a method is written
     * in a stub file. It should only be used by the StubParser.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR,
        ElementType.TYPE, ElementType.PACKAGE})
    public @interface FromStubFile {}
