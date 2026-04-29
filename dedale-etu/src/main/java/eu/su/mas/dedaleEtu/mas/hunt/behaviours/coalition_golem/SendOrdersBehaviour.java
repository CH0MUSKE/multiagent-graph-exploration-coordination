package eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem;

import eu.su.mas.dedaleEtu.mas.hunt.agents.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.hunt.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class SendOrdersBehaviour extends OneShotBehaviour {
    AbstractCommunicationAgent agent;
    public SendOrdersBehaviour(final AbstractCommunicationAgent myagent){
        super(myagent);
        this.agent = myagent;
    }

    @Override
    public void action(){
        String convId = this.agent.getCoalitionId();
        HashMap<String,String> plan = (HashMap<String, String>) this.getDataStore().get("ORDER_GOLEM");
        String curPos = this.agent.getCurrentPosition().getLocationId();
        Serializable[] content = new Serializable[2];
        content[0] = plan;
        content[1] = curPos;

        if (plan == null) {
            //System.err.println(this.myAgent.getLocalName() + " - Erreur : Aucun plan trouvé dans le DataStore ! + ( " + this.agent.getCoalitionId() +" )");
            return;
        }
        List<String> recipients = agent.getChildren();
        if (recipients.isEmpty()) {
            //System.out.println(this.myAgent.getLocalName() + " - Aucun enfant à qui envoyer l'ordre.");
            return;
        }

        ACLMessage msg = MessageFactory.buildMsg(
                this.agent.getAID(),
                recipients,
                "ORDER_GOLEM",
                content,
                convId
        );

        this.agent.sendMessage(msg);
    }
}
