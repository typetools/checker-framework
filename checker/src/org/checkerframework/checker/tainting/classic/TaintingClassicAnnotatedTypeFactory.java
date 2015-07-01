package org.checkerframework.checker.tainting.classic;

import org.checkerframework.checker.tainting.classic.qual.Tainted;
import org.checkerframework.checker.tainting.classic.qual.Untainted;
import org.checkerframework.checker.tainting.classic.qual.PolyTainted;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

public class TaintingClassicAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror TAINTED, UNTAINTED, POLYTAINTED;

    public TaintingClassicAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        TAINTED = AnnotationUtils.fromClass(elements, Tainted.class);
        UNTAINTED = AnnotationUtils.fromClass(elements, Untainted.class);
        POLYTAINTED = AnnotationUtils.fromClass(elements, PolyTainted.class);

        addAliasedAnnotation(org.checkerframework.checker.tainting.qual.Tainted.class, TAINTED);
        addAliasedAnnotation(org.checkerframework.checker.tainting.qual.Untainted.class, UNTAINTED);
        addAliasedAnnotation(org.checkerframework.checker.tainting.qual.PolyTainted.class, POLYTAINTED);

        postInit();
    }
}

/*
package org.checkerframework.checker.tainting;

import com.sun.source.tree.*;

import org.checkerframework.common.basetype.BaseTypeChecker;
import Untainted;
import org.checkerframework.framework.type.SubtypingAnnotatedTypeFactory;
import org.checkerframework.framework.type.TreeAnnotator;
*/

/**
 * Adds implicit and default {@code Untainted} annotation, only if the user
 * does not explicitly insert them.
 * <p/>
 *
 * This factory will add the {@link Untainted} annotation to a type if the
 * input is
 *
 * <ol>
 * <li value="1">a string literal (Note: Handled by Unqualified meta-annotation)
 * <li value="2">a string concatenation where both operands are untainted
 * </ol>
 *
 */
/*
 * This class no longer performs any special functionality, as
 * superclasses where changed.
 * Currently both binary/compound ops on reference types and primitive
 * types give the LUB of the arguments. If there needs to be a difference
 * we could add a TreeAnnotator again.

public class TaintingAnnotatedTypeFactory
  extends SubtypingAnnotatedTypeFactory<TaintingChecker> {

    public TaintingAnnotatedTypeFactory(TaintingChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.postInit();
    }
}
*/
