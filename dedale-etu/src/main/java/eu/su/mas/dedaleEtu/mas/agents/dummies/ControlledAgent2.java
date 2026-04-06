package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ListenerBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;

public class ControlledAgent2 extends AbstractDedaleAgent implements ICommunicationAgent{



    private static final long serialVersionUID = 728177281660199801L;
    private HashMap<String, Long> sentMapsRegistry = new HashMap<>();
    private HashMap<String, Long> lastConvRegistry = new HashMap<>();
    private HashMap<String, Long> activeConversations = new HashMap<>();
    private static final long CONV_TIMEOUT = 5000;
    private MapRepresentation myMap;


    protected void setup(){
        super.setup();
        //get the parameters given into the object[]
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
        lb.add(new ListenerBehaviour(this));
        //lb.add(new GsControlBehaviour(this,300));

        addBehaviour(new StartMyBehaviours(this,lb));

    }


    /**
     * This method is automatically called after doDelete()
     */
    protected void takeDown(){
        super.takeDown();
    }

    /**
     * This method is automatically called before migration. 
     * You can add here all the saving you need
     */
    protected void beforeMove(){
        super.beforeMove();
    }

    /**
     * This method is automatically called after migration to reload. 
     * You can add here all the info regarding the state you want your agent to restart from 
     *
     */
    protected void afterMove(){
        if(myMap == null){
            this.myMap =  new MapRepresentation(this.getLocalName());
        }
        super.afterMove();
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
