package org.checkerframework.checker.nullness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
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

import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.visitor.VisitHistory;
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
    ExecutableElement methElem = method.getElement();
    AnnotatedExecutableType declMethod = this.getAnnotatedType(methElem);

    Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<>();

    // Modify parameters
    List<AnnotatedTypeMirror> params = method.getParameterTypes();
    List<AnnotatedTypeMirror> declParams = declMethod.getParameterTypes();
    assert params.size() == declParams.size();

    for (int i = 0; i < params.size(); ++i) {
      AnnotatedTypeMirror param = params.get(i);
      AnnotatedTypeMirror subst = substituteCall(call, declParams.get(i), param);
      mappings.put(param, subst);
    }

    // Modify return type
    AnnotatedTypeMirror returnType = method.getReturnType();
    if (returnType.getKind() != TypeKind.VOID ) {
      AnnotatedTypeMirror subst = substituteCall(call, declMethod.getReturnType(), returnType);
      mappings.put(returnType, subst);
    }

    method = (AnnotatedExecutableType) AnnotatedTypeReplacer.replace(method, mappings);

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
 private AnnotatedTypeMirror substituteCall(MethodInvocationTree call, AnnotatedTypeMirror declInType, AnnotatedTypeMirror inType) {

     // System.out.println("input type: " + inType);
     AnnotatedTypeMirror outType = inType.shallowCopy();

     AnnotationMirror anno = declInType.getAnnotation(KeyFor.class);
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

     if (declInType.getKind() == TypeKind.DECLARED &&
             outType.getKind() == TypeKind.DECLARED) {
         AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) outType;
         AnnotatedDeclaredType declDeclaredType = (AnnotatedDeclaredType) declInType;
         Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

         List<AnnotatedTypeMirror> typeArgs = declaredType.getTypeArguments();
         List<AnnotatedTypeMirror> declTypeArgs = declDeclaredType.getTypeArguments();

         assert typeArgs.size() == declTypeArgs.size();

         // Get the substituted type arguments
         for (int i = 0; i < typeArgs.size(); ++i) {
             AnnotatedTypeMirror typeArgument = typeArgs.get(i);
             AnnotatedTypeMirror substTypeArgument = substituteCall(call, declTypeArgs.get(i), typeArgument);
             mapping.put(typeArgument, substTypeArgument);
         }

         outType = AnnotatedTypeReplacer.replace(declaredType, mapping);
     } else if (declInType.getKind() == TypeKind.ARRAY &
             outType.getKind() == TypeKind.ARRAY) {
         AnnotatedArrayType arrayType = (AnnotatedArrayType) outType;
         AnnotatedArrayType declArrayType = (AnnotatedArrayType) declInType;

         // Get the substituted component type
         AnnotatedTypeMirror elemType = arrayType.getComponentType();
         AnnotatedTypeMirror substElemType = substituteCall(call, declArrayType.getComponentType(), elemType);

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
      return new KeyForTypeHierarchy(checker, getQualifierHierarchy(),
                                     checker.hasOption("ignoreRawTypeArguments"),
                                     checker.hasOption("invariantArrays"));
  }

  protected class KeyForTypeHierarchy extends DefaultTypeHierarchy {

      public KeyForTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy,
                                 boolean ignoreRawTypes, boolean invariantArrayComponents) {
          super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
      }

      @Override
      public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {

          //TODO: THIS IS FROM THE OLD TYPE HIERARCHY.  WE SHOULD FIX DATA-FLOW/PROPAGATION TO DO THE RIGHT THING
          if (supertype.getKind() == TypeKind.TYPEVAR &&
              subtype.getKind() == TypeKind.TYPEVAR) {
              // TODO: Investigate whether there is a nicer and more proper way to
              // get assignments between two type variables working.
              if (supertype.getAnnotations().isEmpty()) {
                  return true;
              }
          }

          // Otherwise Covariant would cause trouble.
          if (subtype.hasAnnotation(KeyForBottom.class)) {
              return true;
          }
          return super.isSubtype(subtype, supertype, visited);
      }


      protected boolean isCovariant(final int typeArgIndex, final int[] covariantArgIndexes) {
          if(covariantArgIndexes != null) {
              for (int covariantIndex : covariantArgIndexes) {
                  if (typeArgIndex == covariantIndex) {
                      return true;
                  }
              }
          }

          return false;
      }
      @Override
      public Boolean visitTypeArgs(AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype,
                                      VisitHistory visited,  boolean subtypeIsRaw, boolean supertypeIsRaw) {
          final boolean ignoreTypeArgs = ignoreRawTypes && (subtypeIsRaw || supertypeIsRaw);

          if (!ignoreTypeArgs) {

              //TODO: Make an option for honoring this annotation in DefaultTypeHierarchy?
              final TypeElement supertypeElem = (TypeElement) supertype.getUnderlyingType().asElement();
              int[] covariantArgIndexes = null;
              if (supertypeElem.getAnnotation(Covariant.class) != null) {
                  covariantArgIndexes = supertypeElem.getAnnotation(Covariant.class).value();
              }

              final List<? extends AnnotatedTypeMirror> subtypeTypeArgs   = subtype.getTypeArguments();
              final List<? extends AnnotatedTypeMirror> supertypeTypeArgs = supertype.getTypeArguments();

              if( subtypeTypeArgs.isEmpty() || supertypeTypeArgs.isEmpty() ) {
                  return true;
              }

              if (supertypeTypeArgs.size() > 0) {
                  for (int i = 0; i < supertypeTypeArgs.size(); i++) {
                      final AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
                      final AnnotatedTypeMirror subTypeArg   = subtypeTypeArgs.get(i);

                      if(subtypeIsRaw || supertypeIsRaw) {
                          rawnessComparer.isValid(subtype, supertype, visited);
                      } else {
                          if (!isContainedBy(subTypeArg, superTypeArg, visited, isCovariant(i, covariantArgIndexes))) {
                              return false;
                          }
                      }
                  }
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
