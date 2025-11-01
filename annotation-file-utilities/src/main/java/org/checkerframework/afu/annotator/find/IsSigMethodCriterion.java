package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.FieldDescriptor;
import org.checkerframework.checker.signature.qual.MethodDescriptor;
import org.plumelib.reflection.Signatures;
import org.plumelib.util.CollectionsPlume;

/**
 * A criterion that matches a method with a specific signature (name, argument types, and return
 * type). The signature is given in JVM format.
 */
public class IsSigMethodCriterion implements Criterion {

  // The context is used for determining the fully qualified name of methods.
  private static class Context {
    public final String packageName;
    public final List<String> imports;

    public Context(String packageName, List<String> imports) {
      this.packageName = packageName;
      this.imports = imports;
    }
  }

  /** Map from compilation unit to Context. */
  private static final Map<CompilationUnitTree, Context> contextCache = new HashMap<>();

  /**
   * The JVML signature, without return type. This field is used only for diagnostics. Its
   * components appear in the following fields.
   */
  private final String signatureWithoutReturnType;

  /** The method name. */
  private final String simpleMethodName;

  /** List of parameters in Java, not JVML, format. */
  private final List<@BinaryName String> fullyQualifiedParams;

  /** Return type in Java, not JVML, format. null if return type is "void". */
  private final @Nullable @BinaryName String returnType;

  /**
   * Creates a new IsSigMethodCriterion.
   *
   * @param fullSignature the full JVML signature (that is, a method descriptor)
   */
  public IsSigMethodCriterion(@MethodDescriptor String fullSignature) {
    this.signatureWithoutReturnType = fullSignature.substring(0, fullSignature.indexOf(')') + 1);
    this.simpleMethodName = fullSignature.substring(0, fullSignature.indexOf('('));
    try {
      String jvmlArgs =
          fullSignature.substring(fullSignature.indexOf('('), fullSignature.indexOf(')') + 1);
      this.fullyQualifiedParams =
          CollectionsPlume.mapList(
              Signatures::fieldDescriptorToBinaryName, Signatures.splitJvmArglist(jvmlArgs));
    } catch (Exception e) {
      throw new RuntimeException("Exception while parsing method: " + fullSignature, e);
    }
    @Nullable @FieldDescriptor String returnTypeJvml = Signatures.methodDescriptorToReturnType(fullSignature);
    this.returnType =
        returnTypeJvml == null ? null : Signatures.fieldDescriptorToBinaryName(returnTypeJvml);
  }

  // called by isSatisfiedBy(TreePath), will get compilation unit on its own
  private static Context initImports(TreePath path) {
    CompilationUnitTree topLevel = path.getCompilationUnit();
    Context result = contextCache.get(topLevel);
    if (result != null) {
      return result;
    }

    ExpressionTree packageTree = topLevel.getPackageName();
    String packageName;
    if (packageTree == null) {
      packageName = ""; // the default package
    } else {
      packageName = packageTree.toString();
    }

    List<String> imports = new ArrayList<>();
    for (ImportTree i : topLevel.getImports()) {
      String imported = i.getQualifiedIdentifier().toString();
      imports.add(imported);
    }

    result = new Context(packageName, imports);
    contextCache.put(topLevel, result);
    return result;
  }

  // Abstracts out the inner loop of matchTypeParams.
  // goalType is fully-qualified.
  private boolean matchTypeParam(
      String goalType, Tree type, Map<String, String> typeToClassMap, Context context) {
    String simpleType = type.toString();

    boolean haveMatch = matchSimpleType(goalType, simpleType, context);
    if (!haveMatch) {
      if (!typeToClassMap.isEmpty()) {
        for (Map.Entry<String, String> p : typeToClassMap.entrySet()) {
          simpleType = simpleType.replaceAll("\\b" + p.getKey() + "\\b", p.getValue());
          haveMatch = matchSimpleType(goalType, simpleType, context);
          if (!haveMatch) {
            Criteria.dbug.debug("matchTypeParams() => false:%n");
            Criteria.dbug.debug("  type = %s%n", type);
            Criteria.dbug.debug("  simpleType = %s%n", simpleType);
            Criteria.dbug.debug("  goalType = %s%n", goalType);
          }
        }
      }
    }
    return haveMatch;
  }

  private boolean matchTypeParams(
      List<? extends VariableTree> sourceParams,
      Map<String, String> typeToClassMap,
      Context context) {
    assert sourceParams.size() == fullyQualifiedParams.size();
    for (int i = 0; i < sourceParams.size(); i++) {
      String fullType = fullyQualifiedParams.get(i);
      VariableTree vt = sourceParams.get(i);
      Tree vtType = vt.getType();
      if (!matchTypeParam(fullType, vtType, typeToClassMap, context)) {
        Criteria.dbug.debug(
            "matchTypeParam() => false:%n  i=%d vt = %s%n  fullType = %s%n", i, vt, fullType);
        return false;
      }
    }
    return true;
  }

  // simpleType is the name as it appeared in the source code.
  // fullType is fully-qualified.
  // Both are in Java, not JVML, format.
  private boolean matchSimpleType(String fullType, String simpleType, Context context) {
    Criteria.dbug.debug("matchSimpleType(%s, %s, %s)%n", fullType, simpleType, context);

    // must strip off generics, is all of this necessary, though?
    // do you ever have generics anywhere but at the end?
    while (simpleType.contains("<")) {
      int bracketIndex = simpleType.lastIndexOf('<');
      String beforeBracket = simpleType.substring(0, bracketIndex);
      String afterBracket = simpleType.substring(simpleType.indexOf(">", bracketIndex) + 1);
      simpleType = beforeBracket + afterBracket;
    }

    // TODO: arrays?

    // first try qualifying simpleType with this package name,
    // then with java.lang
    // then with default package
    // then with all of the imports

    boolean matchable = false;

    if (!matchable) {
      // match with this package name
      String packagePrefix = context.packageName;
      if (packagePrefix.length() > 0) {
        packagePrefix = packagePrefix + ".";
      }
      if (matchWithPrefix(fullType, simpleType, packagePrefix)) {
        matchable = true;
      }
    }

    if (!matchable) {
      // match with java.lang
      if (matchWithPrefix(fullType, simpleType, "java.lang.")) {
        matchable = true;
      }
    }

    if (!matchable) {
      // match with default package
      if (matchWithPrefix(fullType, simpleType, "")) {
        matchable = true;
      }
    }

    // From Java 7 language definition 6.5.5.2 (Qualified Types):
    // If a type name is of the form Q.Id, then Q must be either a type
    // name or a package name.  If Id names exactly one accessible type
    // that is a member of the type or package denoted by Q, then the
    // qualified type name denotes that type.
    if (!matchable) {
      // match with any of the imports
      for (String someImport : context.imports) {
        String importPrefix = null;
        if (someImport.contains("*")) {
          // don't include the * in the prefix, should end in .
          // TODO: this is a real bug due to nonnull, though I discovered it manually
          // importPrefix = someImport.substring(0, importPrefix.indexOf('*'));
          importPrefix = someImport.substring(0, someImport.indexOf('*'));
        } else {
          // if you imported a specific class, you can only use that import
          // if the last part matches the simple type
          String importSimpleType = someImport.substring(someImport.lastIndexOf('.') + 1);

          // Remove array brackets from simpleType if it has them
          int arrayBracket = simpleType.indexOf('[');
          String simpleBaseType = simpleType;
          if (arrayBracket > -1) {
            simpleBaseType = simpleType.substring(0, arrayBracket);
          }
          if (!(simpleBaseType.equals(importSimpleType)
              || simpleBaseType.startsWith(importSimpleType + "."))) {
            continue;
          }

          importPrefix = someImport.substring(0, someImport.lastIndexOf('.') + 1);
        }

        if (matchWithPrefix(fullType, simpleType, importPrefix)) {
          matchable = true;
          break; // out of for loop
        }
      }
    }

    return matchable;
  }

  private boolean matchWithPrefix(String fullType, String simpleType, String prefix) {
    return matchWithPrefixOneWay(fullType, simpleType, prefix)
        || matchWithPrefixOneWay(simpleType, fullType, prefix);
  }

  // simpleType can be in JVML format ??  Is that really possible?
  private boolean matchWithPrefixOneWay(String fullType, String simpleType, String prefix) {

    // maybe simpleType is in JVML format
    String simpleType2 = simpleType.replace("/", ".");

    String fullType2 = fullType.replace("$", ".");

    /* unused String prefix2 = (prefix.endsWith(".")
    ? prefix.substring(0, prefix.length() - 1)
    : prefix); */
    boolean b =
        (fullType2.equals(prefix + simpleType2)
            // Hacky way to handle the possibility that fulltype is an
            // inner type but simple type is unqualified.
            || (fullType.startsWith(prefix)
                && (fullType.endsWith("$" + simpleType2)
                    || fullType2.endsWith("." + simpleType2))));
    Criteria.dbug.debug("matchWithPrefix(%s, %s, %s) => %b)%n", fullType2, simpleType, prefix, b);
    return b;
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path, @FindDistinct Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path) {
    if (path == null) {
      return false;
    }

    Context context = initImports(path);

    Tree leaf = path.getLeaf();

    if (!(leaf instanceof MethodTree)) {
      Criteria.dbug.debug(
          "IsSigMethodCriterion.isSatisfiedBy(%s) => false: not a METHOD tree%n",
          Main.leafString(path));
      return false;
    }
    // else if ((((JCMethodDecl) leaf).mods.flags & Flags.GENERATEDCONSTR) != 0) {
    //  Criteria.dbug.debug(
    //      "IsSigMethodCriterion.isSatisfiedBy(%s) => false: generated constructor%n",
    //      Main.leafString(path));
    //  return false;
    // }

    MethodTree mt = (MethodTree) leaf;

    if (!simpleMethodName.equals(mt.getName().toString())) {
      Criteria.dbug.debug("IsSigMethodCriterion.isSatisfiedBy => false: Names don't match%n");
      return false;
    }

    List<? extends VariableTree> sourceParams = mt.getParameters();
    if (fullyQualifiedParams.size() != sourceParams.size()) {
      Criteria.dbug.debug(
          "IsSigMethodCriterion.isSatisfiedBy => false: Number of parameters don't match%n");
      return false;
    }

    // now go through all type parameters declared by method
    // and for each one, create a mapping from the type to the
    // first declared extended class, defaulting to Object
    // for example,
    // <T extends Date> void foo(T t)
    //  creates mapping: T -> Date
    // <T extends Date & List> void foo(Object o)
    //  creates mapping: T -> Date
    // <T extends Date, U extends List> foo(Object o)
    //  creates mappings: T -> Date, U -> List
    // <T> void foo(T t)
    //  creates mapping: T -> Object

    Map<String, String> typeToClassMap = new HashMap<>();
    for (TypeParameterTree param : mt.getTypeParameters()) {
      String paramName = param.getName().toString();
      String paramClass = "Object";
      List<? extends Tree> paramBounds = param.getBounds();
      if (paramBounds != null && !paramBounds.isEmpty()) {
        Tree boundZero = paramBounds.get(0);
        if (boundZero instanceof AnnotatedTypeTree) {
          boundZero = ((AnnotatedTypeTree) boundZero).getUnderlyingType();
        }
        paramClass = boundZero.toString();
      }
      typeToClassMap.put(paramName, paramClass);
    }

    // Do the same for the enclosing class.
    // The type variable might not be from the directly enclosing
    // class, but from a further up class.
    // Go through all enclosing classes and add the type parameters.
    {
      TreePath classpath = path;
      ClassTree ct = enclosingClass(classpath);
      while (ct != null) {
        for (TypeParameterTree param : ct.getTypeParameters()) {
          String paramName = param.getName().toString();
          String paramClass = "Object";
          List<? extends Tree> paramBounds = param.getBounds();
          if (paramBounds != null && !paramBounds.isEmpty()) {
            Tree pb = paramBounds.get(0);
            if (pb instanceof AnnotatedTypeTree) {
              pb = ((AnnotatedTypeTree) pb).getUnderlyingType();
            }
            paramClass = pb.toString();
          }
          typeToClassMap.put(paramName, paramClass);
        }
        classpath = classpath.getParentPath();
        ct = enclosingClass(classpath);
      }
    }

    if (!matchTypeParams(sourceParams, typeToClassMap, context)) {
      Criteria.dbug.debug("IsSigMethodCriterion => false: Parameter types don't match%n");
      return false;
    }

    if (mt.getReturnType() != null // must be a constructor
        && returnType != null
        && !matchTypeParam(returnType, mt.getReturnType(), typeToClassMap, context)) {
      Criteria.dbug.debug("IsSigMethodCriterion => false: Return types don't match%n");
      return false;
    }

    Criteria.dbug.debug("IsSigMethodCriterion.isSatisfiedBy => true%n");
    return true;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  /* This is a copy of the method from the Checker Framework
   * TreeUtils.enclosingClass.
   * We cannot have a dependency on the Checker Framework.
   * TODO: as is the case there, anonymous classes are not handled correctly.
   */
  private static ClassTree enclosingClass(final TreePath path) {
    final Set<Tree.Kind> kinds =
        EnumSet.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
    TreePath p = path;

    while (p != null) {
      Tree leaf = p.getLeaf();
      assert leaf != null; /*nninvariant*/
      if (kinds.contains(leaf.getKind())) {
        return (ClassTree) leaf;
      }
      p = p.getParentPath();
    }

    return null;
  }

  @Override
  public Kind getKind() {
    return Kind.SIG_METHOD;
  }

  @Override
  public String toString() {
    return "IsSigMethodCriterion: " + signatureWithoutReturnType;
  }
}
