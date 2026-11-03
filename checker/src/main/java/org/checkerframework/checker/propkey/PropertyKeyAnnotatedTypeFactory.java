package org.checkerframework.checker.propkey;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * This AnnotatedTypeFactory adds PropertyKey annotations to String literals that contain values
 * from lookupKeys.
 */
public class PropertyKeyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  private final Set<String> lookupKeys;

  @SuppressWarnings("this-escape")
  public PropertyKeyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.lookupKeys = Collections.unmodifiableSet(buildLookupKeys());

    this.postInit();
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new KeyLookupTreeAnnotator(this, PropertyKey.class));
  }

  // To allow subclasses access to createTreeAnnotator from the BATF.
  protected TreeAnnotator createBasicTreeAnnotator() {
    return super.createTreeAnnotator();
  }

  /**
   * This TreeAnnotator checks for every String literal whether it is included in the lookup keys.
   * If it is, the given annotation is added to the literal; otherwise, nothing happens. Subclasses
   * of this AnnotatedTypeFactory can directly reuse this class and use a different annotation as
   * parameter.
   */
  protected class KeyLookupTreeAnnotator extends TreeAnnotator {
    AnnotationMirror theAnnot;

    public KeyLookupTreeAnnotator(BaseAnnotatedTypeFactory atf, Class<? extends Annotation> annot) {
      super(atf);
      theAnnot = AnnotationBuilder.fromClass(elements, annot);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
      if (!type.hasPrimaryAnnotationInHierarchy(theAnnot)
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

    // Result of binary op might not be a property key.
    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      type.removePrimaryAnnotation(theAnnot);
      return null; // super.visitBinary(tree, type);
    }

    // Result of unary op might not be a property key.
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
      type.removePrimaryAnnotation(theAnnot);
      return null; // super.visitCompoundAssignment(tree, type);
    }
  }

  /**
   * Instead of a precise comparison, we incrementally remove leading dot-separated strings until we
   * find a match. For example if messages contains "y.z" and we look for "x.y.z" we find a match
   * after removing the first "x.".
   *
   * <p>Compare to SourceChecker.fullMessageOf.
   */
  private static boolean strContains(Set<String> messages, String messageKey) {
    String key = messageKey;

    do {
      if (messages.contains(key)) {
        return true;
      }

      int dot = key.indexOf('.');
      if (dot < 0) {
        return false;
      }
      key = key.substring(dot + 1);
    } while (true);
  }

  /**
   * Returns a set of the valid keys that can be used.
   *
   * @return the valid keys that can be used
   */
  public Set<String> getLookupKeys() {
    return this.lookupKeys;
  }

  /**
   * Returns the keys in the property files and resource bundles named on the command line.
   *
   * @return the keys in the property files and resource bundles named on the command line
   */
  private Set<String> buildLookupKeys() {
    return new HashSet<>(PropertyFileUtils.load(checker, getClass()).keySet());
  }
}
