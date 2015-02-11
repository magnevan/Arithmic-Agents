package assignment;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Magne on 05-Feb-15.
 */
public abstract class ArithmeticSolver extends Agent {
  public static final int delayPerProblemMs = 1000;

  private Queue<ArithmeticTask> problemQueue = new LinkedList<>();
  private Map<Integer, AID> taskToSender = new HashMap<>();

  protected abstract double doOperation(Operator op, double a, double b);
  protected abstract boolean canHandleOperator(Operator op);
  protected abstract DFAgentDescription getAgentDescription();

  @Override
  protected void setup() {
    System.out.printf(
      "Hello! Agent %s is ready\n",
      getAID().getName()
    );
    registerWithYellowpages();
    addBehaviour(new BidOnProblemsBehavior());
    addBehaviour(new AcceptProblemsBehavior());
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

  private class SolveAndAnswerProblemsBehavior extends WakerBehaviour {
    public SolveAndAnswerProblemsBehavior(Agent a, long timeout) {
      super(a, timeout);
    }

    @Override
    public void onWake() {
      if (!problemQueue.isEmpty()) {
        ArithmeticTask task = problemQueue.poll();
        System.out.printf("Solving problem %s\n", task.readableDescription());

        task.answer = doOperation(task.op, task.a, task.b);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(taskToSender.remove(task.uniqueId));
        msg.setContent(task.toJson());
        myAgent.send(msg);

        if (!problemQueue.isEmpty()) {
          myAgent.addBehaviour(
            new SolveAndAnswerProblemsBehavior(myAgent, delayPerProblemMs)
          );
        }
      }
    }

  }

  private class AcceptProblemsBehavior extends CyclicBehaviour {
    @Override
    public void action() {
      ACLMessage msg = myAgent.receive(
        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
      );
      if (msg != null) {
        ACLMessage reply = msg.createReply();
        ArithmeticTask task;
        task = ArithmeticTask.fromJson(msg.getContent());

        if (task == null) {
          reply.setPerformative(ACLMessage.FAILURE);
          reply.setContent("malformed-task");
          myAgent.send(reply);
        } else if (!canHandleOperator(task.op)) {
          reply.setPerformative(ACLMessage.FAILURE);
          reply.setContent("no-capability");
          myAgent.send(reply);
        } else {
          if (problemQueue.isEmpty()) {
            addBehaviour(new SolveAndAnswerProblemsBehavior(
              myAgent, delayPerProblemMs
            ));
          }
          problemQueue.add(task);
          taskToSender.put(task.uniqueId, msg.getSender());
        }
      } else {
        // Stop execution of this behavior until a new message is received
        if (myAgent.getCurQueueSize() == 0) block();
      }
    }
  }

  private class BidOnProblemsBehavior extends CyclicBehaviour {
    @Override
    public void action() {
      ACLMessage msg = myAgent.receive(
        MessageTemplate.MatchPerformative(ACLMessage.CFP) // call for proposal
      );
      if (msg != null) {
        ACLMessage reply = msg.createReply();

        ArithmeticTask task;
        task = ArithmeticTask.fromJson(msg.getContent());

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
        if (myAgent.getCurQueueSize() == 0) block();
      }
    }
  }
}
