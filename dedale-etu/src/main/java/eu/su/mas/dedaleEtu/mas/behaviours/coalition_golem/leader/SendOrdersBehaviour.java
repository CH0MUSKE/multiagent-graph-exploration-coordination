package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.apache.commons.math3.analysis.function.Abs;
import weka.knowledgeflow.Data;

import java.io.Serializable;
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
        Serializable plan = (Serializable) this.getDataStore().get("PLAN_GOLEM");

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
                "GOLEM_ORDER",
                plan,
                convId
        );

        this.agent.sendMessage(msg);
    }
}
