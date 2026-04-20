package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.apache.commons.math3.analysis.function.Abs;
import weka.knowledgeflow.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class SendOrdersBehaviour extends OneShotBehaviour {
    AbstractCommunicationAgent agent;
    String convId;
    public SendOrdersBehaviour(final AbstractCommunicationAgent myagent, String convId){
        super(myagent);
        this.agent = myagent;
        this.convId = convId;
    }

    @Override
    public void action(){
        HashMap<String,String> plan = (HashMap<String, String>) this.getDataStore().get("PLAN_GOLEM");
        String curPos = this.agent.getCurrentPosition().getLocationId();
        Serializable[] content = new Serializable[2];
        content[0] = plan;
        content[1] = curPos;

        if (plan == null) {
            System.err.println(this.myAgent.getLocalName() + " - Erreur : Aucun plan trouvé dans le DataStore !");
            return;
        }

        List<String> recipients = agent.getChildren();
        if (recipients.isEmpty()) {
            System.out.println(this.myAgent.getLocalName() + " - Aucun enfant à qui envoyer l'ordre.");
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
