package org.checkerframework.checker.index.inequality;

import com.sun.source.tree.Tree;
import java.util.List;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class LessThanVisitor extends BaseTypeVisitor<LessThanAnnotatedTypeFactory> {

    public LessThanVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey) {
        // If value is less than all expressions in the annotation in varType,
        // using the Value Checker, then skip the common assignment check.
        List<String> expressions =
                LessThanAnnotatedTypeFactory.getLessThanExpressions(
                        varType.getEffectiveAnnotationInHierarchy(atypeFactory.UNKNOWN));
        if (expressions != null) {
            boolean isLessThan = true;
            for (String expression : expressions) {
                if (!atypeFactory.isLessThanByValue(valueTree, expression, getCurrentPath())) {
                    isLessThan = false;
                }
            }
            if (isLessThan) {
                if (checker.hasOption("showchecks")) {
                    // Print the success message because super isn't called.
                    long valuePos = positions.getStartPosition(root, valueTree);
                    System.out.printf(
                            " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                            "success: actual is subtype of expected",
                            (root.getLineMap() != null
                                    ? root.getLineMap().getLineNumber(valuePos)
                                    : -1),
                            valueTree.getKind(),
                            valueTree,
                            valueType.getKind(),
                            valueType.toString(),
                            varType.getKind(),
                            varType.toString());
                }
                // skip call to super.
                return;
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }
}
