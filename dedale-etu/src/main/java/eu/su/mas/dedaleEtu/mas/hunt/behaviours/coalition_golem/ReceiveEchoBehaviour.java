package eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem;

import eu.su.mas.dedaleEtu.mas.hunt.agents.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.hunt.utils.MessageFactory;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiveEchoBehaviour extends SimpleBehaviour {
    String conversation_id;
    AbstractCommunicationAgent agent;
    private final long timeout;
    private final long startTime;

    private boolean finished;
    public ReceiveEchoBehaviour(AbstractCommunicationAgent myagent, long timeout){
        super(myagent);
        this.agent = myagent; 
        this.startTime = System.currentTimeMillis();
        this.timeout = timeout;
    }
    
    @Override
    public void action(){
        this.conversation_id = this.agent.getCoalitionId();
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= timeout) {
            this.finished = true;
            System.out.println("timeout reached");
            return;
        }
        MessageTemplate template = MessageFactory.buildMultiProtocolTemplate(ACLMessage.INFORM, List.of("ECHO_GOLEM"));
        ACLMessage msg = this.myAgent.receive(template);
        while (msg != null) {
            if (this.agent.checkValidCoalition(msg.getConversationId())) {
                try {
                    @SuppressWarnings("unchecked")
                    HashMap<String, String> branchMembers = (HashMap<String, String>) msg.getContentObject();

                    if (branchMembers != null) {
                        for (Map.Entry<String, String> entry : branchMembers.entrySet()) {
                            String name = entry.getKey();
                            String pos = entry.getValue();
                            this.agent.addAgentToCoalition(name, pos);
                        }
                    }
                } catch (UnreadableException e) {
                    System.err.println("Erreur de désérialisation : " + e.getMessage());
                }
                this.agent.getChildren().add(msg.getSender().getLocalName());
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
    public boolean done() {
        return finished;
    }

    @Override
    public int onEnd() {
        if (this.agent.getCoalitionSize() > 0) {
            System.out.println("Succès ! (Sous) coalition formée avec " + this.agent.getCoalitionSize() + " membres. ( " + this.agent.getCoalitionId() + " )");
            return 1; 
        } else {
            System.out.println("Échec... personne n'a répondu. Tentative de re-ping.");
            return 0; 
        }
    }
}
