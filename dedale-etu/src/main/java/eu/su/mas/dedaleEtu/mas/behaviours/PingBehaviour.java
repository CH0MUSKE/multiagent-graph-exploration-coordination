package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent2;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (myPosition != null && myPosition.getLocationId() != null && !myPosition.getLocationId().isEmpty()) {
            ACLMessage msg = MessageFactory.buildMsg(this.myAgent.getAID(), agentNames, "PING", myPosition.getLocationId(), conversation_id);
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
        }
    }
}
