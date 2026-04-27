package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.participant;

import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.apache.commons.math3.analysis.function.Abs;

import java.util.List;

public class SendEchoBehaviour extends OneShotBehaviour {
    AbstractCommunicationAgent agent;
    public SendEchoBehaviour(final AbstractCommunicationAgent agent){
        super(agent);
        this.agent = agent;
    }
    @Override
    public void action(){
        this.agent.getCoalition().put(this.agent.getAID().getLocalName(), this.agent.getCurrentPosition().getLocationId());
        ACLMessage msg = MessageFactory.buildMsg(this.agent.getAID(), List.of(this.agent.getParent()), "ECHO_GOLEM", this.agent.getCoalition(), this.agent.getCoalitionId());
        this.agent.sendMessage(msg);

    }
}
