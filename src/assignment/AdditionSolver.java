package assignment;

/**
 * Created by Magne on 10-Feb-15.
 */
public class AdditionSolver extends ArithmeticSolver {
  @Override
  protected double doOperation(Operator op, double a, double b) {
    return a+b;
  }

  @Override
  protected boolean canHandleOperator(Operator op) {
    return op == Operator.ADD;
  }
}
