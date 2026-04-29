package eu.su.mas.dedaleEtu.mas.hunt.behaviours;

import eu.su.mas.dedaleEtu.mas.hunt.agents.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.hunt.utils.MessageFactory;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.List;

public class PingBehaviour extends OneShotBehaviour {
    private static final long serialVersionUID = 9088209402507795289L;
    List<String> agentNames;
    private AbstractCommunicationAgent myagent;

    public PingBehaviour(final AbstractCommunicationAgent myagent, List<String> agentNames) {
        super(myagent);
        this.agentNames = agentNames;
        this.myagent = myagent;
    }

    @Override
    public void action() {
        String conversation_id = this.myAgent.getLocalName() + " " + System.currentTimeMillis();
        this.myagent.addConvId(conversation_id);
        ACLMessage msg = MessageFactory.buildMsg(this.myAgent.getAID(), agentNames, "PING", this.myagent.getGlobalEstimatedGolemSmell(), conversation_id);
        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
    }
}
