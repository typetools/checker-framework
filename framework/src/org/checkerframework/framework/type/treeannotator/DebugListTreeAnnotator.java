package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/** A ListTreeAnnotator implementation that additionally outputs debugging information. */
public class DebugListTreeAnnotator extends ListTreeAnnotator {
    private final Set<Kind> kinds;

    public DebugListTreeAnnotator(TreeAnnotator... annotators) {
        super(annotators);
        kinds = Collections.emptySet();
    }

    public DebugListTreeAnnotator(Tree.Kind[] kinds, TreeAnnotator... annotators) {
        super(annotators);
        this.kinds = new HashSet<>(Arrays.asList(kinds));
    }

    @Override
    public Void defaultAction(Tree node, AnnotatedTypeMirror type) {
        if (kinds.contains(node.getKind())) {
            System.out.println("DebugListTreeAnnotator input tree: " + node);
            System.out.println("    Initial type: " + type);
            for (TreeAnnotator annotator : annotators) {
                System.out.println("    Running annotator: " + annotator.getClass());
                annotator.visit(node, type);
                System.out.println("    Current type: " + type);
            }
        } else {
            super.defaultAction(node, type);
        }

        return null;
    }
}
