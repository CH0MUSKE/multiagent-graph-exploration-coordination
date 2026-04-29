package eu.su.mas.dedaleEtu.mas.hunt.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.hunt.agents.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem.participant.CoalitionParticipantGolemBehaviour;
import eu.su.mas.dedaleEtu.mas.hunt.knowledge.NodeInfo;
import eu.su.mas.dedaleEtu.mas.hunt.utils.MessageFactory;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import org.graphstream.graph.Node;

import java.util.List;
public class ListenerBehaviour extends CyclicBehaviour {
    private final List<String> protocols = List.of("PING", "PONG", "ACK", "SHARE-MAP", "FLOOD_GOLEM", "SMELL");
    MessageTemplate template;
    AbstractCommunicationAgent myagent;
    public ListenerBehaviour(AbstractCommunicationAgent myagent) {
        super(myagent);
        this.myagent = myagent;
        this.template = MessageFactory.buildMultiProtocolTemplate(ACLMessage.INFORM,protocols);

    }
    
    @Override
    public void action(){
        // On boucle tant qu'il y a des messages dans la file
        ACLMessage msgReceived;
        while ((msgReceived = this.myAgent.receive(this.template)) != null) {
            String protocol = msgReceived.getProtocol();
            switch (protocol){
                case "PING" -> pingHandler(msgReceived);
                case "PONG" -> pongHandler(msgReceived);
                case "ACK" -> ackHandler(msgReceived);
                case "SHARE-MAP" -> shareMapHandler(msgReceived);
                case "FLOOD_GOLEM" -> floodHandler(msgReceived);
                case "SMELL" -> smellHandler(msgReceived);
            }
        }
        // La boîte est vide, on peut laisser la main aux autres behaviours
        block();
    }
    
    private void pingHandler(ACLMessage msg){
        int smellRadius = 0;
        String sender = msg.getSender().getLocalName();
        List <String> monoList = List.of(sender);
        String conversation_id = msg.getConversationId();
        try {
            smellRadius = (int)msg.getContentObject();
            this.myagent.setGlobalEstimatedGolemSmell(smellRadius);
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        this.myagent.addBehaviour(new SendMsgBehaviour(this.myagent,conversation_id, "", "PONG",monoList));
    }
    
    private void pongHandler(ACLMessage msg){
        //System.out.println(this.myagent.getLocalName() +  ": PONG RECU ");
        String conv_id = msg.getConversationId();
        String sender_id = msg.getSender().getLocalName();
        Long currtime = System.currentTimeMillis();
        if(!this.myagent.checkValidId(conv_id)){
            //System.out.println(this.myagent.getLocalName() + ": convId invalide | " + conv_id);
            return;
        }
        if(this.myagent.hasAlreadySentMap(sender_id)){
            //System.out.println(this.myagent.getLocalName() + ": map déjà envoyé " + conv_id);
            return;
        }
        //System.out.println(this.myagent.getLocalName() + ": Calcul du subgraph...");
        Long lastConv = this.myagent.getLastConvRegistry(sender_id);
        SerializableSimpleGraph sg = this.myagent.getMyMap().createSubGraph(lastConv);
        this.myAgent.addBehaviour(new SendMsgBehaviour((AbstractDedaleAgent)myAgent, conv_id, sg, "SHARE-MAP", List.of(sender_id)));
        this.myagent.markMapAsSent(sender_id, currtime);
        //System.out.println(this.myAgent.getLocalName() + " -> Envoi du delta à " + sender_id);
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
            //System.out.println("Succès : Échange avec " + sender_id + " validé.");
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
            sgreceived = (SerializableSimpleGraph<String, NodeInfo>)msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        if (sgreceived != null) {
            this.myagent.getMyMap().mergeMap( this.myagent.getLocalName(),sgreceived);
            //System.out.println("merged !");
            this.myagent.addBehaviour(new SendMsgBehaviour(this.myagent, conv_id, "", "ACK",List.of(sender_id)));
        } else {
            //System.out.println("Erreur : Graphe reçu vide ou corrompu.");
        }
        
    }
    
    private boolean shouldDelegate(String agentID) {
        if (agentID.equals(this.myagent.getAID().getLocalName())) return false;
        if (this.myagent.getLeaderId() == null || this.myagent.getLeaderId().isEmpty()) return true;
        return agentID.compareTo(this.myagent.getLeaderId()) <= 0;
    }
    
    private void floodHandler(ACLMessage msg) {
        if(this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.BLOCKING)) return;
        String conversation_id = msg.getConversationId();
        String[] content = null;
        try {
            content = (String[]) msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        if (content == null) {
            //System.out.println("Erreur : message flood corrompu");
            return;
        }
        String leader_id = content[0];
        String golem_pos = content[1];
        long timeout = Long.parseLong(content[2]);
        int depth = Integer.parseInt(content[3]);
        String senderId = msg.getSender().getLocalName();
        
        Node golem_node = this.myagent.getMyMap().getG().getNode(golem_pos);
        
        if(golem_node == null ||(boolean) golem_node.getAttribute("blocked")) return;

        boolean transitionAuthorized = false;

        switch (this.myagent.getStatus()) {
            case HUNTING -> {
                if (shouldDelegate(leader_id) && !this.myagent.getCoalitionId().equals(conversation_id)) {
                    transitionAuthorized = true;
                }
            }
            // Si on explore ou qu'on sent le golem, on accepte d'entrer en coalition
            default -> transitionAuthorized = true;
        }
        //System.out.println("Je suis: " + this.myagent.getLocalName() + " et mon leader est: " + leader_id);


        if (transitionAuthorized) {
            // On nettoie TOUJOURS avant de commencer
            this.myagent.stopCurrentCoalition();

            // On lance la nouvelle
            this.myagent.startCoalition(
                    new CoalitionParticipantGolemBehaviour(myagent, timeout, this.myagent.agentList, depth, leader_id),
                    AbstractCommunicationAgent.Status.HUNTING,
                    leader_id,
                    golem_pos,
                    conversation_id,
                    senderId
            );

        }
    }
    
    private void smellHandler(ACLMessage msg){
        if(this.myagent.getStatus() == AbstractCommunicationAgent.Status.EXPLORING){
            List<String> smellNodes = null;
            try{
                smellNodes = (List<String>)msg.getContentObject();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            if(smellNodes != null){
                this.myagent.setStatus(AbstractCommunicationAgent.Status.SMELLING);
                this.myagent.setOrderTarget(smellNodes.get((int) (Math.random() * smellNodes.size())));
                System.out.println(this.myagent.getLocalName() +  " -> SMELL_GOLEM reçu, je me dirige vers: " + this.myagent.getOrderTarget());
            }
        }
    }
    
    
}
