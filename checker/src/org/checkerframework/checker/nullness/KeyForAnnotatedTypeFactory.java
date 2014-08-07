package org.checkerframework.checker.nullness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import org.checkerframework.checker.nullness.qual.Covariant;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

@TypeQualifiers({ KeyFor.class, UnknownKeyFor.class, KeyForBottom.class})
public class KeyForAnnotatedTypeFactory extends
    GenericAnnotatedTypeFactory<CFValue, CFStore, KeyForTransfer, KeyForAnalysis> {

    protected final AnnotationMirror UNKNOWN, KEYFOR;

    public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        KEYFOR = AnnotationUtils.fromClass(elements, KeyFor.class);
        UNKNOWN = AnnotationUtils.fromClass(elements, UnknownKeyFor.class);

        this.postInit();

        this.defaults.addAbsoluteDefault(UNKNOWN, DefaultLocation.ALL);

        // Add compatibility annotations:
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForDecl.class, KEYFOR);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForType.class, KEYFOR);
    }

  /* TODO: we currently do not substitute field types.
   * postAsMemberOf only gives us the type of the receiver expression ("owner"),
   * but not the Tree. Therefore, we could not decide the substitution.
   * I think it shouldn't happen frequently to have a field
   * with annotation @KeyFor("this").
   * However, one field being marked as the key for a different field might
   * be necessary, so changing a @KeyFor("map") into @KeyFor("recv.map")
   * might be necessary.
  @Override
  protected void postAsMemberOf(AnnotatedTypeMirror type,
      AnnotatedTypeMirror owner, Element element) {
  }
    */

  // TODO
  /* Once the method substitution is stable, create
   * substituteNewClass. Look whether they can share code somehow
   * (the two classes don't share a common interface).
  @Override
  public AnnotatedExecutableType constructorFromUse(NewClassTree call) {
    assert call != null;

    AnnotatedExecutableType constructor = super.constructorFromUse(call);

    Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

    // Get the result type
    AnnotatedTypeMirror resultType = getAnnotatedType(call);

    // Modify parameters
    for (AnnotatedTypeMirror parameterType : constructor.getParameterTypes()) {
      AnnotatedTypeMirror combinedType = substituteNewClass(call, parameterType);
      mappings.put(parameterType, combinedType);
    }

    // TODO: upper bounds, throws?

    constructor = constructor.substitute(mappings);

    return constructor;
  }
  */

  // TODO: doc
  @Override
  public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree call) {
    assert call != null;
    // System.out.println("looking at call: " + call);
    Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(call);
    AnnotatedExecutableType method = mfuPair.first;

    Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

    // Modify parameters
    List<AnnotatedTypeMirror> params = method.getParameterTypes();
    for (AnnotatedTypeMirror param : params) {
      AnnotatedTypeMirror subst = substituteCall(call, param);
      mappings.put(param, subst);
    }

    // Modify return type
    AnnotatedTypeMirror returnType = method.getReturnType();
    if (returnType.getKind() != TypeKind.VOID ) {
      AnnotatedTypeMirror subst = substituteCall(call, returnType);
      mappings.put(returnType, subst);
    }

    // TODO: upper bounds, throws?

    method = (AnnotatedExecutableType)method.substitute(mappings);

    // System.out.println("adapted method: " + method);

    return Pair.of(method, mfuPair.second);
  }


  /* TODO: doc
   * This pattern and the logic how to use it is copied from NullnessFlow.
   * NullnessFlow already contains four exact copies of the logic for handling this
   * pattern and should really be refactored.
   */
  private static final Pattern parameterPtn = Pattern.compile("#(\\d+)");

  // TODO: copied from NullnessFlow, but without the "." at the end.
  private String receiver(MethodInvocationTree node) {
    ExpressionTree sel = node.getMethodSelect();
    if (sel.getKind() == Tree.Kind.IDENTIFIER)
      return "";
    else if (sel.getKind() == Tree.Kind.MEMBER_SELECT)
      return ((MemberSelectTree)sel).getExpression().toString();
    ErrorReporter.errorAbort("KeyForAnnotatedTypeFactory.receiver: cannot be here");
    return null; // dead code
  }

  // TODO: doc
  // TODO: "this" should be implicitly prepended
  // TODO: substitutions also need to be applied to argument types
  private AnnotatedTypeMirror substituteCall(MethodInvocationTree call, AnnotatedTypeMirror inType) {

    // System.out.println("input type: " + inType);
    AnnotatedTypeMirror outType = inType.getCopy(true);

    AnnotationMirror anno = inType.getAnnotation(KeyFor.class);
    if (anno != null) {

      List<String> inMaps = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
      List<String> outMaps = new ArrayList<String>();

      String receiver = receiver(call);

      for (String inMapName : inMaps) {
        if (parameterPtn.matcher(inMapName).matches()) {
          int param = Integer.valueOf(inMapName.substring(1));
          if (param <= 0 || param > call.getArguments().size()) {
            // The failure should already have been reported, when the
            // method declaration was processed.
            // checker.report(Result.failure("param.index.nullness.parse.error", inMapName), call);
          } else {
            String res = call.getArguments().get(param-1).toString();
            outMaps.add(res);
          }
        } else if (inMapName.equals("this")) {
          outMaps.add(receiver);
        } else {
          // TODO: look at the code below, copied from NullnessFlow
          // System.out.println("KeyFor argument unhandled: " + inMapName + " using " + receiver + "." + inMapName);
          // do not always add the receiver, e.g. for local variables this creates a mess
          // outMaps.add(receiver + "." + inMapName);
          // just copy name for now, better than doing nothing
          outMaps.add(inMapName);
        }
        // TODO: look at code in NullnessFlow and decide whether there
        // are more cases to copy.
      }

      AnnotationBuilder builder = new AnnotationBuilder(processingEnv, KeyFor.class);
      builder.setValue("value", outMaps);
      AnnotationMirror newAnno =  builder.build();

      outType.removeAnnotation(KeyFor.class);
      outType.addAnnotation(newAnno);
    }

    if (outType.getKind() == TypeKind.DECLARED) {
      AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) outType;
      Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

      // Get the substituted type arguments
      for (AnnotatedTypeMirror typeArgument : declaredType.getTypeArguments()) {
        AnnotatedTypeMirror substTypeArgument = substituteCall(call, typeArgument);
        mapping.put(typeArgument, substTypeArgument);
      }

      outType = declaredType.substitute(mapping);
    } else if (outType.getKind() == TypeKind.ARRAY) {
      AnnotatedArrayType  arrayType = (AnnotatedArrayType) outType;

      // Get the substituted component type
      AnnotatedTypeMirror elemType = arrayType.getComponentType();
      AnnotatedTypeMirror substElemType = substituteCall(call, elemType);

      arrayType.setComponentType(substElemType);
      // outType aliases arrayType
    } else if(outType.getKind().isPrimitive() ||
              outType.getKind() == TypeKind.WILDCARD ||
              outType.getKind() == TypeKind.TYPEVAR) {
      // TODO: for which of these should we also recursively substitute?
      // System.out.println("KeyForATF: Intentionally unhandled Kind: " + outType.getKind());
    } else {
      // System.err.println("KeyForATF: Unknown getKind(): " + outType.getKind());
      // assert false;
    }

    // System.out.println("result type: " + outType);
    return outType;
  }

  @Override
  protected TypeHierarchy createTypeHierarchy() {
      return new KeyForTypeHierarchy(checker, getQualifierHierarchy());
  }

  private class KeyForTypeHierarchy extends TypeHierarchy {

      public KeyForTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy) {
          super(checker, qualifierHierarchy);
      }

      @Override
      public final boolean isSubtype(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
          if (lhs.getKind() == TypeKind.TYPEVAR &&
                  rhs.getKind() == TypeKind.TYPEVAR) {
              // TODO: Investigate whether there is a nicer and more proper way to
              // get assignments between two type variables working.
              if (lhs.getAnnotations().isEmpty()) {
                  return true;
              }
          }
          // Otherwise Covariant would cause trouble.
          if (rhs.hasAnnotation(KeyForBottom.class)) {
              return true;
          }
          return super.isSubtype(rhs, lhs);
      }

      @Override
      protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType rhs, AnnotatedDeclaredType lhs) {
          if (ignoreRawTypeArguments(rhs, lhs)) {
              return true;
          }

          List<AnnotatedTypeMirror> rhsTypeArgs = rhs.getTypeArguments();
          List<AnnotatedTypeMirror> lhsTypeArgs = lhs.getTypeArguments();

          if (rhsTypeArgs.isEmpty() || lhsTypeArgs.isEmpty())
              return true;

          TypeElement lhsElem = (TypeElement) lhs.getUnderlyingType().asElement();
          // TypeElement rhsElem = (TypeElement) lhs.getUnderlyingType().asElement();
          // the following would be needed if Covariant were per type parameter
          // AnnotatedDeclaredType lhsDecl = currentATF.fromElement(lhsElem);
          // AnnotatedDeclaredType rhsDecl = currentATF.fromElement(rhsElem);
          // List<AnnotatedTypeMirror> lhsTVs = lhsDecl.getTypeArguments();
          // List<AnnotatedTypeMirror> rhsTVs = rhsDecl.getTypeArguments();

          // TODO: implementation of @Covariant should be done in the standard TypeHierarchy
          int[] covarVals = null;
          if (lhsElem.getAnnotation(Covariant.class) != null) {
              covarVals = lhsElem.getAnnotation(Covariant.class).value();
          }


          if (lhsTypeArgs.size() != rhsTypeArgs.size()) {
              // This test fails e.g. for casts from a type with one type
              // argument to a type with two type arguments.
              // See test case nullness/generics/GenericsCasts
              // TODO: shouldn't the type be brought to a common type before
              // this?
              return true;
          }

          for (int i = 0; i < lhsTypeArgs.size(); ++i) {
              boolean covar = false;
              if (covarVals != null) {
                  for (int cvv = 0; cvv < covarVals.length; ++cvv) {
                      if (covarVals[cvv] == i) {
                          covar = true;
                      }
                  }
              }

              if (covar) {
                  if (!isSubtype(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                      // TODO: still check whether isSubtypeAsTypeArgument returns true.
                      // This handles wildcards better.
                      return isSubtypeAsTypeArgument(rhsTypeArgs.get(i), lhsTypeArgs.get(i));
              } else {
                  if (!isSubtypeAsTypeArgument(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                      return false;
              }
          }

          return true;
      }
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
      return new KeyForQualifierHierarchy(factory);
  }

  private final class KeyForQualifierHierarchy extends GraphQualifierHierarchy {

      public KeyForQualifierHierarchy(MultiGraphFactory factory) {
          super(factory, null);
      }

      @Override
      public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
          if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR) &&
                  AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
              // If they are both KeyFor annotations, they have to be equal.
              // TODO: or one a subset of the maps of the other? Ordering of maps?
              return AnnotationUtils.areSame(lhs, rhs);
          }
          // Ignore annotation values to ensure that annotation is in supertype map.
          if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR)) {
              lhs = KEYFOR;
          }
          if (AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
              rhs = KEYFOR;
          }
          return super.isSubtype(rhs, lhs);
      }
  }


}
