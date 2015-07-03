The Checker Framework source code (and related projects) contains
`.project` and `.classpath` files to make it easy to edit the source code
in the Eclipse IDE.  This document tells you how to set up your Eclipse.


# Checkout #

Check out these repositories, into sibling directories:
  * jsr308-langtools
  * checker-framework
  * (optional) annotation-tools
  * (optional) javarifier

Build them all.


# Create javaparser project #

File > New > Java Project

Name: javaparser

Contents:  create new project in workspace:  checker-framework/javaparser

# Create jsr308-langtools project #

File > New > Java Project

Name: jsr308-langtools

Contents:  create new project in workspace:  jsr308-langtools

# Create checkers project #

File > New > Java Project

Name: checkers

Contents:  create new project in workspace:  checker-framework/checkers

(Current problem: this doesn't build because the "langtools-ta" project
can't be found.)

There will be a compilation error in NullnessUtils.java.
Right-click that file, then select "Build Path > Exclude".
Any changes to it will not be noticed by Eclipse; you'll have to compile
via ant if it changes.

Create a way to run a specific checker:
Click down arrow next to green "run" arrow, then choose "run configurations".
Select JUnit
Click upper-left-hand-corner "new" icon.
"New\_configuration" => "Nullness"
Test class: com.sun.tools.javac.Main
Arguments tab:
> -processor checkers.nullness.NullnessChecker FILE\_TO\_CHECK.java
Classpath tab:
> bootstrap entries > add projects > jsr308-langtools
> reorder JRE below jsr308-langtools

Run all the tests:
Click down arrow next to green "run" arrow, then choose "run configurations".
Choose Junit, then
click upper-left-hand-corner "new" icon.
Name it "all tests"
select "run all tests"
Test runner: junit 4
Classpath:
> bootstrap entries > add project > jsr308-langtools
> put jsr308-langtools above JRE
Run it and see the green bar

# Subroutine: remove entries from source path #

In the project properties, choose Java Build Path, then the Source tab.
Select all the folders with a little box.
Click "remove from Java build path" (which really ought to be a button).