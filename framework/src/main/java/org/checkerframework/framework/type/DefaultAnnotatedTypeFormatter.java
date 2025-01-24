package org.checkerframework.framework.type;

import com.sun.tools.javac.code.Type;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.checkerframework.javacutil.AnnotationFormatter;
import org.checkerframework.javacutil.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.WeakIdentityHashMap;

/**
 * An AnnotatedTypeFormatter used by default by all AnnotatedTypeFactory (and therefore all
 * annotated types).
 *
 * @see org.checkerframework.framework.type.AnnotatedTypeFormatter
 * @see org.checkerframework.framework.type.AnnotatedTypeMirror#toString
 */
public class DefaultAnnotatedTypeFormatter implements AnnotatedTypeFormatter {

  /** The formatting visitor. */
  protected final FormattingVisitor formattingVisitor;

  /**
   * Constructs a DefaultAnnotatedTypeFormatter that does not print invisible annotations by
   * default.
   */
  public DefaultAnnotatedTypeFormatter() {
    this(new DefaultAnnotationFormatter(), true, false);
  }

  /**
   * @param printVerboseGenerics for type parameters, their uses, and wildcards, print more
   *     information
   * @param defaultPrintInvisibleAnnos whether or not this AnnotatedTypeFormatter should print
   *     invisible annotations
   */
  public DefaultAnnotatedTypeFormatter(
      boolean printVerboseGenerics, boolean defaultPrintInvisibleAnnos) {
    this(new DefaultAnnotationFormatter(), printVerboseGenerics, defaultPrintInvisibleAnnos);
  }

  /**
   * @param formatter an object that converts annotation mirrors to strings
   * @param printVerboseGenerics for type parameters, their uses, and wildcards, print more
   *     information
   * @param defaultPrintInvisibleAnnos whether or not this AnnotatedTypeFormatter should print
   *     invisible annotations
   */
  public DefaultAnnotatedTypeFormatter(
      AnnotationFormatter formatter,
      boolean printVerboseGenerics,
      boolean defaultPrintInvisibleAnnos) {
    this(new FormattingVisitor(formatter, printVerboseGenerics, defaultPrintInvisibleAnnos));
  }

  /**
   * Used by subclasses and other constructors to specify the underlying implementation of this
   * DefaultAnnotatedTypeFormatter.
   */
  protected DefaultAnnotatedTypeFormatter(FormattingVisitor visitor) {
    this.formattingVisitor = visitor;
  }

  /**
   * Maps from type variables to deterministic IDs. This is useful for comparing output across two
   * runs of the Checker Framework.
   *
   * <p>This map is necessary for deterministic and informative output. javac might print two
   * distinct capture-converted variables as "capture#222" if the second is created after the first
   * is garbage-collected, or if they just happen to have the same hash code based on memory layout.
   * Such javac output is misleading because it looks like the two printed representations refer to
   * the same variable.
   *
   * <p>This map contains type variables that have been formatted. Therefore, the numbers may differ
   * between Checker Framework runs if the different runs print different values (say, one of them
   * prints more type variables than the other).
   */
  protected static final Map<TypeVariable, Integer> captureConversionIds =
      new WeakIdentityHashMap<>();

  /** The last deterministic capture conversion ID that was used. */
  protected static int prevCaptureConversionId = 0;

  /**
   * Returns a deterministic capture conversion ID for the given javac captured type.
   *
   * @param capturedType a type variable, which must be a capture-converted type variable
   * @return a deterministic capture conversion ID
   */
  static int getCaptureConversionId(TypeVariable capturedType) {
    return captureConversionIds.computeIfAbsent(capturedType, key -> ++prevCaptureConversionId);
  }

  @Override
  public String format(AnnotatedTypeMirror type) {
    formattingVisitor.resetPrintVerboseSettings();
    return formattingVisitor.visit(type);
  }

  @Override
  public String format(AnnotatedTypeMirror type, boolean printVerbose) {
    formattingVisitor.setVerboseSettings(printVerbose);
    return formattingVisitor.visit(type);
  }

  /** A scanning visitor that prints the entire AnnotatedTypeMirror passed to visit. */
  protected static class FormattingVisitor
      implements AnnotatedTypeVisitor<String, Set<AnnotatedTypeMirror>> {

    /** The object responsible for converting annotations to strings. */
    protected final AnnotationFormatter annoFormatter;

    /**
     * Represents whether or not invisible annotations should be printed if the client of this class
     * does not use the printInvisibleAnnos parameter.
     */
    protected final boolean defaultInvisiblesSetting;

    /**
     * For a given call to format, this setting specifies whether or not to printInvisibles. If a
     * user did not specify a printInvisible parameter in the call to format then this value will
     * equal DefaultAnnotatedTypeFormatter.defaultInvisibleSettings for this object
     */
    protected boolean currentPrintInvisibleSetting;

    /** Default value of currentPrintVerboseGenerics. */
    protected final boolean defaultPrintVerboseGenerics;

    /**
     * Prints type variables in a less ambiguous manner using [] to delimit them. Always prints both
     * bounds even if they lower bound is an AnnotatedNull type.
     */
    protected boolean currentPrintVerboseGenerics;

    /** Whether the visitor is currently printing a raw type. */
    protected boolean currentlyPrintingRaw;

    /**
     * Creates the visitor.
     *
     * @param annoFormatter formatter used for {@code AnnotationMirror}s
     * @param printVerboseGenerics whether to verbosely print type variables and wildcards
     * @param defaultInvisiblesSetting whether to print invisible qualifiers
     */
    public FormattingVisitor(
        AnnotationFormatter annoFormatter,
        boolean printVerboseGenerics,
        boolean defaultInvisiblesSetting) {
      this.annoFormatter = annoFormatter;
      this.defaultPrintVerboseGenerics = printVerboseGenerics;
      this.currentPrintVerboseGenerics = printVerboseGenerics;
      this.defaultInvisiblesSetting = defaultInvisiblesSetting;
      this.currentPrintInvisibleSetting = false;
      this.currentlyPrintingRaw = false;
    }

    /** Set the current verbose settings to use while printing. */
    protected void setVerboseSettings(boolean printVerbose) {
      this.currentPrintInvisibleSetting = printVerbose;
      this.currentPrintVerboseGenerics = printVerbose;
    }

    /** Set verbose settings to the default. */
    protected void resetPrintVerboseSettings() {
      this.currentPrintInvisibleSetting = defaultInvisiblesSetting;
      this.currentPrintVerboseGenerics = defaultPrintVerboseGenerics;
    }

    /**
     * Print, to sb, {@code keyWord} followed by {@code field}. NULL types are substituted with
     * their annotations followed by " Void"
     */
    @SideEffectFree
    protected void printBound(
        String keyWord,
        AnnotatedTypeMirror field,
        Set<AnnotatedTypeMirror> visiting,
        StringBuilder sb) {
      if (!currentPrintVerboseGenerics && (field == null || field.getKind() == TypeKind.NULL)) {
        return;
      }

      sb.append(" ");
      sb.append(keyWord);
      sb.append(" ");

      if (field == null) {
        sb.append("<null>");
      } else if (field.getKind() != TypeKind.NULL) {
        sb.append(visit(field, visiting));
      } else {
        sb.append(
            annoFormatter.formatAnnotationString(
                field.getPrimaryAnnotations(), currentPrintInvisibleSetting));
        sb.append("Void");
      }
    }

    @SideEffectFree
    @Override
    public String visit(AnnotatedTypeMirror type) {
      return type.accept(this, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Override
    public String visit(AnnotatedTypeMirror type, Set<AnnotatedTypeMirror> annotatedTypeVariables) {
      return type.accept(this, annotatedTypeVariables);
    }

    @Override
    public String visitDeclared(AnnotatedDeclaredType type, Set<AnnotatedTypeMirror> visiting) {
      StringBuilder sb = new StringBuilder();
      if (type.isDeclaration() && currentPrintInvisibleSetting) {
        sb.append("/*DECL*/ ");
      }

      if (type.enclosingType != null) {
        sb.append(this.visit(type.enclosingType, visiting));
        sb.append('.');
      }
      Element typeElt = type.getUnderlyingType().asElement();
      String smpl = typeElt.getSimpleName().toString();
      if (smpl.isEmpty()) {
        // For anonymous classes smpl is empty - toString
        // of the element is more useful.
        smpl = typeElt.toString();
      }
      sb.append(
          annoFormatter.formatAnnotationString(
              type.getPrimaryAnnotations(), currentPrintInvisibleSetting));
      sb.append(smpl);

      boolean oldPrintingRaw = currentlyPrintingRaw;
      if (type.isUnderlyingTypeRaw()) {
        currentlyPrintingRaw = true;
      }
      if (type.typeArgs != null) {
        // getTypeArguments sets the field if it does not already exist.
        List<AnnotatedTypeMirror> typeArgs = type.typeArgs;
        if (!typeArgs.isEmpty()) {
          StringJoiner sj = new StringJoiner(", ", "<", ">");
          if (!currentPrintVerboseGenerics && currentlyPrintingRaw) {
            sj.add("/*RAW*/");
          } else {
            for (AnnotatedTypeMirror typeArg : typeArgs) {
              sj.add(visit(typeArg, visiting));
            }
          }
          sb.append(sj);
        }
      } else {
        sb.append("<" + "/*Type args not initialized*/" + ">");
      }
      currentlyPrintingRaw = oldPrintingRaw;
      return sb.toString();
    }

    @Override
    public String visitIntersection(
        AnnotatedIntersectionType type, Set<AnnotatedTypeMirror> visiting) {
      if (type.bounds == null) {
        return "/*Intersection not initialized*/";
      }

      StringBuilder sb = new StringBuilder();

      boolean isFirst = true;
      for (AnnotatedTypeMirror bound : type.getBounds()) {
        if (!isFirst) {
          sb.append(" & ");
        }
        sb.append(visit(bound, visiting));
        isFirst = false;
      }
      return sb.toString();
    }

    @Override
    public String visitUnion(AnnotatedUnionType type, Set<AnnotatedTypeMirror> visiting) {
      if (type.alternatives == null) {
        return "/*Union not initialized*/";
      }

      StringBuilder sb = new StringBuilder();

      boolean isFirst = true;
      for (AnnotatedDeclaredType adt : type.getAlternatives()) {
        if (!isFirst) {
          sb.append(" | ");
        }
        sb.append(visit(adt, visiting));
        isFirst = false;
      }
      return sb.toString();
    }

    @Override
    public String visitExecutable(AnnotatedExecutableType type, Set<AnnotatedTypeMirror> visiting) {
      StringBuilder sb = new StringBuilder();
      if (type.typeVarTypes == null || !type.typeVarTypes.isEmpty()) {
        StringJoiner sj = new StringJoiner(", ", "<", "> ");
        if (type.typeVarTypes == null) {
          sj.add("/*Type var not initialized*/");
        } else {
          for (AnnotatedTypeVariable atv : type.getTypeVariables()) {
            sj.add(visit(atv, visiting));
          }
        }
        sb.append(sj);
      }
      if (type.returnType != null) {
        sb.append(visit(type.getReturnType(), visiting));
      } else {
        sb.append("/*Return type not initialized*/");
      }
      sb.append(' ');
      if (type.getElement() != null) {
        sb.append(type.getElement().getSimpleName());
      } else {
        sb.append("METHOD");
      }
      sb.append('(');
      AnnotatedDeclaredType rcv = type.receiverType;
      if (rcv != null) {
        sb.append(visit(rcv, visiting));
        sb.append(" this");
      }
      if (type.paramTypes == null) {
        sb.append("/*Parameters not initialized*/");
      } else if (!type.paramTypes.isEmpty()) {
        int p = 0;
        for (AnnotatedTypeMirror atm : type.paramTypes) {
          if (rcv != null || p > 0) {
            sb.append(", ");
          }
          sb.append(visit(atm, visiting));
          // Output some parameter names to make it look more like a method.
          // TODO: go to the element and look up real parameter names, maybe.
          sb.append(" p");
          sb.append(p++);
        }
      }
      sb.append(')');
      if (type.thrownTypes == null) {
        sb.append("/*Throws not initialized*/");
      } else if (!type.thrownTypes.isEmpty()) {
        sb.append(" throws ");
        for (AnnotatedTypeMirror atm : type.getThrownTypes()) {
          sb.append(visit(atm, visiting));
        }
      }
      return sb.toString();
    }

    @Override
    public String visitArray(AnnotatedArrayType type, Set<AnnotatedTypeMirror> visiting) {
      StringBuilder sb = new StringBuilder();

      AnnotatedArrayType array = type;
      AnnotatedTypeMirror component;
      while (true) {
        component = array.componentType;
        if (!array.getPrimaryAnnotations().isEmpty()) {
          sb.append(' ');
          sb.append(
              annoFormatter.formatAnnotationString(
                  array.getPrimaryAnnotations(), currentPrintInvisibleSetting));
        }
        sb.append("[]");
        if (!(component instanceof AnnotatedArrayType)) {
          if (component == null) {
            sb.insert(0, "/*Not Initialized*/");
          } else {
            sb.insert(0, visit(component, visiting));
          }
          break;
        }
        array = (AnnotatedArrayType) component;
      }
      return sb.toString();
    }

    @Override
    public String visitTypeVariable(AnnotatedTypeVariable type, Set<AnnotatedTypeMirror> visiting) {
      StringBuilder sb = new StringBuilder();
      if (TypesUtils.isCapturedTypeVariable(type.underlyingType)) {
        // underlyingType.toString() has this form: "capture#826 of ? extends
        // java.lang.Object".
        //   assert underlyingType.toString().startsWith("capture#");
        // We output only the "capture#826" part.

        // TODO: If deterministic output is not needed, we could avoid the use of
        // getCaptureConversionId() by using this code instead:
        //   sb.append(underlyingType, 0, underlyingType.indexOf(" of "));
        // The choice would be controlled by a command-line argument.

        // We output a deterministic number; we prefix it by "0"
        // so we know whether a number is deterministic or from javac.
        sb.append("capture#0").append(getCaptureConversionId((TypeVariable) type.underlyingType));
      } else {
        sb.append(type.underlyingType);
      }

      if (!visiting.contains(type)) {
        if (type.isDeclaration() && currentPrintInvisibleSetting) {
          sb.append("/*DECL*/ ");
        }

        try {
          visiting.add(type);
          if (currentPrintVerboseGenerics) {
            sb.append("[");
          }
          printBound("extends", type.getUpperBoundField(), visiting, sb);
          printBound("super", type.getLowerBoundField(), visiting, sb);
          if (currentPrintVerboseGenerics) {
            sb.append("]");
          }

        } finally {
          visiting.remove(type);
        }
      }
      return sb.toString();
    }

    @SideEffectFree
    @Override
    public String visitPrimitive(AnnotatedPrimitiveType type, Set<AnnotatedTypeMirror> visiting) {
      return formatFlatType(type);
    }

    @SideEffectFree
    @Override
    public String visitNoType(AnnotatedNoType type, Set<AnnotatedTypeMirror> visiting) {
      return formatFlatType(type);
    }

    @SideEffectFree
    @Override
    public String visitNull(AnnotatedNullType type, Set<AnnotatedTypeMirror> visiting) {
      return annoFormatter.formatAnnotationString(
              type.getPrimaryAnnotations(), currentPrintInvisibleSetting)
          + "NullType";
    }

    @Override
    public String visitWildcard(AnnotatedWildcardType type, Set<AnnotatedTypeMirror> visiting) {
      StringBuilder sb = new StringBuilder();
      if (type.isTypeArgOfRawType()) {
        if (currentlyPrintingRaw) {
          sb.append("/*RAW TYPE ARGUMENT:*/ ");
        } else {
          sb.append("/*INFERENCE FAILED for:*/ ");
        }
      }

      sb.append(
          annoFormatter.formatAnnotationString(
              type.getPrimaryAnnotationsField(), currentPrintInvisibleSetting));

      sb.append("?");
      if (!visiting.contains(type)) {

        try {
          visiting.add(type);

          if (currentPrintVerboseGenerics) {
            sb.append("[");
          }
          printBound("extends", type.getExtendsBoundField(), visiting, sb);
          printBound("super", type.getSuperBoundField(), visiting, sb);
          if (currentPrintVerboseGenerics) {
            sb.append("]");
          }

        } finally {
          visiting.remove(type);
        }
      }
      return sb.toString();
    }

    @SideEffectFree
    protected String formatFlatType(AnnotatedTypeMirror flatType) {
      return annoFormatter.formatAnnotationString(
              flatType.getPrimaryAnnotations(), currentPrintInvisibleSetting)
          + TypeAnnotationUtils.unannotatedType((Type) flatType.getUnderlyingType());
    }
  }
}
