package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ICommunicationAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent2;
import eu.su.mas.dedaleEtu.mas.knowledge.NodeInfo;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public class ListenerBehaviour extends CyclicBehaviour {
    private final List<String> protocols = List.of("PING", "PONG", "ACK", "SHARE-MAP");
    MessageTemplate template;
    ICommunicationAgent myagent;
    public ListenerBehaviour(ICommunicationAgent myagent) {
        super((AbstractDedaleAgent)myagent);
        this.myagent = myagent;
        this.template = MessageFactory.buildMultiProtocolTemplate(ACLMessage.INFORM,protocols);

    }
    
    @Override
    public void action(){
        ACLMessage msgReceived=this.myAgent.receive(this.template);
        if(msgReceived != null){
            String protocol = msgReceived.getProtocol();
            switch (protocol){
                case "PING" -> pingHandler(msgReceived);
                case "PONG" -> pongHandler(msgReceived);
                case "ACK" -> ackHandler(msgReceived);
                case "SHARE-MAP" -> shareMapHandler(msgReceived);
            }
        }
        else{
            block();
        }

    }
    
    // On envoie la position juste pour envoyer quelque chose mais pour l'instant ça sert à rien
    private void pingHandler(ACLMessage msg){
        String pos = null;
        String sender = msg.getSender().getLocalName();
        List <String> monoList = List.of(sender);
        String conversation_id = msg.getConversationId();
        try {
            pos = (String)msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        if(pos != null){
            Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            if (myPosition != null && myPosition.getLocationId() != null && !myPosition.getLocationId().isEmpty()) {
                ACLMessage mess = MessageFactory.buildMsg(this.myAgent.getAID(), monoList, "PONG", myPosition.getLocationId(), conversation_id);
                ((AbstractDedaleAgent) this.myAgent).sendMessage(mess);
            }        
        }
    }
    
    private void pongHandler(ACLMessage msg){
        String conv_id = msg.getConversationId();
        String sender_id = msg.getSender().getLocalName();
        Long currtime = System.currentTimeMillis();
        if(!this.myagent.checkValidId(conv_id) || this.myagent.hasAlreadySentMap(sender_id)){
            return;
        }
        Long lastConv = this.myagent.getLastConvRegistry(sender_id);
        SerializableSimpleGraph sg = this.myagent.getMyMap().createSubGraph(lastConv);
        this.myAgent.addBehaviour(new SendMsgBehaviour((AbstractDedaleAgent)myAgent, conv_id, sg, "SHARE-MAP", List.of(sender_id)));
        this.myagent.markMapAsSent(sender_id, currtime);
        System.out.println(this.myAgent.getLocalName() + " -> Envoi du delta à " + sender_id);
    }
    
    private void ackHandler(ACLMessage msg){
        String conv_id = msg.getConversationId();
        String sender_id = msg.getSender().getLocalName();
        Long sentMapDate = this.myagent.getSentMapDate(sender_id);
        
        if(!this.myagent.checkValidId(conv_id)){
            return;
        }
        if (sentMapDate != null) {
            this.myagent.setLastConvRegistry(sender_id, sentMapDate);
            System.out.println("Succès : Échange avec " + sender_id + " validé.");
        } else {
            // Si c'est null, c'est soit un doublon, soit un message orphelin
            System.out.println("ACK ignoré (déjà traité ou expiré).");
        }
    }
    
    private void shareMapHandler(ACLMessage msg){
        String conv_id = msg.getConversationId();
        String sender_id = msg.getSender().getLocalName();

        SerializableSimpleGraph<String, NodeInfo> sgreceived=null;
        try {
            System.out.println("aza");
            sgreceived = (SerializableSimpleGraph<String, NodeInfo>)msg.getContentObject();
        } catch (UnreadableException e) {
            System.out.println("pppo");
            e.printStackTrace();
        }
        if (sgreceived != null) {
            this.myagent.getMyMap().mergeMap(sgreceived);
            System.out.println("merged !");

            ACLMessage mess = MessageFactory.buildMsg(this.myAgent.getAID(), List.of(sender_id), "ACK","", conv_id);
            ((AbstractDedaleAgent) this.myAgent).sendMessage(mess);
        } else {
            System.out.println("Erreur : Graphe reçu vide ou corrompu.");
        }
        
    }
    
    
}
