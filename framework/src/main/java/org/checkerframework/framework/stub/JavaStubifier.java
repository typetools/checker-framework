package org.checkerframework.framework.stub;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Process Java source files in a directory to produce, in-place, minimal stub files.
 *
 * <p>To process a file means to remove:
 *
 * <ol>
 *   <li>everything that is private or package-private,
 *   <li>all comments, except for an initial copyright header,
 *   <li>all method bodies,
 *   <li>all field initializers,
 *   <li>all initializer blocks,
 *   <li>attributes to the {@code Deprecated} annotation (to be Java 8 compatible).
 * </ol>
 */
public class JavaStubifier {
    /**
     * Processes each provided command-line argument; see class documentation for details.
     *
     * @param args command-line arguments: directories to process
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: provide one or more directory names to process");
            System.exit(1);
        }
        for (String arg : args) {
            process(arg);
        }
    }

    /**
     * Process each file in the given directory; see class documentation for details.
     *
     * @param dir directory to process
     */
    private static void process(String dir) {
        Path root = Paths.get(dir);
        MinimizerCallback mc = new MinimizerCallback();
        ProjectRoot projectRoot = new ParserCollectionStrategy().collect(root);

        projectRoot
                .getSourceRoots()
                .forEach(
                        sourceRoot -> {
                            try {
                                sourceRoot.parse("", mc);
                            } catch (IOException e) {
                                System.err.println("IOException: " + e);
                            }
                        });
    }

    /** Callback to process each Java file; see class documentation for details. */
    private static class MinimizerCallback implements SourceRoot.Callback {
        /** The visitor instance. */
        private final MinimizerVisitor mv;

        /** Create a MinimizerCallback instance. */
        public MinimizerCallback() {
            this.mv = new MinimizerVisitor();
        }

        @Override
        public Result process(
                Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
            Result res = Result.SAVE;
            // System.out.printf("Minimizing %s%n", absolutePath);
            Optional<CompilationUnit> opt = result.getResult();
            if (opt.isPresent()) {
                CompilationUnit cu = opt.get();
                // Only remove the "contained" comments so that the copyright comment is not
                // removed.
                cu.getAllContainedComments().forEach(Node::remove);
                mv.visit(cu, null);
                if (cu.findAll(ClassOrInterfaceDeclaration.class).isEmpty()
                        && cu.findAll(AnnotationDeclaration.class).isEmpty()
                        && cu.findAll(EnumDeclaration.class).isEmpty()
                        && !absolutePath.endsWith("package-info.java")) {
                    // All content is removed, delete this file.
                    new File(absolutePath.toUri()).delete();
                    res = Result.DONT_SAVE;
                }
            }
            return res;
        }
    }

    /** Visitor to process one compilation unit; see class documentation for details. */
    private static class MinimizerVisitor extends ModifierVisitor<Void> {
        /** Whether to consider members implicitly public. */
        private boolean implicitlyPublic = false;

        @Override
        public ClassOrInterfaceDeclaration visit(ClassOrInterfaceDeclaration cid, Void arg) {
            boolean prevIP = implicitlyPublic;
            if (cid.isInterface()) {
                // All members of interfaces are implicitly public.
                implicitlyPublic = true;
            }
            super.visit(cid, arg);
            if (cid.isInterface()) {
                implicitlyPublic = prevIP;
            }
            // Do not remove private or package-private classes, because there could
            // be externally-visible members in externally-visible subclasses.
            return cid;
        }

        @Override
        public EnumDeclaration visit(EnumDeclaration ed, Void arg) {
            super.visit(ed, arg);
            // Enums can't be extended, so it is ok to remove them if they are not externally
            // visible.
            removeIfPrivateOrPkgPrivate(ed);
            return ed;
        }

        @Override
        public ConstructorDeclaration visit(ConstructorDeclaration cd, Void arg) {
            super.visit(cd, arg);
            // Constructors cannot be overridden, so it is ok to remove them if they are
            // not externally visible.
            if (!removeIfPrivateOrPkgPrivate(cd)) {
                // ConstructorDeclaration has to have a body
                cd.setBody(new BlockStmt());
            }
            return cd;
        }

        @Override
        public MethodDeclaration visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            // Non-private methods could be overriden with larger visibility.
            // So it is only safe to remove private methods, which can't be overridden.
            if (!removeIfPrivate(md)) {
                md.removeBody();
            }
            return md;
        }

        @Override
        public FieldDeclaration visit(FieldDeclaration fd, Void arg) {
            super.visit(fd, arg);
            // It is safe to remove fields that are not externally visible.
            if (!removeIfPrivateOrPkgPrivate(fd)) {
                fd.getVariables().forEach(v -> v.getInitializer().ifPresent(Node::remove));
            }
            return fd;
        }

        @Override
        public InitializerDeclaration visit(InitializerDeclaration id, Void arg) {
            super.visit(id, arg);
            id.remove();
            return id;
        }

        @Override
        public NormalAnnotationExpr visit(NormalAnnotationExpr nae, Void arg) {
            super.visit(nae, arg);
            NodeList<MemberValuePair> empty = new NodeList<>();
            if (nae.getNameAsString().equals("Deprecated")) {
                nae.setPairs(empty);
            }
            return nae;
        }

        /**
         * Remove the whole node if it is private or package private.
         *
         * @param node a Node to inspect
         * @return true if the node was removed
         */
        private boolean removeIfPrivateOrPkgPrivate(NodeWithAccessModifiers<?> node) {
            if (implicitlyPublic) {
                return false;
            }
            AccessSpecifier as = node.getAccessSpecifier();
            if (as == AccessSpecifier.PRIVATE || as == AccessSpecifier.PACKAGE_PRIVATE) {
                ((Node) node).remove();
                return true;
            }
            return false;
        }

        /**
         * Remove the whole node if it is private.
         *
         * @param node a Node to inspect
         * @return true if the node was removed
         */
        private boolean removeIfPrivate(NodeWithAccessModifiers<?> node) {
            if (implicitlyPublic) {
                return false;
            }
            AccessSpecifier as = node.getAccessSpecifier();
            if (as == AccessSpecifier.PRIVATE) {
                ((Node) node).remove();
                return true;
            }
            return false;
        }
    }
}
