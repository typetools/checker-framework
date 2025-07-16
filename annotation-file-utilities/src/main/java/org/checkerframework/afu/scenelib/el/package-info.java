/**
 * <code>annotations.el</code> provides classes that associate annotations with Java elements.
 * {@link org.checkerframework.afu.scenelib.el.AElement}s represent Java elements of the scene that
 * can carry annotations. There is a multi-level class hierarchy for elements that exploits certain
 * commonalities: for example, all and only {@link org.checkerframework.afu.scenelib.el.ADeclaration
 * declarations} contain an {@link org.checkerframework.afu.scenelib.el.ADeclaration#insertTypecasts
 * insertTypecasts} field. A &ldquo;scene&rdquo; ({@link
 * org.checkerframework.afu.scenelib.el.AScene}) contains many elements and represents all the
 * annotations on a set of classes and packages.
 *
 * <p>One related utility class that is important to understand is {@link
 * org.checkerframework.afu.scenelib.util.coll.VivifyingMap}, a Map implementation that allows empty
 * entries (for some user-defined meaning of {@link
 * org.checkerframework.afu.scenelib.util.coll.VivifyingMap#isEmpty() empty}) and provides a {@link
 * org.checkerframework.afu.scenelib.util.coll.VivifyingMap#prune() prune} method to eliminate these
 * entries. The only way to create any element is to invoke {@link
 * org.checkerframework.afu.scenelib.util.coll.VivifyingMap#getVivify(Object) getVivify()} on a
 * {@link org.checkerframework.afu.scenelib.util.coll.VivifyingMap} static member of the appropriate
 * {@link org.checkerframework.afu.scenelib.el.AElement} superclass.
 */
package org.checkerframework.afu.scenelib.el;
