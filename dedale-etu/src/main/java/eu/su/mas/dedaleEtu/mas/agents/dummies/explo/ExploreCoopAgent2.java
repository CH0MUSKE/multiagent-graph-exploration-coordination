package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ICommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreCoopBehaviour2;
import eu.su.mas.dedaleEtu.mas.behaviours.ListenerBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PingBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;

import java.util.*;

public class ExploreCoopAgent2 extends AbstractDedaleAgent implements ICommunicationAgent {
    private static final long serialVersionUID = -7969469610241668130L;
    // Dans MyAgent.java
    private HashMap<String, Long> sentMapsRegistry = new HashMap<>();
    private HashMap<String, Long> lastConvRegistry = new HashMap<>();
    private HashMap<String, Long> activeConversations = new HashMap<>();
    private static final long CONV_TIMEOUT = 5000;
    private MapRepresentation myMap;
    
    protected void setup(){

        super.setup();

        final Object[] args = getArguments();
        List<String> list_agentNames = new ArrayList<>();

        if (args != null && args.length > 0) {
            EntityCharacteristics ec = (EntityCharacteristics) args[0];

            List<String> userParams = ec.getUserParameters();

            if (userParams != null && !userParams.isEmpty()) {
                list_agentNames.addAll(userParams);
                System.out.println(this.getLocalName() + " contactera : " + list_agentNames);
            }
        }


        List<Behaviour> lb=new ArrayList<Behaviour>();

        /************************************************
         *
         * ADD the behaviours of the Dummy Moving Agent
         *
         ************************************************/
        lb.add(new ExploreCoopBehaviour2(this, list_agentNames));
        lb.add(new ListenerBehaviour(this));




        /***
         * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
         */


        addBehaviour(new StartMyBehaviours(this,lb));

        System.out.println("the  agent "+this.getLocalName()+ " is started");

    }

    public void markMapAsSent(String user_id, Long currTime) {
        this.sentMapsRegistry.put(user_id, currTime);
    }

    public boolean hasAlreadySentMap(String agentId) {
        if (!this.sentMapsRegistry.containsKey(agentId)) {
            return false; 
        }

        long lastSentTime = this.sentMapsRegistry.get(agentId);
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastSentTime) > CONV_TIMEOUT) {
            this.sentMapsRegistry.remove(agentId); 
            return false;
        }

        return true; 
    }
    public Long getSentMapDate(String sender_id){
        return this.sentMapsRegistry.get(sender_id);
    }
    
    public void deleteEntrySentMapRegistry(String sender_id){
        this.sentMapsRegistry.remove(sender_id);
    }
    
    public void clearMapRegistry(){
        this.sentMapsRegistry.clear();
    }
    public void addConvId(String id){
        activeConversations.put(id,System.currentTimeMillis());
    }
    
    public boolean checkValidId(String id){
        if (id == null || !activeConversations.containsKey(id)) {
            return false;
        }

        long startTime = activeConversations.get(id);
        if ((System.currentTimeMillis() - startTime) > CONV_TIMEOUT) {
            activeConversations.remove(id);
            return false;
        }
        return true;
    }
    
    public MapRepresentation getMyMap(){
        return myMap;
    }
    
    
    /**
     * This method is automatically called after doDelete()
     */
    protected void takeDown(){
        super.takeDown();
    }

    protected void beforeMove(){
        super.beforeMove();
        //System.out.println("I migrate");
    }

    protected void afterMove(){
        super.afterMove();
        //System.out.println("I migrated");
    }

    public Long getLastConvRegistry(String id) {
        return lastConvRegistry.get(id);
    }

    public void setLastConvRegistry(String id, Long last_update) {
        this.lastConvRegistry.put(id, last_update);
    }
    
    public void setMap(MapRepresentation map){
        this.myMap = map;
    }
    
    
}
