package org.checkerframework.framework.type;

import java.util.IdentityHashMap;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;

public class CaptureUtil {

  public static AnnotatedTypeVariable capture(
      AnnotatedWildcardType wildcard, TypeVariable capturedWildcard) {
    AnnotatedTypeVariable atv =
        (AnnotatedTypeVariable)
            AnnotatedTypeMirror.createType(
                capturedWildcard, wildcard.atypeFactory, wildcard.isDeclaration());
    AnnotatedTypeMirror superBound = new Visitor(atv, wildcard).visit(wildcard.getSuperBound());
    AnnotatedTypeMirror extendsBound = new Visitor(atv, wildcard).visit(wildcard.getExtendsBound());
    atv.setUpperBound(extendsBound);
    atv.setLowerBound(superBound);
    atv.addAnnotations(wildcard.getAnnotations());
    return atv;
  }

  private static class Visitor extends AnnotatedTypeCopier {
    private final AnnotatedTypeVariable typeVariable;
    private final AnnotatedWildcardType wildcardType;

    public Visitor(AnnotatedTypeVariable typeVariable, AnnotatedWildcardType wildcardType) {
      this.typeVariable = typeVariable;
      this.wildcardType = wildcardType;
    }

    @Override
    public AnnotatedTypeMirror visitTypeVariable(
        AnnotatedTypeVariable original,
        IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
      if (original == typeVariable) {
        return typeVariable;
      }
      return super.visitTypeVariable(original, originalToCopy);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard(
        AnnotatedWildcardType original,
        IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
      if (original == wildcardType) {
        return typeVariable;
      }
      return super.visitWildcard(original, originalToCopy);
    }
  }
}
