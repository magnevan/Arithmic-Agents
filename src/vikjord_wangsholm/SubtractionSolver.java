package vikjord_wangsholm;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Created by Magne on 11-Feb-15.
 */
public class SubtractionSolver extends ArithmeticSolver {
  @Override
  protected double doOperation(vikjord_wangsholm.Operator op, double a, double b) {
    return a-b;
  }

  @Override
  protected boolean canHandleOperator(vikjord_wangsholm.Operator op) {
    return op == vikjord_wangsholm.Operator.SUBTRACT;
  }

  @Override
  protected DFAgentDescription getAgentDescription() {
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());

    ServiceDescription sd = new ServiceDescription();
    sd.setName("JADE-subtraction-service");
    sd.setType(vikjord_wangsholm.Operator.SUBTRACT.getText());

    dfd.addServices(sd);
    return dfd;
  }
}
