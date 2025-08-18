package org.checkerframework.afu.scenelib.io;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Name;
import org.checkerframework.afu.annotator.find.CaseUtils;
import org.checkerframework.afu.scenelib.util.JVMNames;
import org.checkerframework.afu.scenelib.util.coll.WrapperMap;

/** Cache of {@code ASTPath} data for the nodes of a compilation unit tree. */
public class ASTIndex extends WrapperMap<Tree, ASTRecord> {
  // single-item cache
  private static Tree cachedRoot = null;
  private static Map<Tree, ASTRecord> cachedIndex = null;
  private static final int EXPECTED_SIZE = 128;

  private final CompilationUnitTree cut;
  private final Map<String, Map<String, List<String>>> formals;

  /**
   * Maps source trees in compilation unit to corresponding AST paths.
   *
   * @param root compilation unit to be indexed
   * @return map of trees in compilation unit to AST paths
   */
  public static Map<Tree, ASTRecord> indexOf(CompilationUnitTree root) {
    if (cachedRoot == null || !cachedRoot.equals(root)) {
      cachedRoot = root;
      cachedIndex = new ASTIndex(root);
    }
    return cachedIndex;
  }

  private ASTIndex(CompilationUnitTree root) {
    super(HashBiMap.<Tree, ASTRecord>create(EXPECTED_SIZE));
    cut = root;
    formals = new HashMap<>();

    // The visitor implementation is slightly complicated by the
    // inclusion of information from both parent and child nodes in each
    // ASTEntry.  The pattern for most node types is to call save() and
    // saveAll() as needed to handle the node's descendants and finally
    // to invoke defaultAction() to save the entry for the current node.
    // (If the JVM could take advantage of tail recursion, it would be
    // better to save the current node's entry first, at a small cost to
    // the clarity of the code.)
    cut.accept(
        new SimpleTreeVisitor<Void, ASTRecord>() {
          Deque<Integer> counters = new ArrayDeque<Integer>();
          String inMethod = null;

          private void save(Tree node, ASTRecord rec, Kind kind, String sel) {
            if (node != null) {
              node.accept(this, rec.extend(kind, sel));
            }
          }

          private void save(Tree node, ASTRecord rec, Kind kind, String sel, int arg) {
            if (node != null) {
              node.accept(this, rec.extend(kind, sel, arg));
            }
          }

          private void saveAll(
              Iterable<? extends Tree> nodes, ASTRecord rec, Kind kind, String sel) {
            if (nodes != null) {
              int i = 0;
              for (Tree node : nodes) {
                save(node, rec, kind, sel, i++);
              }
            }
          }

          /**
           * Save a class.
           *
           * @param node the class
           */
          private void saveClass(ClassTree node) {
            String className = ((JCTree.JCClassDecl) node).sym.flatname.toString();
            ASTRecord rec = new ASTRecord(cut, className, null, null, ASTPath.empty());
            counters.push(0);
            node.accept(this, rec);
            counters.pop();
          }

          @Override
          public Void defaultAction(Tree node, ASTRecord rec) {
            switch (node.getKind()) {
              case BREAK:
              case COMPILATION_UNIT:
              case CONTINUE:
              case IMPORT:
              case MODIFIERS:
                break; // not handled
              default:
                put(node, rec);
            }
            return null;
          }

          @Override
          public Void visitAnnotatedType(AnnotatedTypeTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            saveAll(node.getAnnotations(), rec, kind, ASTPath.ANNOTATION);
            save(node.getUnderlyingType(), rec, kind, ASTPath.UNDERLYING_TYPE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitAnnotation(AnnotationTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getAnnotationType(), rec, kind, ASTPath.TYPE);
            saveAll(node.getArguments(), rec, kind, ASTPath.ARGUMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitMethodInvocation(MethodInvocationTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            saveAll(node.getTypeArguments(), rec, kind, ASTPath.TYPE_ARGUMENT);
            save(node.getMethodSelect(), rec, kind, ASTPath.METHOD_SELECT);
            saveAll(node.getArguments(), rec, kind, ASTPath.ARGUMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitAssert(AssertTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getCondition(), rec, kind, ASTPath.CONDITION);
            save(node.getDetail(), rec, kind, ASTPath.DETAIL);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitAssignment(AssignmentTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            save(node.getVariable(), rec, kind, ASTPath.VARIABLE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitCompoundAssignment(CompoundAssignmentTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            save(node.getVariable(), rec, kind, ASTPath.VARIABLE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitBinary(BinaryTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getLeftOperand(), rec, kind, ASTPath.LEFT_OPERAND);
            save(node.getRightOperand(), rec, kind, ASTPath.RIGHT_OPERAND);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitBlock(BlockTree node, ASTRecord rec) {
            Iterable<? extends Tree> nodes = node.getStatements();
            if (nodes != null) {
              int i = 0;
              for (Tree stmt : nodes) {
                if (ASTPath.isClassEquiv(stmt.getKind())) {
                  saveClass((ClassTree) stmt);
                } else {
                  save(stmt, rec, node.getKind(), ASTPath.STATEMENT, i);
                }
                ++i;
              }
            }
            return defaultAction(node, rec);
          }

          @Override
          public Void visitCase(CaseTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            saveAll(CaseUtils.caseTreeGetExpressions(node), rec, kind, ASTPath.EXPRESSION);
            saveAll(node.getStatements(), rec, kind, ASTPath.STATEMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitCatch(CatchTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getBlock(), rec, kind, ASTPath.BLOCK);
            save(node.getParameter(), rec, kind, ASTPath.PARAMETER);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitClass(ClassTree node, ASTRecord rec) {
            Kind kind = Tree.Kind.CLASS; // use for all class-equivalent kinds
            int i = 0;
            formals.put(rec.className, new HashMap<>());
            if (node.getSimpleName().length() > 0) {
              // don't save exts/impls for anonymous inner class
              save(node.getExtendsClause(), rec, kind, ASTPath.BOUND, -1);
              saveAll(node.getImplementsClause(), rec, kind, ASTPath.BOUND);
            }
            saveAll(node.getTypeParameters(), rec, kind, ASTPath.TYPE_PARAMETER);
            for (Tree member : node.getMembers()) {
              if (member instanceof BlockTree) {
                save(member, rec, kind, ASTPath.INITIALIZER, i++);
              } else if (ASTPath.isClassEquiv(member.getKind())) {
                String className = ((JCTree.JCClassDecl) member).sym.flatname.toString();
                member.accept(this, new ASTRecord(cut, className, null, null, ASTPath.empty()));
              } else {
                member.accept(this, rec);
              }
            }
            return defaultAction(node, rec);
          }

          @Override
          public Void visitConditionalExpression(ConditionalExpressionTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getCondition(), rec, kind, ASTPath.CONDITION);
            save(node.getFalseExpression(), rec, kind, ASTPath.FALSE_EXPRESSION);
            save(node.getTrueExpression(), rec, kind, ASTPath.TRUE_EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitDoWhileLoop(DoWhileLoopTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getCondition(), rec, kind, ASTPath.CONDITION);
            save(node.getStatement(), rec, kind, ASTPath.STATEMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitExpressionStatement(ExpressionStatementTree node, ASTRecord rec) {
            save(node.getExpression(), rec, node.getKind(), ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitEnhancedForLoop(EnhancedForLoopTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getVariable(), rec, kind, ASTPath.VARIABLE);
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            save(node.getStatement(), rec, kind, ASTPath.STATEMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitForLoop(ForLoopTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            saveAll(node.getInitializer(), rec, kind, ASTPath.INITIALIZER);
            save(node.getCondition(), rec, kind, ASTPath.CONDITION);
            save(node.getStatement(), rec, kind, ASTPath.STATEMENT);
            saveAll(node.getUpdate(), rec, kind, ASTPath.UPDATE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitIf(IfTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getCondition(), rec, kind, ASTPath.CONDITION);
            save(node.getThenStatement(), rec, kind, ASTPath.THEN_STATEMENT);
            save(node.getElseStatement(), rec, kind, ASTPath.ELSE_STATEMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitArrayAccess(ArrayAccessTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            save(node.getIndex(), rec, kind, ASTPath.INDEX);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitLabeledStatement(LabeledStatementTree node, ASTRecord rec) {
            save(node.getStatement(), rec, node.getKind(), ASTPath.STATEMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitMethod(MethodTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            Tree rcvr = node.getReceiverParameter();
            ModifiersTree mods = node.getModifiers();
            List<? extends Tree> params = node.getParameters();
            String outMethod = inMethod;
            inMethod = JVMNames.getJVMMethodSignature(node);
            rec = new ASTRecord(cut, rec.className, inMethod, null, ASTPath.empty());
            if (mods != null) {
              save(mods, rec, kind, ASTPath.MODIFIERS);
            }
            if (rcvr != null) {
              rcvr.accept(this, rec.extend(kind, ASTPath.PARAMETER, -1));
            }
            if (params != null && !params.isEmpty()) {
              Map<String, List<String>> map = formals.get(rec.className);
              List<String> names = new ArrayList<>(params.size());
              int i = 0;
              map.put(inMethod, names);
              for (Tree param : params) {
                if (param != null) {
                  names.add(((VariableTree) param).getName().toString());
                  param.accept(this, rec.extend(Tree.Kind.METHOD, ASTPath.PARAMETER, i++));
                }
              }
            }
            save(node.getReturnType(), rec, kind, ASTPath.TYPE);
            saveAll(node.getTypeParameters(), rec, kind, ASTPath.TYPE_PARAMETER);
            // save(node.getReceiverParameter(), rec, kind, ASTPath.PARAMETER, -1);
            // saveAll(node.getParameters(), rec, kind, ASTPath.PARAMETER);
            saveAll(node.getThrows(), rec, kind, ASTPath.THROWS);
            save(node.getBody(), rec, kind, ASTPath.BODY);
            inMethod = outMethod;
            return defaultAction(node, rec);
          }

          @Override
          public Void visitModifiers(ModifiersTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            saveAll(node.getAnnotations(), rec, kind, ASTPath.ANNOTATION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitNewArray(NewArrayTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            Tree type = node.getType();
            int n = node.getDimensions().size();
            do {
              save(type, rec, kind, ASTPath.TYPE, n);
            } while (--n > 0);
            saveAll(node.getDimensions(), rec, kind, ASTPath.DIMENSION);
            saveAll(node.getInitializers(), rec, kind, ASTPath.INITIALIZER);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitNewClass(NewClassTree node, ASTRecord rec) {
            JCTree.JCClassDecl classBody = (JCTree.JCClassDecl) node.getClassBody();
            Kind kind = node.getKind();
            save(node.getEnclosingExpression(), rec, kind, ASTPath.ENCLOSING_EXPRESSION);
            saveAll(node.getTypeArguments(), rec, kind, ASTPath.TYPE_ARGUMENT);
            save(node.getIdentifier(), rec, kind, ASTPath.IDENTIFIER);
            saveAll(node.getArguments(), rec, kind, ASTPath.ARGUMENT);
            if (classBody != null) {
              Name name = classBody.getSimpleName();
              String className = null;
              if (name == null || name.toString().isEmpty()) {
                int i = counters.pop();
                counters.push(++i);
                className = rec.className + "$" + i;
              } else {
                ClassSymbol sym = classBody.sym;
                String s = sym == null ? "" : sym.toString();
                if (s.startsWith("<anonymous ")) {
                  int i = counters.pop();
                  counters.push(++i);
                  className = s.substring(11, s.length() - 1);
                } else {
                  className = rec.className + "$" + name;
                }
              }
              counters.push(0);
              classBody.accept(this, new ASTRecord(cut, className, null, null, ASTPath.empty()));
              counters.pop();
            }
            return defaultAction(node, rec);
          }

          @Override
          public Void visitLambdaExpression(LambdaExpressionTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            String outMethod = inMethod;
            Iterable<? extends Tree> nodes = node.getParameters();
            if (nodes != null) {
              int i = 0;
              for (Tree t : nodes) {
                ASTRecord newRec = rec.extend(kind, ASTPath.PARAMETER, i++);
                Tree.Kind newKind = t.getKind();
                if (newKind == Tree.Kind.VARIABLE) {
                  VariableTree vt = (VariableTree) t;
                  save(vt.getType(), newRec, newKind, ASTPath.TYPE);
                  save(vt.getInitializer(), newRec, newKind, ASTPath.INITIALIZER);
                  defaultAction(vt, newRec);
                } else {
                  t.accept(this, rec.extend(kind, ASTPath.PARAMETER, i++));
                }
              }
            }
            save(node.getBody(), rec, kind, ASTPath.BODY);
            inMethod = outMethod;
            return defaultAction(node, rec);
          }

          @Override
          public Void visitParenthesized(ParenthesizedTree node, ASTRecord rec) {
            save(node.getExpression(), rec, node.getKind(), ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitReturn(ReturnTree node, ASTRecord rec) {
            save(node.getExpression(), rec, node.getKind(), ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitMemberSelect(MemberSelectTree node, ASTRecord rec) {
            save(node.getExpression(), rec, node.getKind(), ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitMemberReference(MemberReferenceTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getQualifierExpression(), rec, kind, ASTPath.QUALIFIER_EXPRESSION);
            saveAll(node.getTypeArguments(), rec, kind, ASTPath.TYPE_ARGUMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitSwitch(SwitchTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            saveAll(node.getCases(), rec, kind, ASTPath.CASE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitSynchronized(SynchronizedTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            save(node.getBlock(), rec, kind, ASTPath.BLOCK);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitThrow(ThrowTree node, ASTRecord rec) {
            save(node.getExpression(), rec, node.getKind(), ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitCompilationUnit(CompilationUnitTree node, ASTRecord rec) {
            for (Tree tree : node.getTypeDecls()) {
              if (ASTPath.isClassEquiv(tree.getKind())) {
                saveClass((ClassTree) tree);
              }
            }
            return null;
          }

          @Override
          public Void visitTry(TryTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            saveAll(node.getResources(), rec, kind, ASTPath.RESOURCE);
            save(node.getBlock(), rec, kind, ASTPath.BLOCK);
            saveAll(node.getCatches(), rec, kind, ASTPath.CATCH);
            save(node.getFinallyBlock(), rec, kind, ASTPath.FINALLY_BLOCK);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitParameterizedType(ParameterizedTypeTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getType(), rec, kind, ASTPath.TYPE);
            saveAll(node.getTypeArguments(), rec, kind, ASTPath.TYPE_ARGUMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitUnionType(UnionTypeTree node, ASTRecord rec) {
            saveAll(node.getTypeAlternatives(), rec, node.getKind(), ASTPath.TYPE_ALTERNATIVE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitIntersectionType(IntersectionTypeTree node, ASTRecord rec) {
            saveAll(node.getBounds(), rec, node.getKind(), ASTPath.BOUND);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitArrayType(ArrayTypeTree node, ASTRecord rec) {
            save(node.getType(), rec, node.getKind(), ASTPath.TYPE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitTypeCast(TypeCastTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getType(), rec, kind, ASTPath.TYPE);
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitTypeParameter(TypeParameterTree node, ASTRecord rec) {
            saveAll(node.getBounds(), rec, node.getKind(), ASTPath.BOUND);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitInstanceOf(InstanceOfTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getExpression(), rec, kind, ASTPath.EXPRESSION);
            save(node.getType(), rec, kind, ASTPath.TYPE);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitUnary(UnaryTree node, ASTRecord rec) {
            save(node.getExpression(), rec, node.getKind(), ASTPath.EXPRESSION);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitVariable(VariableTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            if (rec.methodName == null) { // member field
              rec =
                  new ASTRecord(
                      cut, rec.className, rec.methodName, node.getName().toString(), rec.astPath);
            }
            save(node.getType(), rec, kind, ASTPath.TYPE);
            save(node.getInitializer(), rec, kind, ASTPath.INITIALIZER);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitWhileLoop(WhileLoopTree node, ASTRecord rec) {
            Kind kind = node.getKind();
            save(node.getCondition(), rec, kind, ASTPath.CONDITION);
            save(node.getStatement(), rec, kind, ASTPath.STATEMENT);
            return defaultAction(node, rec);
          }

          @Override
          public Void visitWildcard(WildcardTree node, ASTRecord rec) {
            save(node.getBound(), rec, node.getKind(), ASTPath.BOUND);
            return defaultAction(node, rec);
          }
        },
        null);
  }

  public static ASTRecord getASTPath(CompilationUnitTree cut, Tree node) {
    return indexOf(cut).get(node);
  }

  public static TreePath getTreePath(CompilationUnitTree cut, ASTRecord rec) {
    Tree node = getNode(cut, rec);
    return node == null ? null : TreePath.getPath(cut, node);
  }

  public static Tree getNode(CompilationUnitTree cut, ASTRecord rec) {
    Map<Tree, ASTRecord> fwdIndex = ((ASTIndex) indexOf(cut)).back;
    Map<ASTRecord, Tree> revIndex = ((BiMap<Tree, ASTRecord>) fwdIndex).inverse();
    ExpressionTree et = cut.getPackageName();
    String pkg = et == null ? "" : et.toString();
    if (!pkg.isEmpty() && rec.className.indexOf('.') < 0) {
      String className = pkg + "." + rec.className;
      rec = new ASTRecord(cut, className, rec.methodName, rec.varName, rec.astPath);
    }
    return revIndex.get(rec);
  }

  public static String getParameterName(
      CompilationUnitTree cut, String className, String methodName, int index) {
    try {
      ASTIndex ai = (ASTIndex) ASTIndex.indexOf(cut);
      return ai.formals.get(className).get(methodName).get(index);
    } catch (NullPointerException ex) {
      return null;
    }
  }

  @SuppressWarnings("EmptyCatch") // TODO
  public static Integer getParameterIndex(
      CompilationUnitTree cut, String className, String methodName, String varName) {
    if (cut != null && className != null && methodName != null && varName != null) {
      // If `varName` is already a number, return it
      try {
        return Integer.valueOf(varName);
      } catch (NumberFormatException ex) {
        // Fall through in order to check name.
      }
      // otherwise, look through parameter list for string
      try {
        ASTIndex ai = (ASTIndex) ASTIndex.indexOf(cut);
        List<String> names = ai.formals.get(className).get(methodName);
        int i = 0;
        for (String name : names) {
          if (varName.equals(name)) {
            return i;
          }
          ++i;
        }
      } catch (NullPointerException ex) {
        // Not found.
        // TODO: It would be cleaner to check for null above than to catch the exception here.
      }
    }
    // not found
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Tree, ASTRecord> entry : entrySet()) {
      sb.append(entry.getKey().toString().replaceAll("\\s+", " "))
          .append(" # ")
          .append(entry.getValue())
          .append("\n");
    }
    return sb.toString();
  }
}
