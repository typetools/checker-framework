# Replace FlowExpression Parser
Currently, the CheckerFramework uses Regex to parse expressions/arguments given to Annotations. However, to make it more generalizable and robust, an alternative using JavaParser is demonstrated here.

### Changes:
In the [FlowExpressionParseUtil.java](https://github.com/KankshaZ/checker-framework/blob/ReplaceFlowExpressionParser/framework/src/main/java/org/checkerframework/framework/util/FlowExpressionParseUtil.java) file, the following patterns have been created from regular expressions
- unanchoredParameterPattern
- parameterPattern
- identifierPattern
- intPattern
- longPattern
- stringPattern
- parenthesisPattern
- memberSelectOfStringPattern
- pattern for indentParser

These regular expressions used for matching have been replaced with calls to the JavaParser. 
Since the unanchoredParameterPattern and parameterPattern are non-standard expressions, they have not been replaced.

Run all tests with 

    ./gradlew allTests 

Every test gets completed.

### Run (to see changes):

    git checkout ReplaceFlowExpressionParser
    cd $JSR308/checker-framework
    git diff master..ReplaceFlowExpressionParser -- framework/src/main/java/org/checkerframework/framework/util/FlowExpressionParseUtil.java


### Working:
The JavaParser's quick API is used. Static methods parse fragments of source code which are Strings. For example:

    Expression e = parseExpression("this");


### Note:
Replacement of unanchoredParameterPattern and parameterPattern is not done since they are non-standard Java Expressions. Occurences of parameters are replaced by arguments, and then replaced back before the expression is finally returned.


Please see the Checker Framework manual ([HTML](https://checkerframework.org/manual/), [PDF](https://checkerframework.org/manual/checker-framework-manual.pdf)).

Additional documentation for Checker Framework developers
is in directory `docs/developer/`.
