package checkers.quals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



    /**
     *If a method is annotated with this declaration annotation, 
     *then its signature is not written in a stub file and the method 
     *is not declared in source.  This annotation is added in AnnotatedTypeFactory. 
     *If a method does not have a Tree then it is FromBinary. (If a method is 
     *already annotated with @FromStubFile, this annotation is not added.)  
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR,
        ElementType.TYPE, ElementType.PACKAGE})
    public @interface FromByteCode {}
