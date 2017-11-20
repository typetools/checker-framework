package org.checkerframework.eclipse.util;

import static org.checkerframework.eclipse.util.JavaUtils.iterable;
import static org.eclipse.core.resources.IResource.FILE;
import static org.eclipse.core.resources.IResource.FOLDER;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ResourceUtils {

    private ResourceUtils() {
        throw new AssertionError("Shouldn't be initialized");
    }

    /**
     * @param relativePath workspace relative path
     * @return given path if path is not known in workspace
     */
    public static IPath relativeToAbsolute(IPath relativePath) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(relativePath);
        return (resource != null) ? resource.getLocation() : relativePath;
    }

    /**
     * Returns a list of all files in a resource delta. This is of help when performing an
     * incremental build.
     *
     * @return Collection A list of all files to be built.
     */
    public static List<IResource> collectIncremental(IResourceDelta delta) {
        // XXX deleted packages should be considered to remove markers
        List<IResource> result = new ArrayList<IResource>();
        List<IResourceDelta> foldersDelta = new ArrayList<IResourceDelta>();

        for (IResourceDelta childDelta : delta.getAffectedChildren()) {
            IResource child = childDelta.getResource();
            if (child.isDerived()) {
                continue;
            }
            int childType = child.getType();
            int deltaKind = childDelta.getKind();

            if (childType == FILE) {
                if ((deltaKind == IResourceDelta.ADDED || deltaKind == IResourceDelta.CHANGED)
                        && Util.isJavaArtifact(child)) {
                    result.add(child);
                }
            } else if (childType == FOLDER) {
                if (deltaKind == IResourceDelta.ADDED) {
                    result.add(child);
                } else if (deltaKind == IResourceDelta.REMOVED) {
                    // TODO should just remove markers....
                    IContainer parent = child.getParent();
                    if (parent instanceof IProject) {
                        // have to recompute entire project if one of root
                        // folders is removed
                        result.clear();
                        result.add(parent);
                        return result;
                    }
                    result.add(parent);
                } else {
                    foldersDelta.add(childDelta);
                }
            }
        }

        for (IResourceDelta childDelta : foldersDelta) {
            result.addAll(collectIncremental(childDelta));
        }
        return result;
    }

    /**
     * Convenient method to get resources from adaptables
     *
     * @param element an IAdaptable object which may provide an adapter for IResource
     * @return resource object or null
     */
    public static IResource getResource(Object element) {
        if (element instanceof IResource) {
            return (IResource) element;
        }

        if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            return (IResource) adaptable.getAdapter(IResource.class);
        }

        return null;
    }

    /**
     * Collects and combines the selection which may contain sources from different projects and /
     * or multiple sources from same project.
     *
     * <p>If selection contains hierarchical data (like file and its parent directory), the only
     * topmost element is returned (same for directories from projects).
     *
     * <p>The children from selected parents are not resolved, so that the return value contains the
     * 'highest' possible hierarchical elements without children.
     *
     * @param structuredSelection
     * @return a map with the project as a key and selected resources as value. If project itself
     *     was selected, then key is the same as value.
     */
    public static Map<IProject, List<IResource>> getResourcesPerProject(
            IStructuredSelection structuredSelection) {

        Map<IProject, List<IResource>> projectsMap = new HashMap<IProject, List<IResource>>();

        @SuppressWarnings("unchecked")
        Iterable<?> iterable = iterable(structuredSelection.iterator());

        for (Object element : iterable) {
            IResource resource = getResource(element);
            mapResource(resource, projectsMap, false);
        }

        return projectsMap;
    }

    /** Maps the resource into its project */
    private static void mapResource(
            IResource resource,
            Map<IProject, List<IResource>> projectsMap,
            boolean checkJavaProject) {

        if (resource.getType() == FILE && !Util.isJavaArtifact(resource)) {
            // Ignore non java files
            return;
        }

        IProject project = resource.getProject();
        if (checkJavaProject && !Util.isJavaProject(project)) {
            // non java projects: can happen only for changesets
            return;
        }

        List<IResource> resources = projectsMap.get(project);
        if (resources == null) {
            resources = new ArrayList<IResource>();
            projectsMap.put(project, resources);
        }

        // do not need to check for duplicates, cause user cannot select
        // the same element twice
        if (!containsParents(resources, resource)) {
            resources.add(resource);
        }
    }

    /**
     * @param resources
     * @param candidate
     * @return true if the given list contains at least one parent of the given candidate
     */
    private static boolean containsParents(List<IResource> resources, IResource candidate) {

        IPath location = candidate.getLocation();
        for (IResource resource : resources) {
            if (resource.getType() == FILE) {
                continue;
            }

            IPath resourceLoc = resource.getLocation();
            if (resourceLoc != null && resourceLoc.isPrefixOf(location)) return true;
        }
        return false;
    }

    public static IWorkspaceRoot workspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    public static Set<String> sourceFilesOf(IJavaElement element) throws CoreException {
        final Set<String> fileNames = new LinkedHashSet<String>();

        for (ICompilationUnit cu : Util.getAllCompilationUnits(element)) {
            fileNames.add(cu.getResource().getLocation().toOSString());
        }
        return fileNames;
    }

    public static Set<String> sourceFilesOf(List<IJavaElement> elements) throws CoreException {
        final Set<String> fileNames = new LinkedHashSet<String>();
        for (final IJavaElement element : elements) {
            final Set<String> files = sourceFilesOf(element);
            for (String file : files) {
                if (file.endsWith(".java")) {
                    fileNames.add(file);
                }
            }
        }

        return fileNames;
    }

    /**
     * Get the specified project file as an Eclipse resource.
     *
     * <p>Returns null if the file isn't found.
     */
    public static IResource getFile(IJavaProject jProject, File file) {
        IProject project = jProject.getProject();
        IPath filePath = Path.fromOSString(file.getPath());
        int segCount = project.getLocation().segmentCount();

        return project.findMember(filePath.removeFirstSegments(segCount));
    }

    /** Returns the path of the output directory of the project */
    public static String outputLocation(IClasspathEntry cp, IJavaProject project) {

        IPath out = cp.getOutputLocation();

        if (out == null) {
            try {
                out = project.getOutputLocation();

                // TODO: THERE HAS TO BE A BETTER WAY TO DO THIS BECAUSE THIS IS KLUDGERIFFIC
                if (out != null) {

                    String path =
                            project.getProject().getLocation().toOSString()
                                    + File.separator
                                    + ".."
                                    + File.separator
                                    + project.getOutputLocation().toOSString();

                    return new File(path).getCanonicalPath();
                }

            } catch (final Exception exc) {
                throw new RuntimeException(exc);
            }
        } else {
            return out.toOSString();
        }

        // location is null if the classpath entry outputs to the 'default'
        // location, i.e. project
        IFile outDir = ResourceUtils.workspaceRoot().getFile(cp.getPath());
        return outDir.getLocation().toOSString();
    }
}
