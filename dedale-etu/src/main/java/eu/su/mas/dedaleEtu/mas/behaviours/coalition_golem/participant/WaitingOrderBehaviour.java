package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.participant;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingOrderBehaviour extends SimpleBehaviour {
    AbstractCommunicationAgent agent;
    private final long timeout = 7000;
    private final long startTime;
    private boolean finished;
    
    public WaitingOrderBehaviour(final AbstractCommunicationAgent agent){
        this.startTime = System.currentTimeMillis();
        this.agent = agent;
    }
    @Override
    public void action() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= timeout) {
            this.finished = true;
            return;
        }
        MessageTemplate template = MessageFactory.buildMultiProtocolTemplate(ACLMessage.INFORM, List.of("ORDER_GOLEM"));
        ACLMessage msg = this.myAgent.receive(template);
        while (msg != null) {
            if (this.agent.checkValidCoalition(msg.getConversationId())) {
                try {
                    @SuppressWarnings("unchecked")
                    Serializable[] content = (Serializable[]) msg.getContentObject();
                    HashMap<String, String> plan = (HashMap<String, String>) content[0];
                    String order = plan.get(this.agent.getAID().getLocalName());
                    // Si l'agent n'a pas été pris en compte dans le plan, il suit son parent
                    if (order == null) {
                        order = this.agent.getGolemPos();
                    }
                    this.agent.setOrderTarget(order);
                    this.finished = true;
                    this.getDataStore().put("ORDER_GOLEM", plan);
                    System.out.println("plan reçu, je me dirige vers: " + order);
                    return;

                } catch (UnreadableException e) {
                    System.err.println("Erreur de désérialisation : " + e.getMessage());
                }
            }
            else{
                System.out.println("Plan reçu mais convId invalide");
            }
            msg = this.myAgent.receive(template);
        }
        long remaining = timeout - (System.currentTimeMillis() - startTime);
        if (remaining > 0) {
            block(remaining);
        } else {
            this.finished = true;
        }
    }
    
    @Override
    public boolean done(){
        return finished;
    }
}
