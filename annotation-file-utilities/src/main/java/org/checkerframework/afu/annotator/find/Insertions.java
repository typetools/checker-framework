package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.io.ASTIndex;
import org.checkerframework.afu.scenelib.io.ASTPath;
import org.checkerframework.afu.scenelib.io.ASTRecord;
import org.checkerframework.afu.scenelib.type.ArrayType;
import org.checkerframework.afu.scenelib.type.BoundedType;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;
import org.objectweb.asm.TypePath;

/**
 * A collection of {@link Insertion}s, indexed by outer class and inner class. It has methods to
 * select Insertions for a given class ({@link #forClass}) or for an outer class along with its
 * local classes ({@link #forOuterClass}). When a single JAIF stores annotations for many source
 * files, this class reduces the number of insertions to be considered for any AST node.
 *
 * <p>The class now serves a second purpose, which should probably be separated out: It attaches
 * {@link ASTPath}-based inner type {@link Insertion}s to a {@link TypedInsertion} on the outer type
 * if one exists (see {@link #organizeTypedInsertions(CompilationUnitTree, String, Collection)}.
 * Since getting these insertions right depends on this organization, this class is now essential
 * for correctness, not merely for performance.
 */
public class Insertions implements Iterable<Insertion> {

  /**
   * First index is (qualified) outer class name, second index is inner class path (or "" if not
   * within a nested class).
   */
  // TODO: Inner class name might itself contain "$"; this probably doesn't handle that case.
  private Map<String, Map<String, Set<Insertion>>> store;

  /** The number of {@link Insertion}s in this collection. */
  private int size;

  public Insertions() {
    store = new HashMap<>();
    size = 0;
  }

  /**
   * Selects {@link Insertion}s relevant to a given class.
   *
   * @param cut the current compilation unit
   * @param qualifiedClassName the fully qualified class name
   * @return {@link java.util.Set} of {@link Insertion}s with an {@link InClassCriterion} for the
   *     given class
   */
  public Set<Insertion> forClass(CompilationUnitTree cut, String qualifiedClassName) {
    Set<Insertion> set = new LinkedHashSet<>();
    forClass(cut, qualifiedClassName, set);
    return set;
  }

  /**
   * Selects {@link Insertion}s relevant to a given outer class and its local classes.
   *
   * @param cut the current compilation unit
   * @param qualifiedOuterClassName the fully qualified outer class name
   * @return set of {@link Insertion}s with an {@link InClassCriterion} for the given outer class or
   *     one of its local classes
   */
  @SuppressWarnings("MixedMutabilityReturnType") // clients do not modify the result
  public Set<Insertion> forOuterClass(CompilationUnitTree cut, String qualifiedOuterClassName) {
    Map<String, Set<Insertion>> map = store.get(qualifiedOuterClassName);
    if (map == null || map.isEmpty()) {
      return Collections.<Insertion>emptySet();
    } else {
      if (Main.temporaryDebug) {
        System.out.printf("forOuterClass(%s): map = %s%n", qualifiedOuterClassName, map);
      }
      Set<Insertion> set = new LinkedHashSet<>();
      for (String innerClassPath : map.keySet()) {
        String qualifiedClassName = qualifiedOuterClassName + innerClassPath;
        forClass(cut, qualifiedClassName, set);
      }
      return set;
    }
  }

  /** Side-effects {@code result} to add {@link Insertion}s for {@code qualifiedClassName}. */
  private void forClass(CompilationUnitTree cut, String qualifiedClassName, Set<Insertion> result) {
    if (Main.temporaryDebug) {
      System.out.printf(
          "calling forClass(cut, %s, set of size %d)%n", qualifiedClassName, result.size());
    }
    String outerClass = outerClassName(qualifiedClassName);
    Map<String, Set<Insertion>> map = store.get(outerClass);
    if (map != null) {
      Set<Insertion> set = new TreeSet<>(byASTRecord);
      set.addAll(map.get(innerClassName(qualifiedClassName)));
      if (Main.temporaryDebug) {
        System.out.println("organizeTypedInsertions argument set size = " + set.size());
      }
      Set<Insertion> organized = organizeTypedInsertions(cut, qualifiedClassName, set);
      if (Main.temporaryDebug) {
        System.out.println("organizeTypedInsertions result set size = " + organized.size());
      }
      result.addAll(organized);
    }
  }

  /** Add an {@link Insertion} to this collection. */
  public void add(Insertion ins) {
    InClassCriterion icc = ins.getCriteria().getInClass();

    String outerClass;
    String innerClass;
    if (icc == null) {
      // Not in a class.
      outerClass = "";
      innerClass = "";
    } else {
      outerClass = outerClassName(icc.className);
      innerClass = innerClassName(icc.className);
    }

    Map<String, Set<Insertion>> map = store.get(outerClass);
    if (map == null) {
      map = new HashMap<>();
      store.put(outerClass, map);
    }

    Set<Insertion> set = map.get(innerClass);
    if (set == null) {
      set = new LinkedHashSet<Insertion>();
      map.put(innerClass, set);
    }

    size -= set.size();
    set.add(ins);
    size += set.size();
  }

  /** Add all the given {@link Insertion}s to this collection. */
  public void addAll(Collection<? extends Insertion> c) {
    for (Insertion ins : c) {
      add(ins);
    }
  }

  /** Returns the number of {@link Insertion}s in this collection. */
  public int size() {
    return size;
  }

  @Override
  public Iterator<Insertion> iterator() {
    return new Iterator<Insertion>() {
      private Iterator<Map<String, Set<Insertion>>> miter = store.values().iterator();
      // These two fields are initially empty iterators, but are set the first time that hasNext is
      // called.
      private Iterator<Set<Insertion>> siter = Collections.<Set<Insertion>>emptySet().iterator();
      private Iterator<Insertion> iiter = Collections.<Insertion>emptySet().iterator();

      @Override
      public boolean hasNext() {
        if (iiter.hasNext()) {
          return true;
        }
        if (siter.hasNext()) {
          iiter = siter.next().iterator();
          return hasNext();
        }
        if (miter.hasNext()) {
          siter = miter.next().values().iterator();
          return hasNext();
        }
        return false;
      }

      @Override
      public Insertion next() {
        if (hasNext()) {
          return iiter.next();
        }
        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** Returns a {@link java.util.List} containing all {@link Insertion}s in this collection. */
  public List<Insertion> toList() {
    List<Insertion> list = new ArrayList<>(size);
    for (Insertion ins : this) {
      list.add(ins);
    }
    return list;
  }

  /**
   * This method detects inner type relationships among ASTPath-based insertion specifications and
   * organizes the insertions accordingly. [TODO: What is "accordingly"?] This step is necessary
   * because 1) insertion proceeds from the end to the beginning of the source and 2) the insertion
   * location does not always exist prior to the top-level type insertion.
   *
   * <p>This method attaches {@link ASTPath}-based inner type {@link Insertion}s to a {@link
   * TypedInsertion} on the outer type if one exists.
   */
  @SuppressWarnings("CatchAndPrintStackTrace") // maybe rethrow the exception
  private Set<Insertion> organizeTypedInsertions(
      CompilationUnitTree cut, String className, Collection<Insertion> insertions) {
    Map<ASTRecord, TypedInsertion> outerInsertions = new HashMap<>();
    Set<Insertion> innerInsertions = new LinkedHashSet<>();
    List<Insertion> innerInsertionsList = new ArrayList<>();
    Set<Insertion> organized = new LinkedHashSet<>();

    if (Main.temporaryDebug) {
      System.out.printf("organizeTypedInsertions (1): insertions.size()= %d%n", insertions.size());
    }

    // First divide the insertions into three buckets:
    //  * TypedInsertions on outer types (`outerInsertions`)
    //  * ASTPath-based insertions on local types (`innerInsertions` --
    //    built as list and then sorted, since building as a set spuriously
    //    removes "duplicates" according to the comparator), and
    //  * everything else (`organized` -- where all eventually land).
    for (Insertion ins : insertions) {
      if (Main.temporaryDebug) {
        System.out.printf("Considering insertion %s (isInserted=%s)%n", ins, ins.isInserted());
      }
      if (ins.isInserted()) {
        continue;
      }
      Criteria criteria = ins.getCriteria();
      GenericArrayLocationCriterion galc = criteria.getGenericArrayLocation();
      ASTPath p = criteria.getASTPath();
      if (p == null
          || p.isEmpty()
          || (galc != null && !galc.getLocation().isEmpty())
          || ins instanceof CastInsertion
          || ins instanceof CloseParenthesisInsertion) {
        if (Main.temporaryDebug) {
          System.out.printf("Adding to organized (size %d): %s%n", organized.size(), ins);
        }
        organized.add(ins);
        if (Main.temporaryDebug) {
          System.out.printf("  organized now has size %d%n", organized.size());
        }
      } else {
        ASTRecord rec =
            new ASTRecord(
                cut, criteria.getClassName(), criteria.getMethodName(), criteria.getFieldName(), p);
        ASTPath.ASTEntry entry = rec.astPath.getLast();

        Tree node;
        if (entry.getTreeKind() == Tree.Kind.NEW_ARRAY
            && entry.childSelectorIs(ASTPath.TYPE)
            && entry.getArgument() == 0) {
          ASTPath parentPath = rec.astPath.getParentPath();
          node = ASTIndex.getNode(cut, rec.replacePath(parentPath));
          node =
              node instanceof JCTree.JCNewArray
                  ? TypeTree.fromJavacType(((JCTree.JCNewArray) node).type)
                  : null;
        } else {
          node = ASTIndex.getNode(cut, rec);
        }

        if (ins instanceof TypedInsertion) {
          TypedInsertion tins = outerInsertions.get(rec);
          if (ins instanceof NewInsertion) {
            NewInsertion nins = (NewInsertion) ins;
            if (entry.getTreeKind() == Tree.Kind.NEW_ARRAY && entry.childSelectorIs(ASTPath.TYPE)) {
              int a = entry.getArgument();
              List<TypePathEntry> loc0 = new ArrayList<>(a);
              ASTRecord rec0 = null;
              if (a == 0) {
                rec0 = rec.replacePath(p.getParentPath());
                Tree t = ASTIndex.getNode(cut, rec0);
                if (t == null || t.toString().startsWith("{")) {
                  rec0 = null;
                } else {
                  rec = rec0;
                  rec0 = rec.extend(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, 0);
                }
              } else if (node != null && !nins.getInnerTypeInsertions().isEmpty()) {
                if (node instanceof IdentifierTree) {
                  node = ASTIndex.getNode(cut, rec.replacePath(p.getParentPath()));
                }
                if ((node instanceof NewArrayTree || node instanceof ArrayTypeTree)
                    && !node.toString().startsWith("{")) {
                  rec = rec.replacePath(p.getParentPath());

                  Collections.fill(loc0, TypePathEntry.ARRAY_ELEMENT);
                  // irec = rec;
                  // if (node.getKind() == Tree.Kind.NEW_ARRAY) {
                  rec0 = rec.extend(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, 0);
                  // }
                }
              }

              if (rec0 != null) {
                for (Insertion inner : nins.getInnerTypeInsertions()) {
                  Criteria icriteria = inner.getCriteria();
                  GenericArrayLocationCriterion igalc = icriteria.getGenericArrayLocation();
                  if (igalc != null) {
                    ASTRecord rec1;
                    int b = igalc.getLocation().size();
                    List<TypePathEntry> loc = new ArrayList<>(a + b);
                    loc.addAll(loc0);
                    loc.addAll(igalc.getLocation());
                    rec1 = extendToInnerType(rec0, loc, node);
                    icriteria.addPermitReplacement(new GenericArrayLocationCriterion());
                    icriteria.addPermitReplacement(new ASTPathCriterion(rec1.astPath));
                    inner.setInserted(false);
                    if (Main.temporaryDebug) {
                      System.out.printf(
                          "Adding to organized (size %d): %s%n", organized.size(), ins);
                    }
                    organized.add(inner);
                    if (Main.temporaryDebug) {
                      System.out.printf("  organized now has size %d%n", organized.size());
                    }
                  }
                }
                nins.getInnerTypeInsertions().clear();
              }
            }
          }
          if (tins == null) {
            outerInsertions.put(rec, (TypedInsertion) ins);
          } else if (tins.getType().equals(((TypedInsertion) ins).getType())) {
            mergeTypedInsertions(tins, (TypedInsertion) ins);
          }
        } else {
          int d = newArrayInnerTypeDepth(p);
          if (d > 0) {
            ASTPath temp = p;
            while (!temp.isEmpty() && (node == null || !(node instanceof NewArrayTree))) {
              // TODO: avoid repeating work of newArrayInnerTypeDepth()
              temp = temp.getParentPath();
              node = ASTIndex.getNode(cut, rec.replacePath(temp));
            }
            if (node == null) {
              // TODO: ???
              throw new Error("node == null case not yet implemented");
            }
            temp = temp.extend(new ASTPath.ASTEntry(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, 0));
            if (node.toString().startsWith("{")) {
              TypedInsertion tins = outerInsertions.get(rec.replacePath(temp));
              if (tins == null) {
                // TODO
                throw new Error("tins == null case not yet implemented");
              } else {
                tins.getInnerTypeInsertions().add(ins);
                ins.setInserted(true);
              }
            } else {
              List<? extends ExpressionTree> dims = ((NewArrayTree) node).getDimensions();
              ASTRecord irec =
                  rec.replacePath(p.getParentPath()).extend(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, 0);
              GenericArrayLocationCriterion igalc = criteria.getGenericArrayLocation();
              for (int i = 0; i < d; i++) {
                irec = irec.extend(Tree.Kind.ARRAY_TYPE, ASTPath.TYPE);
              }
              if (igalc != null) {
                List<TypePathEntry> loc = igalc.getLocation();
                if (!loc.isEmpty()) {
                  try {
                    Tree dim = dims.get(d - 1);
                    irec = extendToInnerType(irec, loc, dim);
                    criteria.add(new ASTPathCriterion(irec.astPath));
                    criteria.add(new GenericArrayLocationCriterion());
                  } catch (RuntimeException e) {
                    throw new Error(e);
                  }
                }
              }
            }
          }
          innerInsertionsList.add(ins);
        }
      }
    }
    // if (outerInsertions.isEmpty()) {
    //  organized.addAll(innerInsertions);
    //  return organized;
    // }
    if (Main.temporaryDebug) {
      System.out.printf("organized.size() (1) = %d%n", organized.size());
    }
    if (Main.temporaryDebug) {
      System.out.printf("innerInsertionsList size (1) = %d%n", innerInsertionsList.size());
    }
    Collections.sort(innerInsertionsList, byASTRecord);
    if (Main.temporaryDebug) {
      System.out.printf("innerInsertionsList size (2) = %d%n", innerInsertionsList.size());
    }
    if (Main.temporaryDebug) {
      System.out.printf("innerInsertions size (1) = %d%n", innerInsertions.size());
    }
    innerInsertions.addAll(innerInsertionsList);
    if (Main.temporaryDebug) {
      System.out.printf("innerInsertions size (2) = %d%n", innerInsertions.size());
    }

    // Each Insertion in innerInsertions gets attached to a TypedInsertion
    // in outerInsertions if possible; otherwise, it gets dumped into organized.
    for (Insertion ins : innerInsertions) {
      Criteria criteria = ins.getCriteria();
      String methodName = criteria.getMethodName();
      String fieldName = criteria.getFieldName();
      ASTPath localTypePath = criteria.getASTPath();
      List<TypePathEntry> tpes = new ArrayList<>();
      if (localTypePath == null) {
        // || methodName == null && fieldName == null)
        organized.add(ins);
        continue;
      }

      // First find the relevant "top-level" insertion, if any.
      Deque<ASTPath> astack = new ArrayDeque<ASTPath>(localTypePath.size());
      ASTPath topLevelTypePath = localTypePath;
      do {
        astack.push(topLevelTypePath);
        topLevelTypePath = topLevelTypePath.getParentPath();
      } while (!topLevelTypePath.isEmpty());
      ASTRecord rec;
      Tree.Kind kind;
      do {
        topLevelTypePath = astack.pop();
        kind = topLevelTypePath.getLast().getTreeKind();
        rec = new ASTRecord(cut, className, methodName, fieldName, topLevelTypePath);
      } while (!(astack.isEmpty() || outerInsertions.containsKey(rec)));

      TypedInsertion tins = outerInsertions.get(rec);
      TreePath path = ASTIndex.getTreePath(cut, rec);
      Tree node = path == null ? null : path.getLeaf();
      if (node == null && topLevelTypePath.isEmpty()) {
        organized.add(ins);
        continue;
      }

      // Try to create a top-level insertion if none exists (e.g., if
      // there is an insertion for NewArray.type 1 but not for 0).
      if (tins == null) {
        GenericArrayLocationCriterion galc = criteria.getGenericArrayLocation();
        if (node == null) {
          // TODO: figure out from rec?
          organized.add(ins);
          continue;
        } else {
          Tree t = path.getLeaf();
          switch (t.getKind()) {
            case NEW_ARRAY:
              int d = 0;
              ASTPath.ASTEntry e = localTypePath.getLast();
              List<TypePathEntry> loc = null;
              List<Insertion> inners = new ArrayList<>();
              Type type = TypeTree.javacTypeToType(((JCTree.JCNewArray) t).type);
              if (e.getTreeKind() == Tree.Kind.NEW_ARRAY) {
                d += e.getArgument();
              }
              if (galc != null) {
                loc = galc.getLocation();
                int n = loc.size();
                while (--n >= 0 && loc.get(n).step == TypePath.ARRAY_ELEMENT) {
                  ++d;
                }
                loc = n < 0 ? null : loc.subList(0, ++n);
              }
              criteria.add(new ASTPathCriterion(rec.astPath.getParentPath().extendNewArray(d)));
              criteria.add(
                  loc == null || loc.isEmpty()
                      ? new GenericArrayLocationCriterion()
                      : new GenericArrayLocationCriterion(loc));
              inners.add(ins);
              tins = new NewInsertion(type, criteria, inners);
              tins.setInserted(true);
              outerInsertions.put(rec, tins);
              break;
            default:
              break;
          }
          path = path.getParentPath();
        }
      }

      // The sought node may or may not be found in the tree; if not, it
      // may need to be created later.  Use whatever part of the path
      // exists already to distinguish MEMBER_SELECT nodes that indicate
      // qualifiers from those that indicate local types.  Assume any
      // MEMBER_SELECTs in the AST path that don't correspond to
      // existing nodes are part of a type use.
      if (node == null) {
        ASTPath ap = topLevelTypePath;
        if (!ap.isEmpty()) {
          do {
            ap = ap.getParentPath();
            node = ASTIndex.getNode(cut, rec.replacePath(ap));
          } while (node == null && !ap.isEmpty());
        }
        if (node == null) {
          organized.add(ins);
          continue;
        }

        // find actual type
        ClassSymbol csym = null;
        switch (tins.getKind()) {
          case CONSTRUCTOR:
            if (node instanceof JCTree.JCMethodDecl) {
              MethodSymbol msym = ((JCTree.JCMethodDecl) node).sym;
              csym = (ClassSymbol) msym.owner;
              node = TypeTree.fromJavacType(csym.type);
              break;
            } else if (node instanceof JCTree.JCClassDecl) {
              csym = ((JCTree.JCClassDecl) node).sym;
              if (csym.owner instanceof ClassSymbol) {
                csym = (ClassSymbol) csym.owner;
                node = TypeTree.fromJavacType(csym.type);
                break;
              }
            }
            throw new RuntimeException();
          case NEW:
            if (node instanceof JCTree.JCNewArray) {
              if (node.toString().startsWith("{")) {
                node = TypeTree.fromJavacType(((JCTree.JCNewArray) node).type);
                break;
              } else {
                organized.add(ins);
                continue;
              }
            }
            throw new RuntimeException();
          case RECEIVER:
            if (node instanceof JCTree.JCMethodDecl) {
              JCTree.JCMethodDecl jmd = (JCTree.JCMethodDecl) node;
              csym = (ClassSymbol) jmd.sym.owner;
              if ("<init>".equals(jmd.name.toString())) {
                csym = (ClassSymbol) csym.owner;
              }
            } else if (node instanceof JCTree.JCClassDecl) {
              csym = ((JCTree.JCClassDecl) node).sym;
            }
            if (csym != null) {
              node = TypeTree.fromJavacType(csym.type);
              break;
            }
            throw new RuntimeException();
          default:
            throw new RuntimeException();
        }
      }

      /*
       * Inner types require special consideration due to the
       * structural differences between an AST that represents a type
       * (subclass of com.sun.source.Tree) and the type's logical
       * representation (subclass of type.Type).  The differences are
       * most prominent in the case of a type with a parameterized
       * local type.  For example, the AST for A.B.C<D> looks like
       * this:
       *
       *                     ParameterizedType
       *                    /                 \
       *               MemberSelect       Identifier
       *              /            \           |
       *        MemberSelect      (Name)       D
       *         /      \           |
       *  Identifier   (Name)       C
       *        |        |
       *        A        B
       *
       * (Technically, the Names are not AST nodes but rather
       * attributes of their parent MemberSelect nodes.)  The logical
       * representation seems more intuitive:
       *
       *       DeclaredType
       *      /     |      \
       *    Name  Params  Inner
       *     |      |       |
       *     A      -  DeclaredType
       *              /     |      \
       *            Name  Params  Inner
       *             |      |       |
       *             B      -  DeclaredType
       *                      /     |      \
       *                    Name  Params  Inner
       *                     |      |       |
       *                     C      D       -
       *
       * The opposing "chirality" of local type nesting means that the
       * usual recursive descent strategy doesn't work for finding a
       * logical type path in an AST; in effect, local types have to
       * be "turned inside-out".
       *
       * Worse yet, the actual tree structure may not exist in the tree!
       * It is possible to recover the actual type from the symbol
       * table, but the methods to create AST nodes are not visible
       * here.  Hence, the conversion relies on custom implementations
       * of the interfaces in com.sun.source.tree.Tree, which are
       * defined in the local class TypeTree.
       */
      int i = topLevelTypePath.size();
      int n = localTypePath.size();
      int actualDepth = 0; // inner type levels seen
      int expectedDepth = 0; // inner type levels anticipated

      // skip any declaration nodes
      while (i < n) {
        ASTPath.ASTEntry entry = localTypePath.get(i);
        kind = entry.getTreeKind();
        if (kind != Tree.Kind.METHOD && kind != Tree.Kind.VARIABLE) {
          break;
        }
        ++i;
      }

      // now build up the type path in JVM's format
      while (i < n) {
        ASTPath.ASTEntry entry = localTypePath.get(i);
        rec = rec.extend(entry);
        kind = entry.getTreeKind();

        while (node instanceof AnnotatedTypeTree) {
          node = ((AnnotatedTypeTree) node).getUnderlyingType();
        }
        if (expectedDepth == 0) {
          expectedDepth = localDepth(node);
        }

        switch (kind) {
          case ARRAY_TYPE:
            if (expectedDepth == 0 && node.getKind() == kind) {
              node = ((ArrayTypeTree) node).getType();
              while (--actualDepth >= 0) {
                tpes.add(TypePathEntry.INNER_TYPE);
              }
              tpes.add(TypePathEntry.ARRAY_ELEMENT);
              break;
            }
            throw new RuntimeException();

          case MEMBER_SELECT:
            if (--expectedDepth >= 0) { // otherwise, shouldn't have MEMBER_SELECT
              node = ((MemberSelectTree) node).getExpression();
              ++actualDepth;
              break;
            }
            throw new RuntimeException();

          case NEW_ARRAY:
            assert tpes.isEmpty();
            topLevelTypePath =
                topLevelTypePath.add(new ASTPath.ASTEntry(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, 0));
            if (expectedDepth == 0 && node.getKind() == kind) {
              if (node instanceof JCTree.JCNewArray) {
                int arg = entry.getArgument();
                if (arg > 0) {
                  node = ((JCTree.JCNewArray) node).elemtype;
                  tpes.add(TypePathEntry.ARRAY_ELEMENT);
                  while (--arg > 0 && node instanceof JCTree.JCArrayTypeTree) {
                    node = ((JCTree.JCArrayTypeTree) node).elemtype;
                    tpes.add(TypePathEntry.ARRAY_ELEMENT);
                  }
                  if (arg > 0) {
                    throw new RuntimeException();
                  }
                } else {
                  node = TypeTree.fromJavacType(((JCTree.JCNewArray) node).type);
                }
              } else {
                throw new RuntimeException("NYI"); // TODO
              }
              break;
            }
            throw new RuntimeException();

          case PARAMETERIZED_TYPE:
            if (node.getKind() == kind) {
              ParameterizedTypeTree ptt = (ParameterizedTypeTree) node;
              if (entry.childSelectorIs(ASTPath.TYPE)) {
                node = ptt.getType();
                break; // ParameterizedType.type is "transparent" wrt type path
              } else if (expectedDepth == 0 && entry.childSelectorIs(ASTPath.TYPE_ARGUMENT)) {
                List<? extends Tree> typeArgs = ptt.getTypeArguments();
                int j = entry.getArgument();
                if (j >= 0 && j < typeArgs.size()) {
                  // make sure any inner types are accounted for
                  actualDepth = 0;
                  expectedDepth = localDepth(ptt.getType());
                  while (--expectedDepth >= 0) {
                    tpes.add(TypePathEntry.INNER_TYPE);
                  }
                  node = typeArgs.get(j);
                  tpes.add(TypePathEntry.create(TypePath.TYPE_ARGUMENT, j));
                  break;
                }
              }
            }
            throw new RuntimeException();

          case UNBOUNDED_WILDCARD:
            if (ASTPath.isWildcard(node.getKind())) {
              if (expectedDepth == 0
                  && (i < 1 || localTypePath.get(i - 1).getTreeKind() != Tree.Kind.INSTANCE_OF)
                  && (i < 2 || localTypePath.get(i - 2).getTreeKind() != Tree.Kind.ARRAY_TYPE)) {
                while (--actualDepth >= 0) {
                  tpes.add(TypePathEntry.INNER_TYPE);
                }
                tpes.add(TypePathEntry.WILDCARD_BOUND);
                break;
              }
            }
            throw new RuntimeException();

          default:
            node = ASTIndex.getNode(cut, rec);
            break;
        }

        ++i;
      }

      while (--actualDepth >= 0) {
        tpes.add(TypePathEntry.INNER_TYPE);
      }

      organized.add(ins);
      if (tpes.isEmpty()) {
        // assert localTypePath.equals(topLevelTypePath) &&
        // !outerInsertions.containsKey(topLevelTypePath);
        //        organized.add(ins);
        // outerInsertions.put(rec, (TypedInsertion) ins);
      } else {
        criteria.addPermitReplacement(new ASTPathCriterion(topLevelTypePath));
        criteria.addPermitReplacement(new GenericArrayLocationCriterion(tpes));
        tins.getInnerTypeInsertions().add(ins);
      }
    }
    if (Main.temporaryDebug) {
      System.out.printf("organized.size() (2) = %d%n", organized.size());
    }
    organized.addAll(outerInsertions.values());
    if (Main.temporaryDebug) {
      System.out.printf("organized.size() (3) = %d%n", organized.size());
    }
    return organized;
  }

  // TODO: document this
  private int newArrayInnerTypeDepth(ASTPath path) {
    int result = 0;
    while (!path.isEmpty()) {
      ASTPath.ASTEntry entry = path.getLast();
      switch (entry.getTreeKind()) {
        case ANNOTATED_TYPE:
        case MEMBER_SELECT:
        case PARAMETERIZED_TYPE:
        case UNBOUNDED_WILDCARD:
          result = 0;
          break;
        case ARRAY_TYPE:
          ++result;
          break;
        case NEW_ARRAY:
          if (entry.childSelectorIs(ASTPath.TYPE) && entry.hasArgument()) {
            result += entry.getArgument();
          }
          return result;
        default:
          return 0;
      }
      path = path.getParentPath();
    }
    return 0;
  }

  /**
   * Find an {@link ASTRecord} for the tree corresponding to a nested type of the type (use) to
   * which the given tree and record correspond.
   *
   * @param rec record that locates {@code node} in the source
   * @param loc inner type path
   * @param node starting point for inner type path
   * @return record that locates the nested type in the source
   */
  private ASTRecord extendToInnerType(ASTRecord rec, List<TypePathEntry> loc, Tree node) {
    ASTRecord r = rec;
    Tree t = node;
    Iterator<TypePathEntry> iter = loc.iterator();
    TypePathEntry tpe = iter.next();

    outer:
    while (true) {
      int d = localDepth(node);

      switch (t.getKind()) {
        case ANNOTATED_TYPE:
          r = r.extend(Tree.Kind.ANNOTATED_TYPE, ASTPath.TYPE);
          t = ((JCTree.JCAnnotatedType) t).getUnderlyingType();
          break;

        case ARRAY_TYPE:
          if (d == 0 && tpe.step == TypePath.ARRAY_ELEMENT) {
            int a = 0;
            if (!r.astPath.isEmpty()) {
              ASTPath.ASTEntry e = r.astPath.getLast();
              if (e.getTreeKind() == Tree.Kind.NEW_ARRAY && e.childSelectorIs(ASTPath.TYPE)) {
                a = 1 + e.getArgument();
              }
            }
            r =
                a > 0
                    ? r.replacePath(r.astPath.getParentPath())
                        .extend(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, a)
                    : r.extend(Tree.Kind.ARRAY_TYPE, ASTPath.TYPE);
            t = ((ArrayTypeTree) t).getType();
            break;
          }
          throw new RuntimeException();

        case MEMBER_SELECT:
          if (d > 0 && tpe.step == TypePath.INNER_TYPE) {
            Tree temp = t;
            do {
              temp = ((JCTree.JCFieldAccess) temp).getExpression();
              if (!iter.hasNext()) {
                do {
                  r = r.extend(Tree.Kind.MEMBER_SELECT, ASTPath.EXPRESSION);
                } while (--d > 0);
                return r;
              }
              tpe = iter.next();
              if (--d == 0) {
                continue outer; // avoid next() at end of loop
              }
            } while (tpe.step == TypePath.INNER_TYPE);
          }
          throw new RuntimeException();

        case NEW_ARRAY:
          if (d == 0) {
            if (!r.astPath.isEmpty()) {
              ASTPath.ASTEntry e = r.astPath.getLast();
              if (e.getTreeKind() == Tree.Kind.NEW_ARRAY) {
                int a = 0;
                while (tpe.step == TypePath.ARRAY_ELEMENT) {
                  ++a;
                  if (!iter.hasNext()) {
                    break;
                  }
                  tpe = iter.next();
                }
                r =
                    r.replacePath(r.astPath.getParentPath())
                        .extend(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, a);
                break;
              }
            }
            r = r.extend(Tree.Kind.ARRAY_TYPE, ASTPath.TYPE);
            t = ((JCTree.JCArrayTypeTree) t).getType();
            break;
          }
          throw new RuntimeException();

        case PARAMETERIZED_TYPE:
          if (d == 0 && tpe.step == TypePath.TYPE_ARGUMENT) {
            r = r.extend(Tree.Kind.PARAMETERIZED_TYPE, ASTPath.TYPE_ARGUMENT, tpe.argument);
            t = ((JCTree.JCTypeApply) t).getTypeArguments().get(tpe.step);
            break;
          } else if (d > 0 && tpe.step == TypePath.INNER_TYPE) {
            Tree temp = ((JCTree.JCTypeApply) t).getType();
            r = r.extend(Tree.Kind.PARAMETERIZED_TYPE, ASTPath.TYPE);
            t = temp;
            do {
              temp = ((JCTree.JCFieldAccess) temp).getExpression();
              if (!iter.hasNext()) {
                do {
                  r = r.extend(Tree.Kind.MEMBER_SELECT, ASTPath.EXPRESSION);
                } while (--d > 0);
                return r;
              }
              tpe = iter.next();
              if (--d == 0) {
                continue outer; // avoid next() at end of loop
              }
            } while (tpe.step == TypePath.INNER_TYPE);
          }
          throw new RuntimeException();

        case EXTENDS_WILDCARD:
        case SUPER_WILDCARD:
        case UNBOUNDED_WILDCARD:
          if (tpe.step == TypePath.WILDCARD_BOUND) {
            t = ((JCTree.JCWildcard) t).getBound();
            break;
          }
          throw new RuntimeException();

        default:
          if (iter.hasNext()) {
            throw new RuntimeException();
          }
      }

      if (!iter.hasNext()) {
        return r;
      }
      tpe = iter.next();
    }
  }

  /**
   * Merge annotations, assuming types are structurally identical. Side-effects the first argument.
   */
  private void mergeTypedInsertions(TypedInsertion ins1, TypedInsertion ins2) {
    mergeTypes(ins1.getType(), ins2.getType());
  }

  /**
   * Merge annotations, assuming types are structurally identical. Side-effects the first argument.
   */
  private void mergeTypes(Type t1, Type t2) {
    // TODO: should this test for .equals too?
    if (t1 == t2) {
      return;
    }
    switch (t1.getKind()) {
      case ARRAY:
        {
          ArrayType at1 = (ArrayType) t1;
          ArrayType at2 = (ArrayType) t2;
          mergeTypes(at1.getComponentType(), at2.getComponentType());
          return;
        }
      case BOUNDED:
        {
          BoundedType bt1 = (BoundedType) t1;
          BoundedType bt2 = (BoundedType) t2;
          if (bt1.getBoundKind() != bt2.getBoundKind()) {
            throw new Error(String.format("Types have different bounds: %s %s", t1, t2));
          }
          mergeTypes(bt1.getBound(), bt2.getBound());
          mergeTypes(bt1.getName(), bt2.getName());
          return;
        }
      case DECLARED:
        {
          DeclaredType dt1 = (DeclaredType) t1;
          DeclaredType dt2 = (DeclaredType) t2;
          List<Type> params1 = dt1.getTypeParameters();
          List<Type> params2 = dt2.getTypeParameters();
          int numParams = params1.size();
          if (params2.size() != numParams) {
            throw new Error(
                String.format("Types have different numbers of parameters: %s %s", t1, t2));
          }
          mergeTypes(dt1.getInnerType(), dt2.getInnerType());
          for (String anno : dt2.getAnnotations()) {
            if (!dt1.getAnnotations().contains(anno)) {
              dt1.addAnnotation(anno);
            }
          }
          for (int i = 0; i < numParams; i++) {
            mergeTypes(params1.get(i), params2.get(i));
          }
          return;
        }
      default:
        throw new RuntimeException();
    }
  }

  /**
   * Returns the depth of type nesting of the innermost nested type of a type AST. For example, both
   * {@code A.B.C} and {@code A.B<D.E.F.G>.C} have depth 3.
   */
  private int localDepth(Tree node) {
    Tree t = node;
    int result = 0;
    loop:
    while (t != null) {
      switch (t.getKind()) {
        case ANNOTATED_TYPE:
          t = ((AnnotatedTypeTree) t).getUnderlyingType();
          break;
        case MEMBER_SELECT:
          if (t instanceof JCTree.JCFieldAccess) {
            JCTree.JCFieldAccess jfa = (JCTree.JCFieldAccess) t;
            if (jfa.sym.kind == Kinds.Kind.PCK) {
              t = jfa.getExpression();
              continue;
            }
          }
          t = ((MemberSelectTree) t).getExpression();
          ++result;
          break;
        default:
          break loop;
      }
    }
    return result;
  }

  private static int kindLevel(Insertion i) {
    // Ordered so insertion that depends on another gets inserted after other.
    // TODO: could change to use natural order of the enumeration (reorder the enumeration).
    switch (i.getKind()) {
      case CONSTRUCTOR:
        return 3;
      case NEW:
      case RECEIVER:
        return 2;
      case CAST:
        return 1;
      case ANNOTATION:
      case CLOSE_PARENTHESIS:
        return 0;
      default:
        throw new Error("unrecognized case");
    }
  }

  /** Compare by AstRecord, then by kind, then by string representation. */
  private static final Comparator<Insertion> byASTRecord =
      new Comparator<Insertion>() {
        @Override
        public int compare(Insertion o1, Insertion o2) {
          Criteria crit1 = o1.getCriteria();
          Criteria crit2 = o2.getCriteria();
          ASTPath p1 = crit1.getASTPath();
          ASTPath p2 = crit2.getASTPath();
          ASTRecord r1 =
              new ASTRecord(
                  null,
                  crit1.getClassName(),
                  crit1.getMethodName(),
                  crit1.getFieldName(),
                  p1 == null ? ASTPath.empty() : p1);
          ASTRecord r2 =
              new ASTRecord(
                  null,
                  crit2.getClassName(),
                  crit2.getMethodName(),
                  crit2.getFieldName(),
                  p2 == null ? ASTPath.empty() : p2);
          int cmp;
          cmp = r1.compareTo(r2);
          if (cmp != 0) {
            return cmp;
          }
          // cmp = o1.getKind().compareTo(o2.getKind());
          cmp = Integer.compare(kindLevel(o2), kindLevel(o1)); // descending
          if (cmp != 0) {
            return cmp;
          }
          cmp = o1.toString().compareTo(o2.toString());
          return cmp;
        }
      };

  /**
   * Return the outer class part of the argument; that is, the part before '$'. Return the argument
   * if it contains no '$'.
   */
  private static String outerClassName(String className) {
    int i = className.indexOf('$'); // FIXME: don't split on '$' in source
    if (i == -1) {
      return className;
    } else {
      return className.substring(0, i);
    }
  }

  /**
   * Return the inner class part of the argument; that is, the part after '$'. Return the empty
   * string if there is no '$'.
   */
  private static String innerClassName(String className) {
    int i = className.indexOf('$'); // FIXME: don't split on '$' in source
    if (i == -1) {
      return "";
    } else {
      return className.substring(i);
    }
  }

  // TODO: Why is a new implementation needed, rather than using an existing one?
  /** Simple AST implementation used only in determining type paths. */
  abstract static class TypeTree implements ExpressionTree {
    private static Map<String, TypeTag> primTags = new HashMap<>();

    {
      primTags.put("byte", TypeTag.BYTE);
      primTags.put("char", TypeTag.CHAR);
      primTags.put("short", TypeTag.SHORT);
      primTags.put("long", TypeTag.LONG);
      primTags.put("float", TypeTag.FLOAT);
      primTags.put("int", TypeTag.INT);
      primTags.put("double", TypeTag.DOUBLE);
      primTags.put("boolean", TypeTag.BOOLEAN);
    }

    static TypeTree fromJCTree(JCTree jt) {
      if (jt != null) {
        Kind kind = jt.getKind();
        switch (kind) {
          case ANNOTATED_TYPE:
            return fromJCTree(((JCTree.JCAnnotatedType) jt).getUnderlyingType());
          case IDENTIFIER:
            return new IdentifierTT(((JCTree.JCIdent) jt).sym.getSimpleName().toString());
          case ARRAY_TYPE:
            return new ArrayTT(fromJCTree(((JCTree.JCArrayTypeTree) jt).getType()));
          case MEMBER_SELECT:
            return new MemberSelectTT(
                fromJCTree(((JCTree.JCFieldAccess) jt).getExpression()),
                ((JCTree.JCFieldAccess) jt).getIdentifier());
          case EXTENDS_WILDCARD:
          case SUPER_WILDCARD:
            return new WildcardTT(kind, fromJCTree(((JCTree.JCWildcard) jt).getBound()));
          case UNBOUNDED_WILDCARD:
            return new WildcardTT();
          case PARAMETERIZED_TYPE:
            com.sun.tools.javac.util.List<JCExpression> typeArgs =
                ((JCTree.JCTypeApply) jt).getTypeArguments();
            List<Tree> args = new ArrayList<>(typeArgs.size());
            for (JCTree.JCExpression typeArg : typeArgs) {
              args.add(fromJCTree(typeArg));
            }
            return new ParameterizedTypeTT(fromJCTree(((JCTree.JCTypeApply) jt).getType()), args);
          default:
            break;
        }
      }
      return null;
    }

    /** Create a TypeTree from a scene-lib Type. */
    static TypeTree fromType(final Type type) {
      switch (type.getKind()) {
        case ARRAY:
          final ArrayType atype = (ArrayType) type;
          final TypeTree componentType = fromType(atype.getComponentType());
          return new ArrayTT(componentType);
        case BOUNDED:
          final BoundedType btype = (BoundedType) type;
          final BoundedType.BoundKind bk = btype.getBoundKind();
          final String bname = btype.getName().getName();
          final TypeTree bound = fromType(btype.getBound());
          return new TypeParameterTT(bname, bk, bound);
        case DECLARED:
          final DeclaredType dtype = (DeclaredType) type;
          if (dtype.isWildcard()) {
            return new WildcardTT();
          } else {
            final String dname = dtype.getName();
            TypeTag typeTag = primTags.get(dname);
            if (typeTag == null) {
              final TypeTree base = new IdentifierTT(dname);
              TypeTree ret = base;
              List<Type> params = dtype.getTypeParameters();
              DeclaredType inner = dtype.getInnerType();
              if (!params.isEmpty()) {
                final List<Tree> typeArgs = new ArrayList<>(params.size());
                for (Type t : params) {
                  typeArgs.add(fromType(t));
                }
                ret = new ParameterizedTypeTT(base, typeArgs);
              }
              return inner == null ? ret : addPrefix(fromType(inner), ret);
            } else {
              final TypeKind typeKind = typeTag.getPrimitiveTypeKind();
              return new PrimitiveTypeTT(typeKind);
            }
          }
        default:
          throw new RuntimeException("unknown type kind " + type.getKind());
      }
    }

    /** Create a TypeTree from a javac Type. */
    static TypeTree fromJavacType(final com.sun.tools.javac.code.Type type) {
      return fromType(javacTypeToType(type));
    }

    /** Create a javac Type from a scene-lib Type. */
    static Type javacTypeToType(final com.sun.tools.javac.code.Type jtype) {
      switch (jtype.getKind()) {
        case ARRAY:
          com.sun.tools.javac.code.Type.ArrayType arraytype =
              (com.sun.tools.javac.code.Type.ArrayType) jtype;
          return new ArrayType(javacTypeToType(arraytype.elemtype));
        case DECLARED:
          {
            com.sun.tools.javac.code.Type t = jtype;
            DeclaredType d = null;
            do {
              DeclaredType d0 = d;
              com.sun.tools.javac.code.Type.ClassType ct =
                  (com.sun.tools.javac.code.Type.ClassType) t;
              d = new DeclaredType(ct.tsym.name.toString());
              d.setInnerType(d0);
              // d0 = d;
              for (com.sun.tools.javac.code.Type a : ct.getTypeArguments()) {
                d.addTypeParameter(javacTypeToType(a));
              }
              t = ct.getEnclosingType();
            } while (t.getKind() == TypeKind.DECLARED);
            return d;
          }
        case WILDCARD:
          com.sun.tools.javac.code.Type.WildcardType wildcard =
              ((com.sun.tools.javac.code.Type.WildcardType) jtype);
          if (wildcard.kind == com.sun.tools.javac.code.BoundKind.UNBOUND) {
            return new DeclaredType("?");
          }
          return new BoundedType(
              new DeclaredType(jtype.tsym.name.toString()),
              wildcard.kind,
              (DeclaredType) javacTypeToType(wildcard.bound));
        case TYPEVAR:
          {
            Type upperBound =
                javacTypeToType(((com.sun.tools.javac.code.Type.TypeVar) jtype).getUpperBound());
            if (upperBound.getKind() == Type.Kind.DECLARED) {
              return new BoundedType(
                  new DeclaredType(jtype.tsym.name.toString()),
                  BoundedType.BoundKind.EXTENDS,
                  (DeclaredType) upperBound);
            } else {
              return upperBound;
            }
          }
        case INTERSECTION:
          return new DeclaredType(jtype.tsym.erasure_field.tsym.name.toString());
        case UNION:
          // TODO
          throw new Error("UNION case not yet implemented");
          // TODO: reinstate after replacing "throw new Error()": break;
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case DOUBLE:
        case LONG:
        case SHORT:
        case FLOAT:
        case INT:
          return new DeclaredType(jtype.tsym.name.toString());
        case ERROR:
          // Return a fake declared type that corresponds to the error.
          // This ignores setup problems where some classes can't be found.
          return new DeclaredType(jtype.toString());
          // case EXECUTABLE:
          // case NONE:
          // case NULL:
          // case OTHER:
          // case PACKAGE:
          // case VOID:
        default:
          throw new Error(
              "Found unknown type: " + jtype + " (" + jtype.getKind() + "). Check your setup.");
      }
    }

    /**
     * Use prefix as a prefix for identifiers in t. For example, prefix may be a package or an outer
     * type.
     */
    private static TypeTree addPrefix(final TypeTree t, final TypeTree prefix) {
      switch (t.getKind()) {
        case IDENTIFIER:
          IdentifierTT it = (IdentifierTT) t;
          return new MemberSelectTT(prefix, it.getName());
        case MEMBER_SELECT:
          MemberSelectTT lt = (MemberSelectTT) t;
          return new MemberSelectTT(addPrefix(lt.getExpression(), prefix), lt.getIdentifier());
        case PARAMETERIZED_TYPE:
          ParameterizedTypeTT pt = (ParameterizedTypeTT) t;
          return new ParameterizedTypeTT(addPrefix(pt.getType(), prefix), pt.getTypeArguments());
        default:
          throw new IllegalArgumentException("unexpected type " + t);
      }
    }

    static final class ArrayTT extends TypeTree implements ArrayTypeTree {
      private final TypeTree componentType;

      ArrayTT(TypeTree componentType) {
        this.componentType = componentType;
      }

      @Override
      public Kind getKind() {
        return Kind.ARRAY_TYPE;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitArrayType(this, data);
      }

      @Override
      public TypeTree getType() {
        return componentType;
      }

      @Override
      public String toString() {
        return componentType + "[]";
      }
    }

    static final class MemberSelectTT extends TypeTree implements MemberSelectTree {
      private final TypeTree expr;
      private final Name name;

      MemberSelectTT(TypeTree expr, Name name) {
        this.expr = expr;
        this.name = name;
      }

      @Override
      public Kind getKind() {
        return Kind.MEMBER_SELECT;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitMemberSelect(this, data);
      }

      @Override
      public TypeTree getExpression() {
        return expr;
      }

      @Override
      public Name getIdentifier() {
        return name;
      }

      @Override
      public String toString() {
        return expr + "." + name;
      }
    }

    static final class ParameterizedTypeTT extends TypeTree implements ParameterizedTypeTree {
      private final TypeTree base;
      private final List<? extends Tree> typeArgs;

      ParameterizedTypeTT(TypeTree base, List<? extends Tree> typeArgs) {
        this.base = base;
        this.typeArgs = typeArgs;
      }

      @Override
      public Kind getKind() {
        return Kind.PARAMETERIZED_TYPE;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitParameterizedType(this, data);
      }

      @Override
      public TypeTree getType() {
        return base;
      }

      @Override
      public List<? extends Tree> getTypeArguments() {
        return typeArgs;
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder(base.toString());
        String s = "<";
        for (Tree t : typeArgs) {
          sb.append(s);
          sb.append(t.toString());
          s = ", ";
        }
        sb.append('>');
        return sb.toString();
      }
    }

    static final class PrimitiveTypeTT extends TypeTree implements PrimitiveTypeTree {
      private final TypeKind typeKind;

      PrimitiveTypeTT(TypeKind typeKind) {
        this.typeKind = typeKind;
      }

      @Override
      public Kind getKind() {
        return Kind.PRIMITIVE_TYPE;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitPrimitiveType(this, data);
      }

      @Override
      public TypeKind getPrimitiveTypeKind() {
        return typeKind;
      }

      @Override
      public String toString() {
        switch (typeKind) {
          case BOOLEAN:
            return "boolean";
          case BYTE:
            return "byte";
          case CHAR:
            return "char";
          case DOUBLE:
            return "double";
          case FLOAT:
            return "float";
          case INT:
            return "int";
          case LONG:
            return "long";
          case SHORT:
            return "short";
            // case VOID: return "void";
            // case WILDCARD: return "?";
          default:
            throw new IllegalArgumentException("unexpected type kind " + typeKind);
        }
      }
    }

    static final class IdentifierTT extends TypeTree implements IdentifierTree {
      private final String name;

      IdentifierTT(String dname) {
        this.name = dname;
      }

      @Override
      public Kind getKind() {
        return Kind.IDENTIFIER;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitIdentifier(this, data);
      }

      @Override
      public Name getName() {
        return new TypeName(name);
      }

      @Override
      public String toString() {
        return name;
      }
    }

    static final class WildcardTT extends TypeTree implements WildcardTree {
      private final TypeTree bound;
      private final Kind kind;

      WildcardTT() {
        this(Kind.UNBOUNDED_WILDCARD, null);
      }

      WildcardTT(TypeTree bound, BoundedType.BoundKind bk) {
        this(
            bk == BoundedType.BoundKind.SUPER ? Kind.SUPER_WILDCARD : Kind.EXTENDS_WILDCARD, bound);
      }

      WildcardTT(Kind kind, TypeTree bound) {
        this.kind = kind;
        this.bound = bound;
      }

      @Override
      public Kind getKind() {
        return kind;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitWildcard(this, data);
      }

      @Override
      public Tree getBound() {
        return bound;
      }

      @Override
      public String toString() {
        return "?";
      }
    }

    static final class TypeParameterTT extends TypeTree implements TypeParameterTree {
      private final String bname;
      private final BoundedType.BoundKind bk;
      private final Tree bound;

      TypeParameterTT(String bname, BoundedType.BoundKind bk, TypeTree bound) {
        this.bname = bname;
        this.bk = bk;
        this.bound = bound;
      }

      @Override
      public Kind getKind() {
        return Kind.TYPE_PARAMETER;
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
      }

      @Override
      public Name getName() {
        return new TypeName(bname);
      }

      @Override
      public List<? extends Tree> getBounds() {
        return Collections.singletonList(bound);
      }

      @Override
      public List<? extends AnnotationTree> getAnnotations() {
        return Collections.emptyList();
      }

      @Override
      public String toString() {
        return bname + " " + bk.toString() + " " + bound.toString();
      }
    }

    static final class TypeName implements Name {
      private final String str;

      TypeName(String str) {
        this.str = str;
      }

      @Override
      public int length() {
        return str.length();
      }

      @Override
      public char charAt(int index) {
        return str.charAt(index);
      }

      @Override
      public CharSequence subSequence(int start, int end) {
        return str.subSequence(start, end);
      }

      @Override
      public boolean contentEquals(CharSequence cs) {
        return str.contentEquals(cs);
      }

      @Override
      public String toString() {
        return str;
      }
    }
  }
}
