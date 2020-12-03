package org.checkerframework.checker.nullness;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.lang.model.element.Element;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class NullnessWholeProgramInferenceScenes extends WholeProgramInferenceScenes {

    /** Create a NullnessWholeProgramInferenceScenes. */
    public NullnessWholeProgramInferenceScenes(AnnotatedTypeFactory atypeFactory) {
        // "false" argument means don't ignore null assignments.
        super(atypeFactory, false);
    }

    // If
    //  1. rhs is @Nullable
    //  2. lhs is a field of this
    //  3. in a constructor
    // then change rhs to @MonotonicNonNull.
    @Override
    public void wpiAdjustForUpdateField(
            Tree lhsTree, Element element, String fieldName, AnnotatedTypeMirror rhsATM) {
        if (!rhsATM.hasAnnotation(Nullable.class)) {
            return;
        }
        TreePath lhsPath = atypeFactory.getPath(lhsTree);
        if (TreeUtils.enclosingClass(lhsPath) == ((VarSymbol) element).enclClass()
                && TreeUtils.inConstructor(lhsPath)) {
            rhsATM.replaceAnnotation(
                    ((NullnessAnnotatedTypeFactory) atypeFactory).MONOTONIC_NONNULL);
        }
    }

    // If
    //  1. rhs is @MonotonicNonNull
    // then change rhs to @Nullable
    @Override
    public void wpiAdjustForUpdateNonField(AnnotatedTypeMirror rhsATM) {
        if (rhsATM.hasAnnotation(MonotonicNonNull.class)) {
            rhsATM.replaceAnnotation(((NullnessAnnotatedTypeFactory) atypeFactory).NULLABLE);
        }
    }
}
