/**
 * The test case reveals issues in the url90b6689baa_adsonrocha_PD321_tgz-pJ8-MyFuzzyLiteClassJ8
 * project from the NJR dataset, where the lack of the
 * MustCallConsistencyAnalyzer::shouldTrackInvocationResult bailout check leads to exponential
 * growth in complexity. This results from an unchecked accumulation of obligations to track across
 * multiple control-flow paths, originally suspected to cause non-termination.
 */
import java.util.ArrayList;
import java.util.List;

public class FuzzyEngine {
  private List<InputVariable> inputVariables;
  private List<OutputVariable> outputVariables;
  private List<RuleBlock> ruleBlocks;

  public FuzzyEngine() {
    inputVariables = new ArrayList<>();
    outputVariables = new ArrayList<>();
    ruleBlocks = new ArrayList<>();
  }

  public boolean isReady(StringBuilder message) {
    message.setLength(0);
    if (inputVariables.isEmpty()) {
      message.append("- Engine has no input variables\n");
    }
    for (int i = 0; i < inputVariables.size(); ++i) {
      InputVariable inputVariable = inputVariables.get(i);
      if (inputVariable == null) {
        message.append(String.format("- Engine has a null input variable at index <%d>\n", i));
      } else if (inputVariable.getTerms().isEmpty()) {
        // ignore because sometimes inputs can be empty: takagi-sugeno/matlab/slcpp1.fis
        // message.append(String.format("- Input variable <%s> has no terms\n",
        // inputVariable.getName()));
      }
    }

    if (outputVariables.isEmpty()) {
      message.append("- Engine has no output variables\n");
    }
    for (int i = 0; i < outputVariables.size(); ++i) {
      OutputVariable outputVariable = outputVariables.get(i);
      if (outputVariable == null) {
        message.append(String.format("- Engine has a null output variable at index <%d>\n", i));
      } else {
        if (outputVariable.getTerms().isEmpty()) {
          message.append(
              String.format("- Output variable <%s> has no terms\n", outputVariable.getName()));
        }
        Defuzzifier defuzzifier = outputVariable.getDefuzzifier();
        if (defuzzifier == null) {
          message.append(
              String.format(
                  "- Output variable <%s> has no defuzzifier\n", outputVariable.getName()));
        } else if (defuzzifier instanceof IntegralDefuzzifier
            && outputVariable.fuzzyOutput().getAccumulation() == null) {
          message.append(
              String.format(
                  "- Output variable <%s> has no Accumulation\n", outputVariable.getName()));
        }
      }
    }

    if (ruleBlocks.isEmpty()) {
      message.append("- Engine has no rule blocks\n");
    }
    for (int i = 0; i < ruleBlocks.size(); ++i) {
      RuleBlock ruleBlock = ruleBlocks.get(i);
      if (ruleBlock == null) {
        message.append(String.format("- Engine has a null rule block at index <%d>\n", i));
      } else {
        if (ruleBlock.getRules().isEmpty()) {
          message.append(String.format("- Rule block <%s> has no rules\n", ruleBlock.getName()));
        }
        int requiresConjunction = 0;
        int requiresDisjunction = 0;
        for (Rule rule : ruleBlock.getRules()) {
          if (rule == null) {
            message.append(
                String.format(
                    "- Rule block <%s> has a null rule at index <%d>\n",
                    ruleBlock.getName(), ruleBlocks.indexOf(rule)));
          } else {
            int thenIndex = rule.getText().indexOf(" " + Rule.FL_THEN + " ");
            if (rule.getText().indexOf(" " + Rule.FL_AND + " ") < thenIndex) {
              ++requiresConjunction;
            }
            if (rule.getText().indexOf(" " + Rule.FL_OR + " ") < thenIndex) {
              ++requiresDisjunction;
            }
          }
        }
        if (requiresConjunction > 0 && ruleBlock.getConjunction() == null) {
          message.append(
              String.format("- Rule block <%s> has no Conjunction\n", ruleBlock.getName()));
          message.append(
              String.format(
                  "- Rule block <%s> has %d rules that require " + "Conjunction\n",
                  ruleBlock.getName(), requiresConjunction));
        }
        if (requiresDisjunction > 0 && ruleBlock.getDisjunction() == null) {
          message.append(
              String.format("- Rule block <%s> has no Disjunction\n", ruleBlock.getName()));
          message.append(
              String.format(
                  "- Rule block <%s> has %d rules that require " + "Disjunction\n",
                  ruleBlock.getName(), requiresDisjunction));
        }
        if (ruleBlock.getActivation() == null) {
          message.append(
              String.format("- Rule block <%s> has no Activation\n", ruleBlock.getName()));
        }
      }
    }
    return message.length() == 0;
  }

  // Inner classes and interfaces definitions

  private interface Defuzzifier {}

  private static class IntegralDefuzzifier implements Defuzzifier {
    // Implementations specific to Integral Defuzzifier
  }

  private class InputVariable {
    private List<String> terms;
    private String name;

    public List<String> getTerms() {
      return terms;
    }

    public String getName() {
      return name;
    }
  }

  private class OutputVariable {
    private List<String> terms;
    private String name;
    private Defuzzifier defuzzifier;
    private FuzzyOutput fuzzyOutput;

    public List<String> getTerms() {
      return terms;
    }

    public String getName() {
      return name;
    }

    public Defuzzifier getDefuzzifier() {
      return defuzzifier;
    }

    public FuzzyOutput fuzzyOutput() {
      return fuzzyOutput;
    }
  }

  private class FuzzyOutput {
    private Accumulation accumulation;

    public Accumulation getAccumulation() {
      return accumulation;
    }
  }

  private class RuleBlock {
    private List<Rule> rules;
    private String name;
    private Conjunction conjunction;
    private Disjunction disjunction;
    private Activation activation;

    public List<Rule> getRules() {
      return rules;
    }

    public String getName() {
      return name;
    }

    public Conjunction getConjunction() {
      return conjunction;
    }

    public Disjunction getDisjunction() {
      return disjunction;
    }

    public Activation getActivation() {
      return activation;
    }
  }

  private static class Rule {
    public static final String FL_THEN = "then";
    public static final String FL_AND = "and";
    public static final String FL_OR = "or";
    private String text;

    public String getText() {
      return text;
    }
  }

  private interface Conjunction {}

  private interface Disjunction {}

  private interface Activation {}

  private class Accumulation {}
}
