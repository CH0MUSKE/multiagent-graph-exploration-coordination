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

public class ControlledAgent2 extends AbstractCommunicationAgent{
    
    private static final long serialVersionUID = 728177281660199801L;
    private HashMap<String, Long> sentMapsRegistry = new HashMap<>();
    private HashMap<String, Long> lastConvRegistry = new HashMap<>();
    private HashMap<String, Long> activeConversations = new HashMap<>();
    private static final long CONV_TIMEOUT = 5000;
    private MapRepresentation myMap;
    private HashMap<String, String> coalition;
    private String coalitionId;


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
    
}
