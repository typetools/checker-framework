package org.checkerframework.framework.testchecker.nontopdefault;

import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDBottom;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

public class NTDVisitor extends BaseTypeVisitor<NTDAnnotatedTypeFactory> {
  public NTDVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  // Because classes and interfaces are by default NTDMiddle, an override is defined here which
  // allows references to be declared using any NDT type except NTDBottom.
  @Override
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    // eg for the statement "@NTDSide Double x;" the declarationType is @NTDMiddle
    // Double, and the useType is @NTDSide Double
    if (declarationType.getEffectiveAnnotation(NTDMiddle.class) != null
        && useType.getEffectiveAnnotation(NTDBottom.class) == null) {
      return true;
    } else {
      // otherwise check the usage using super
      return super.isValidUse(declarationType, useType, tree);
    }
  }
}
