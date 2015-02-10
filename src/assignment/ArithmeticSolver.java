package assignment;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jdk.nashorn.internal.runtime.JSONFunctions;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Magne on 05-Feb-15.
 */
public abstract class ArithmeticSolver extends Agent {
  private static final int delayPerProblemMs = 1000;

  private Queue<ArithmeticTask> problemQueue = new LinkedList<>();

  protected abstract double doOperation(Operator op, double a, double b);
  protected abstract boolean canHandleOperator(Operator op);
  protected abstract DFAgentDescription getAgentDescription();

  @Override
  protected void setup() {
    System.out.printf(
      "Hello! Addition-agent %s is ready.",
      getAID().getName()
    );
    registerWithYellowpages();
  }

  private void registerWithYellowpages() {
    DFAgentDescription dfd = getAgentDescription();
    try {
      DFService.register(this, dfd);
    }
    catch (FIPAException fe) {
      fe.printStackTrace();
    }
  }

  private class AcceptProblemsBehavior extends CyclicBehaviour {
    @Override
    public void action() {
      ACLMessage msg = myAgent.receive(
        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
      );
      ACLMessage reply = msg.createReply();
      if (msg != null) {
        ArithmeticTask task;
        task = ArithmeticTask.fromString(msg.getContent());

        if (task == null) {
          reply.setPerformative(ACLMessage.FAILURE);
          reply.setContent("malformed-task");
          myAgent.send(reply);
        } else if (!canHandleOperator(task.op)) {
          reply.setPerformative(ACLMessage.FAILURE);
          reply.setContent("no-capability");
          myAgent.send(reply);
        } else {
          problemQueue.add(task); // TODO need to add the asker
        }
      } else {
        // Stop execution of this behavior until a new message is received
        block();
      }
    }
  }

  private class BidOnProblemsBehavior extends CyclicBehaviour {
    @Override
    public void action() {
      ACLMessage msg = myAgent.receive(
        MessageTemplate.MatchPerformative(ACLMessage.CFP) // call for proposal
      );
      ACLMessage reply = msg.createReply();
      if (msg != null) {
        ArithmeticTask task;
        task = ArithmeticTask.fromString(msg.getContent());

        if (task == null) {
          reply.setPerformative(ACLMessage.REFUSE);
          reply.setContent("malformed-task");
        } else if (task.op != Operator.ADD) {
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
