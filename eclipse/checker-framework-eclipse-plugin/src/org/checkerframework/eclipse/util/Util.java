package org.checkerframework.eclipse.util;

import java.util.*;
import org.checkerframework.eclipse.CheckerPlugin;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

/**
 * Eclipse-specific utilities.
 *
 * <p>Copied from FindBugs.
 *
 * @author Phil Crosby
 * @author Peter Friese
 */
public class Util {
    public static final String NL = System.getProperty("line.separator");

    private static boolean isFileWithExtension(IResource resource, String ext) {
        return (resource != null
                && resource.getType() == IResource.FILE
                && ext.equalsIgnoreCase(resource.getFileExtension()));
    }

    /**
     * Checks whether the given resource is a Java source file.
     *
     * @param resource The resource to check.
     * @return {@code true} if the given resource is a Java source file, {@code false} otherwise.
     */
    public static boolean isJavaFile(IResource resource) {
        return isFileWithExtension(resource, "java");
    }

    /**
     * Checks whether the given resource is a Java class file.
     *
     * @param resource The resource to check.
     * @return {@code true} if the given resource is a class file, {@code false} otherwise.
     */
    public static boolean isClassFile(IResource resource) {
        return isFileWithExtension(resource, "class");
    }

    /**
     * Checks whether the given resource is a Java artifact (i.e. either a Java source file or a
     * Java class file).
     *
     * @param resource The resource to check.
     * @return {@code true} if the given resource is a Java artifact. {@code false} otherwise.
     */
    public static boolean isJavaArtifact(IResource resource) {
        return isJavaFile(resource) || isClassFile(resource);
    }

    public static boolean isJavaProject(IProject project) {
        try {
            return project != null && project.isOpen() && project.hasNature(JavaCore.NATURE_ID);
        } catch (CoreException e) {
            CheckerPlugin.logException(e, "couldn't determine project nature");
            return false;
        }
    }

    /**
     * Get all compilation units of a selection.
     *
     * @param javaElements the selected java elements
     * @return all compilation units containing and contained in elements from javaElements
     * @throws JavaModelException
     */
    public static ICompilationUnit[] getAllCompilationUnits(IJavaElement... javaElements)
            throws JavaModelException {
        Set<ICompilationUnit> result = new LinkedHashSet<ICompilationUnit>();
        for (int i = 0; i < javaElements.length; i++) {
            addAllCus(result, javaElements[i]);
        }
        return result.toArray(new ICompilationUnit[result.size()]);
    }

    private static void addAllCus(Set<ICompilationUnit> collector, IJavaElement javaElement)
            throws JavaModelException {
        switch (javaElement.getElementType()) {
            case IJavaElement.JAVA_PROJECT:
                IJavaProject javaProject = (IJavaProject) javaElement;
                IPackageFragmentRoot[] packageFragmentRoots = javaProject.getPackageFragmentRoots();
                for (int i = 0; i < packageFragmentRoots.length; i++)
                    addAllCus(collector, packageFragmentRoots[i]);
                return;

            case IJavaElement.PACKAGE_FRAGMENT_ROOT:
                IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) javaElement;
                if (packageFragmentRoot.getKind() != IPackageFragmentRoot.K_SOURCE) return;
                IJavaElement[] packageFragments = packageFragmentRoot.getChildren();
                for (int j = 0; j < packageFragments.length; j++)
                    addAllCus(collector, packageFragments[j]);
                return;

            case IJavaElement.PACKAGE_FRAGMENT:
                IPackageFragment packageFragment = (IPackageFragment) javaElement;
                collector.addAll(Arrays.asList(packageFragment.getCompilationUnits()));
                return;

            case IJavaElement.COMPILATION_UNIT:
                collector.add((ICompilationUnit) javaElement);
                return;

            default:
                ICompilationUnit cu =
                        (ICompilationUnit) javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
                if (cu != null) collector.add(cu);
        }
    }
}
