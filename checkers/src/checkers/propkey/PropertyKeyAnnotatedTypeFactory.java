package checkers.propkey;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.Bottom;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

/**
 * This AnnotatedTypeFactory adds PropertyKey annotations to String literals
 * that contain values from lookupKeys.
 *
 * @author wmdietl
 */
public class PropertyKeyAnnotatedTypeFactory<Checker extends PropertyKeyChecker>
        extends BasicAnnotatedTypeFactory<Checker> {

    private final Set<String> lookupKeys;

    public PropertyKeyAnnotatedTypeFactory(Checker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.lookupKeys = checker.getLookupKeys();

        // Reuse the framework Bottom annotation and make it the default for the
        // null literal.
        AnnotationMirror BOTTOM = this.annotations.fromClass(Bottom.class);
        this.treeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, BOTTOM);

        this.postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator(Checker checker) {
           return new KeyLookupTreeAnnotator(checker, this, PropertyKey.class);
    }


    /**
     * This TreeAnnotator checks for every String literal whether it is included in the lookup
     * keys. If it is, the given annotation is added to the literal; otherwise, nothing happens.
     * Subclasses of this AnnotatedTypeFactory can directly reuse this class and use a different
     * annotation as parameter.
     */
    protected class KeyLookupTreeAnnotator extends TreeAnnotator {
        Class<? extends Annotation> theAnnot;

        public KeyLookupTreeAnnotator(BaseTypeChecker checker,
                BasicAnnotatedTypeFactory<?> tf, Class<? extends Annotation> annot) {
            super(checker, tf);
            theAnnot = annot;
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && tree.getKind() == Tree.Kind.STRING_LITERAL
                && strContains(lookupKeys, tree.getValue().toString())) {
                type.addAnnotation(theAnnot);
            }
            // A possible extension is to record all the keys that have been used and
            // in the end output a list of keys that were not used in the program,
            // possibly pointing to the opposite problem, keys that were supposed to
            // be used somewhere, but have not been, maybe because of copy-and-paste errors.
            return super.visitLiteral(tree, type);
        }
    }

    /**
     * Instead of a precise comparison, we incrementally remove leading dot-separated
     * strings until we find a match.
     * For example if messages contains "y.z" and we look for "x.y.z" we find a match
     * after removing the first "x.".
     *
     * Compare to SourceChecker.fullMessageOf.
     */
    private static boolean strContains(Set<String> messages, String messageKey) {
        String key = messageKey;

        do {
            if (messages.contains(key)) {
                return true;
            }

            int dot = key.indexOf('.');
            if (dot < 0) return false;
            key = key.substring(dot + 1);
        } while (true);
    }
}
