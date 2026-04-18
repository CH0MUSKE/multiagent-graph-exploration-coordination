package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import java.util.HashMap;
import java.util.List;

public interface ICommunicationAgent {
    void markMapAsSent(String user_id, Long currTime);

    boolean hasAlreadySentMap(String user_id);

    Long getSentMapDate(String sender_id);

    void deleteEntrySentMapRegistry(String sender_id);

    void clearMapRegistry();

    void addConvId(String id);

    boolean checkValidId(String id);
    MapRepresentation getMyMap();
    Long getLastConvRegistry(String id);
    void setLastConvRegistry(String id, Long last_update);
    void setMap(MapRepresentation map);

    String getCoalitionPos(String id);
    void addAgentToCoalition(String id, String pos);
    HashMap<String, String> getCoalition();
    
    String getCoalitionId();
    void setCoalitionId(String coalitionId);
    boolean checkValidCoalition(String coalitionId);
    int getCoalitionSize();
    String getGolemPos();
    void setGolemPos(String pos);
    
    List<String> getChildren();
    

}