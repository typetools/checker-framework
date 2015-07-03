Contents:



# Introduction #

This page lists a few potential projects that would be a valuable extension
to the Checker Framework.  We are also open to other new ideas!  Feel free
to email us or to comment on this wiki page.

Some of these projects help developers to use the
Checker Framework in practice.  Other projects are research
that pushes the limits of technology and may be publishable.
Some of the projects have both types of impact.

In addition to the larger projects on this page, see the list of
[open issues](http://code.google.com/p/checker-framework/issues/list).  Or
add to it if you notice other problems!

**Required skills:**
These projects require facility with Java.  For most of them, you should be
very comfortable with Java's type system.  (For example, you should know
that subtyping theory permits arguments to change contravariantly (even though Java forbids it for reasons related to overloading), whereas
return values may change
[covariantly](http://en.wikipedia.org/wiki/Covariance_and_contravariance_%28computer_science%29) both in theory and in Java.)
You should know how a type system helps you and where it can hinder you,
and you should believe the tradeoff is worth making.
You should be willing to dive into and understand a moderately-sized codebase.

If you want to discuss these projects, feel free to do so on the Checker
Framework's mailing lists, or contact the Checker Framework developers
directly at checker-framework-dev@googlegroups.com.  We look forward to
hearing from you!


# Background on the Checker Framework #

Type-checking helps to detect and prevent errors. However, type-checking
doesn't prevent _enough_ errors.  Our goal is to make type-checking even
more powerful and useful to programmers, without getting in the way. For
example, Java types do not indicate whether a variable may be null, or
whether a value is intended to be side-effected. As a result, a Java
program can suffer from null pointer exceptions and incorrect side effects.

We have built the
[Checker Framework](http://types.cs.washington.edu/checker-framework/),
which permits users to define their own type systems
that can be plugged into javac.  We would like to further extend and
evaluate the Checker Framework and the type systems themselves, and to
improve their usability.

# Usability-related Projects #

## IDE support ##

The Checker Framework comes with support for a large set of
[External Tools](http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#external-tools),
including an Eclipse plug-in.

Integrate the Checker Framework with an IDE such as Eclipse, IntelliJ, IDEA or NetBeans,
which will make pluggable type-checking easier to use and more attractive to developers.

Some specific projects include:
  * Create an IDE plug-in that invokes the checker and reports any errors to the developer, just as the IDE currently does for type errors.
  * Improve the existing Eclipse plug-in.
  * Add a button to the IDE that hides/shows all annotations of a given variety, to reduce clutter.  This would be useful beyond type annotations.
  * Implement quick fixes for common errors.
  * Integrate type inference with the IDE, to make it easier for a developer to add annotations to code.
  * Improve an IDE's existing refactoring support so that it is aware of, and properly retains, type annotations.
  * Highlight defaulted types and flow-sensitive type refinements, making error messages easier to understand.
  * Highlight the parts of the code that influenced flow-sensitive type refinements, again to make errors easier to comprehend.

IDEs that build on the OpenJDK Java compiler could benefit from even tighter integration with the Checker Framework.

This project may entail the following:

  * familiarity with the IDE plug-in development environment, Eclipse PDT or NetBeans plug-in support:  By the end of the project, the developer should be familiar with building reasonably complex IDE plug-ins.

  * UI/UX design:  The developer would design a UI experience that is appealing to developers and integrates well with the developer's workflow!

Difficulty: Medium/Hard

Amount of work: Medium/Significant


## Javadoc support ##

Currently, type annotations are only displayed in Javadoc if they are explicitly written by the programmer.
However, the Checker Framework provides flexible defaulting mechanisms, reducing the annotation overhead.
This project will work on integrating the Checker Framework defaulting phase with Javadoc, showing the signatures after applying defaulting rules.

There are other type-annotation-related improvements to Javadoc that can be explored, e.g. using JavaScript to show or hide only the type annotations currently of interest.

Difficulty: Easy

Amount of work: Medium


## Universal AST ##

Migrate the Checker Framework to use a universal Abstract Syntax Tree (AST)
to represent the Java source code.  Currently, The Checker Framework is
coupled to the OpenJDK javac AST representation.  This means that it works
with Sun's javac, but not with other Java compilers.  For the Checker Framework
to work natively on other compilers and IDEs (e.g. Eclipse, IntelliJ), someone
needs to create a universal AST to abstract the various vendor ASTs and
migrate the Checker Framework to use the new AST representation.

Some effort to create a universal AST is already underway (e.g.
[Lombok AST project](http://github.com/rzwitserloot/lombok.ast)), and the
developer may leverage their work.

Complexity: Medium

Expected Amount of work: Significant


## Universal Dataflow Framework ##

In recent work, the Checker Framework implemented a Control Flow Graph CFG and Dataflow Framework to implement more precise flow-sensitive type refinement.
This abstraction is also built on the OpenJDK javac AST, but less dependencies are surfaced.
This project would investigate building a common Dataflow representation from a variety of Java ASTs.
Type checking would be moved from traversing the OpenJDK AST to this new dataflow representation.

Complexity: Medium/Hard

Amount of work: Significant


## Case study:  fixing bugs in existing programs ##

Pluggable type-checking helps you to find and fix both bugs and code
smells.  Take your favorite program, and apply one or more pluggable
type-checkers to it.  This will give you more familiarity with the use of
pluggable type-checking, it will make the codebase more robust, and it will
give you ideas for how to improve the type-checkers.

Complexity: Easy/Medium

Expected Amount of work: Medium/Significant


## Alternative to Annotated JDKs and Stub files ##

The Checker Framework currently provides two mechanisms to provide type annotations for libraries: stub files, a text-based format that is interpreted at compile time, and the Annotated JDK, a JAR file containing all type annotations.
Creating the annotated JDK requires that all possible type system annotations are available and can be merged into a single JAR file. Because each class in the JVM can be only loaded once, all type systems need to be merged into one class file.
This inhibits extensions by new type systems: the new type system needs to be integrated into the existing annotated JDK and cannot provide a stand-alone JAR file with additional annotations.

This project will explore writing a class-loader that reads in multiple jar files only for the purpose of decoding their type annotations. The same class should be searched for in all jar files, to allow easy extension by additional type systems.

Complexity: Medium

Amount of work: Medium


# Type System Projects #

## Implement a new type system ##

The Checker Framework is shipped with 10 type-checkers.  Users can
[create a new checker](http://types.cs.washington.edu/checker-framework/#writing-a-checker)
of their own.  However, some users don't want to go to that trouble, but
would like to have more type-checkers packaged with the Checker Framework
for easy use.  So, this task involves
[implementing a new type-checker](http://types.cs.washington.edu/checker-framework/#writing-a-checker).

It is easiest to choose an existing type system from the literature.

An alternative to implementing an existing type system is to design a new type
system to solve some programming problem.  This task can be simple or very
challenging, depending on how ambitious the type system is.  Remember to
focus on what helps a software developer most!

No matter how you choose the type system, you will implement it as a compiler
plug-in.  You will also perform case studies to evaluate the type system.
(This is very valuable even for an existing, well-known type system.
Because no framework such as ours existed before, experimental evaluation
of previous type systems has left much to be discovered.)

Complexity: Easy to Hard -- depending on the proposed type system

Expected Amount of work: Medium/Significant


## Stateful type systems ##

This project is to improve support for
[typestate checking](http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#typestate-checker).

Ordinarily, a program variable has
the same type throughout its lifetime from when the variable is declared
until it goes out of scope. “Typestate”
permits the type of an object or variable to _change_ in a controlled way.
Essentially, it is a combination of standard type systems with dataflow
analysis. For instance, a file object changes from unopened, to opened, to
closed; certain operations such as writing to the file are only permitted
when the file is in the opened typestate. Another way of saying this is
that <tt>write</tt> is permitted after <tt>open</tt>, but not after <tt>close</tt>.
Typestate
is applicable to many other types of software properties as well.

Two [typestate checking frameworks](http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#typestate-checker)
exist for the Checker Framework.  They need to be improved, however.

Complexity: Medium/High

Expected Amount of work: Medium/Significant


## Run-time checking ##

Implement run-time checking to complement compile-time checking.  This will
let users combine the power of static checking with that of dynamic
checking.

Every type system is too strict: it rejects some programs that never go
wrong at run time. A human must insert a type loophole to make such a
program type-check.  For example, Java takes this approach with its
cast operation (and in some other places).

When doing type-checking, it is desirable to automatically insert run-time
checks at each operation that the static checker was unable to verify.
(Again, Java takes exactly this approach.)  This guards against mistakes by
the human who inserted the type loopholes.  A nice property of this
approach is that it enables you to prevent errors in a program
with no type annotations:  whenever the static checker is unable
to verify an operation, it would insert a dynamic check.  Run-time checking
would also be useful in verifying whether the suppressed warnings are
correct -- whether the programmer made a mistake when writing them.

The annotation processor (the pluggable type-checker) should automatically
insert the checks, as part of the compilation process.

There should be various modes for the run-time checks:
  * fail immediately.
  * logging, to permit post-mortem debugging without crashing the program.

The run-time penalty should be small:  a run-time check is necessary only
at the location of each cast or suppressed warning.  Everywhere that the
compile-time checker reports no possible error, there is no need to insert a
check.  But, it will be an interesting project to determine how to minimize
the run-time cost.

Another interesting, and more challenging, design question is whether you need to add and maintain
a run-time representation of the property being tested.  It's easy to test
whether a particular value is null, but how do you test whether it is
tainted, or should be treated as immutable?  For a more concrete example,
see the discussion of the (not yet implemented)
[Javari run-time checker](http://pag.csail.mit.edu/pubs/ref-immutability-oopsla2005-abstract.html).
Adding this run-time support would be an interesting and challenging project.

We developed a prototype for the
[EnerJ runtime system](https://ece.uwaterloo.ca/~wdietl/publications/pubs/EnerJ11-abstract.html).
That code could be used as starting point.

In the short term, this could be prototyped as a source- or
bytecode-rewriting approach; but integrating it into the type checker is a
better long-term implementation strategy.

Complexity: Medium

Expected Amount of work: Medium/Significant


## Type inference ##

A custom type-checker is of no use unless the code is annotated with type
qualifiers.  It is tedious for a developer to add type annotations
throughout a codebase.  It would be better for a tool to insert
annotations, and a developer can then tweak them.  The goal of this project
is to create such a tool that does most or all of the annotation work for
the developer.  It can be used on libraries or on application code.

Some inference systems exist, but there are not enough of them, and all
existing ones have some weaknesses.  So, you can either improve an existing
inference tool, or build a new one from scratch.  This can be both an
implementation challenge and/or a research challenge, depending on the type
system that you target.

  * Javari:
> > The Javarifier tool ([code](http://code.google.com/p/javarifier/),
> > [manual](http://groups.csail.mit.edu/pag/javari/javarifier/)) infers type
> > annotations for the
> > [Javari](http://types.cs.washington.edu/checker-framework/#javari-checker)
> > type system.  Javari gives guarantees that your code does not
> > make modifications that it should not.  Javarifier needs to be updated
> > to use newer versions of some libraries (a great starter task), then
> > there are several substantial architectural changes or enhancements that
> > are possible.

  * Nullness:
> > Several inference tools for
> > [nullness](http://types.cs.washington.edu/checker-framework/#nullness-inference)
> > exist, but all have shortcomings (and some need a maintainer).
> > You can extend the precise dynamic tool to infer more varieties of
> > annotation, or can extend a conservative static tool to be more precise.

  * IGJ:
> > [IGJ](http://types.cs.washington.edu/checker-framework/#igj-checker) is
> > another, even more powerful system for immutability (that is, for
> > controlling side effects).  It has no type inference system, but would
> > benefit from one.

  * Another type system:
> > Choose any type system that you are interested in, either from the
> > [Checker Framework manual](http://types.cs.washington.edu/checker-framework/),
> > or from research papers, or from your own experience.  Build
> > an inference system for it.

A very ambitious project that we would like to take on -- after building a few
specific type inference tools -- is to build a general framework for type
inference tools.  Before the Checker Framework, implementing a custom type
system was a major undertaking, so few developers or researchers did so,
despite the benefits.  Now, the Checker Framework has made the task much
easier.  Likewise, we would like to design a framework that makes creating
a type inference easier.  (This project has stymied numerous researchers.
But, on the other hand, many groups had failed at the project of building a
framework for type-checking, yet we were successful with the Checker
Framework.)

Complexity: Medium/High

Expected Amount of work: Medium/Significant


## Bytecode verification framework ##

Did you know that your Java code gets type-checked twice before it gets
run?  First, it is type-checked by the Java compiler such as javac, and
is converted into byte-codes.  Second, when the JVM loads a class file,
the JVM type-checks the bytecodes before executing them.  Furthermore, all
of the safety and security guarantees of the JVM depend purely on the
second, bytecode validation step.  The first step is purely a convenience
for the programmer, and has no bearing on the correctness or safety of your
program's execution.  The second step is necessary because the JVM may be
asked to load arbitrary bytecodes that may come from an untrusted source;
the JVM can't be sure that the bytecodes were created by a correct and
trusted compiler.

The practical consequence of this is that type-checking source code offers
no guarantees about a program's execution, since the program could be run alongside
code that has not been type-checked for the same properties.  It is
necessary to type-check the bytecodes.

More generally, the goal of this project is to create a general bytecode
verification framework, in which it will be easy for a JVM, a classloader,
or other tools to verify certain properties of their programs.  In
particular, the designers of pluggable type checkers should be able to
easily extend the bytecode verification by the additional checks they need.
The combination of a pluggable type checker, the bytecode verifier, and the
corresponding runtime checks will then ensure type safety over all phases
of the program.

An example of a previous attempt appears in the OOPSLA 2004 paper
["Pluggable verification modules: An extensible protection mechanism for the JVM"](http://www.cs.uregina.ca/Research/Techreports/2003-11.pdf).

Complexity: High

Expected Amount of work: Significant


## Model checking of a type system ##

Design and implement an algorithm to check type soundness of a type system
by exhaustively verifying the type checker on all programs up to a certain
size. The challenge lies in efficient enumeration of all programs and
avoiding redundant checks. This approach is related to bounded exhaustive
testing and model checking, which was recently applied to type
systems. For a reference, see
[Efficient Software Model Checking of Soundness of Type Systems](http://www.eecs.umich.edu/~bchandra/publications/oopsla08.pdf).

By integrating support for model-checking a type system into the declarative mechanisms of describing a type system in the Checker Framework, developing a new type system would be significantly simplified.

Complexity: High

Expected Amount of work: Significant


## Compiler optimizations ##

The key purpose of type annotations is to help programmers to find -- and,
more importantly, to prevent! -- errors.  But, they are useful for a
variety of other purposes.  For example, they can be used by an optimizing
compiler or run-time system.  If there is a static guarantee that a value
is non-null, it does not need to be checked.  If there is as static
guarantee that a value will not change, or is not aliased, then it can be
safely cached.  Many other possibilities exist.

Complexity: Medium/High

Expected Amount of work: Significant


## Java 8 language features ##

The Checker Framework currently supports all Java 7 language features, but new Java 8 language features were developed in parallel and are not supported yet.

A simpler example is improved target-type inference of type arguments (diamond operator), which will require an interesting re-architecture of Checker Framework features.

A much bigger change is full support for Project Lambda: how can type annotations be inferred for lambda functions? What type checks should be performed? What defaulting should we use?

Complexity: Medium/High

Expected Amount of work: Significant




<a href='Hidden comment: 
LocalWords:  wiki IGJ Stateful tt mortem UX workflow javari igj classloader toc
LocalWords:  contravariantly Nullness nullness Lombok
'></a>