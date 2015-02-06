package assignment;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Magne on 05-Feb-15.
 */
public class AdditionSolver extends Agent {
  private static final int delayPerProblemMs = 1000;

  private Queue<ArithmeticTask> problemQueue = new LinkedList<>();

  @Override
  protected void setup() {
    System.out.printf(
      "Hello! Addition-agent %s is ready.",
      getAID().getName()
    );
  }


  private class BidOnProblemsBehavior extends CyclicBehaviour {
    @Override
    public void action() {
      ACLMessage msg = myAgent.receive(
        MessageTemplate.MatchPerformative(ACLMessage.CFP)
      );
      ACLMessage reply = msg.createReply();
      if (msg != null) {
        ArithmeticTask task;
        task = ArithmeticTask.fromString(msg.getContent());

        if (task == null) {
          reply.setPerformative(ACLMessage.REFUSE);
          reply.setContent("malformed-task");
        } else if (task.op != ArithmeticTask.Operator.ADD) {
          reply.setPerformative(ACLMessage.REFUSE);
          reply.setContent("no-capability");
        } else {
          reply.setPerformative(ACLMessage.PROPOSE);
          reply.setContent(String.valueOf(
            (problemQueue.size() + 1) * delayPerProblemMs
          ));
        }

        myAgent.send(reply);
      } else {
        // Stop execution of this behavior until a new message is received
        block();
      }
    }
  }
}
