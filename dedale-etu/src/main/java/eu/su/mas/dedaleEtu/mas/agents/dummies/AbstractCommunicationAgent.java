package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import net.sourceforge.plantuml.salt.element.ListWidth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractCommunicationAgent extends AbstractDedaleAgent implements ICommunicationAgent {
    protected HashMap<String, Long> sentMapsRegistry = new HashMap<>();
    //Pour l'échange de map seulement. TODO: CHANGER LE NOM
    protected HashMap<String, Long> lastConvRegistry = new HashMap<>();
    //Pour l'échange de map seulement. TODO: CHANGER LE NOM
    protected HashMap<String, Long> activeConversations = new HashMap<>();
    protected HashMap<String, String> coalition = new HashMap<>();
    protected String coalitionId;
    //Pour l'échange de map seulement. TODO: CHANGER LE NOM
    protected static final long CONV_TIMEOUT = 5000;
    protected MapRepresentation myMap;
    protected String golemPos;
    protected List<String> children = new ArrayList<>();

    @Override
    public void markMapAsSent(String user_id, Long currTime) {
        this.sentMapsRegistry.put(user_id, currTime);
    }

    @Override
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
    @Override
    public Long getSentMapDate(String sender_id){
        return this.sentMapsRegistry.get(sender_id);
    }
    @Override
    public void deleteEntrySentMapRegistry(String sender_id){
        this.sentMapsRegistry.remove(sender_id);
    }

    @Override
    public void clearMapRegistry(){
        this.sentMapsRegistry.clear();
    }
    @Override
    public void addConvId(String id){
        activeConversations.put(id,System.currentTimeMillis());
    }
    @Override
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
    @Override
    public boolean checkValidCoalition(String coalitionId){
        return this.coalitionId.equals(coalitionId);
    }

    @Override
    public MapRepresentation getMyMap(){
        return myMap;
    }

    @Override
    public Long getLastConvRegistry(String id) {
        return lastConvRegistry.get(id);
    }
    @Override
    public void setLastConvRegistry(String id, Long last_update) {
        this.lastConvRegistry.put(id, last_update);
    }

    @Override
    public void setMap(MapRepresentation map){
        this.myMap = map;
    }

    @Override
    public String getCoalitionPos(String id) {
        return this.coalition.get(id);
    }
    
    @Override
    public void addAgentToCoalition(String id, String pos) {
        this.coalition.put(id, pos);
    }
    @Override
    public String getCoalitionId() {
        return coalitionId;
    }
    @Override
    public void setCoalitionId(String coalitionId) {
        this.coalitionId = coalitionId;
    }
    
    @Override
    public int getCoalitionSize(){
        return coalition.size();
    }
    
    @Override
    public String getGolemPos(){
        return this.golemPos;
    }
    @Override
    public void setGolemPos(String golemPos){
        this.golemPos = golemPos;
    }
    @Override
    public HashMap<String,String> getCoalition(){
        return this.coalition;
    }

    @Override
    public List<String> getChildren() {
        return this.children;
    }
}
