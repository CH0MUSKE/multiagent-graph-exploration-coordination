package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

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

}