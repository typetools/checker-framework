## The Checker Framework homepage is ##
# http://checker-framework.org/ #

The Checker Framework enhances Java's type system to make it more powerful and useful. This lets software developers detect and prevent errors in their Java programs.

The Checker Framework comes with checkers for common errors.  This is a
partial list, since we keep adding new ones; see the
[manual](http://types.cs.washington.edu/checker-framework/current/checkers-manual.html) for a longer list.
  * null pointer errors
  * concurrency bugs: locking synchronization
  * security bugs:  tainting (information flow a la SQL injection), encryption
  * localization errors:  non-internationalized strings
  * string formatting errors:  incorrect regular expressions, wrong property keys
  * mutation errors:  incorrect side effects
  * ... and [more](http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#introduction)

The Checker Framework enables you to write new type-checkers of your own.

All of the checkers are easy to use and are invoked via an IDE or directly
by javac's standard `-processor` switch.