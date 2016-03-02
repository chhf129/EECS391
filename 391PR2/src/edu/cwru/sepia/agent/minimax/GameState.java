package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
	
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
	List<GameUnit>footmen=new LinkedList<GameUnit>();//contain the footmen units
	List<GameUnit>archers=new LinkedList<GameUnit>();//contain the footmen units
	static int archerAttackRange,footmenAttackRange;
	static int xExtent,yExtent;//size of the map
	List<ResourceView> obstacles=new LinkedList<ResourceView>();
	boolean isFootmenTurn=false;
	State.StateView stateView; //for testing purpose
    public GameState(State.StateView state) {
    	stateView=state;
    	for (int i=0; i<state.getUnits(0).size();i++){//player's units are footmen
    		footmen.add(new GameUnit(state.getUnits(0).get(i)));
    	}
    	for (int i=0; i<state.getUnits(1).size();i++){//enemy units are archers
    		archers.add(new GameUnit(state.getUnits(1).get(i)));
    	}
    	xExtent=state.getXExtent();
    	yExtent=state.getYExtent();
    	obstacles.addAll(state.getAllResourceNodes());
    	footmenAttackRange=state.getUnits(0).get(0).getTemplateView().getRange();
    	archerAttackRange=state.getUnits(1).get(0).getTemplateView().getRange();
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
    	if (archers.isEmpty()){
    		return Double.POSITIVE_INFINITY;
    	}
    	if (footmen.isEmpty()){
    		return Double.NEGATIVE_INFINITY;
    	}
    	double total = 0;
    	for (GameUnit f: footmen){
    		total += distanceUtility(f, archers);
    		total += footmanHPUtility(f);
    	}
    	for (GameUnit a: archers){
    		total += archerHPUtility(a);
    	}
        return total;
    }
    //range 0-1 based on the inverse of the distance between footman and the closest archer
    public double distanceUtility(GameUnit footman, List<GameUnit> archers){
    	double dist = Double.POSITIVE_INFINITY;
		//find the distance to the closest archer
		for (GameUnit a: archers){
			int xDif = Math.abs(footman.getXPosition()-a.getXPosition());
			int yDif = Math.abs(footman.getYPosition()-a.getYPosition());
			dist = Math.min(dist, Math.sqrt(xDif*xDif + yDif*yDif));//pythagorean theorem
		}
		return Math.pow(dist, -1);
    }
    //range 0-1 based on the fraction of HP remaining for the footman
    public double footmanHPUtility(GameUnit footman){
    	return (double)footman.hp/footman.maxHP;
    }
    //range -1-0 based in the fraction of hp remaining for the archer
    public double archerHPUtility(GameUnit archer){
    	return (double)-archer.hp/archer.maxHP;
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.	
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
    	List<Action> unitOneAction,unitTwoAction=new LinkedList<Action>();
    	List<GameStateChild> gameStateChildren=new LinkedList<GameStateChild>();;
    	//collect all possible action
    	if (isFootmenTurn){
    		for (GameUnit f: footmen){
    			getAllAction(f);
    		}
    	}
    	
        return gameStateChildren;
    }
    
    
    /*
     * helper method for getChildren(),get all possible moves for one unit
     */
    private List<Action> getAllAction(GameUnit unit){
    	List<Action> action =new LinkedList<Action>();
 
    	Direction[] possibleDirections={Direction.EAST,Direction.SOUTH,Direction.NORTH,Direction.WEST};
		for (Direction direction: possibleDirections){
			if (isValidDirection(unit,direction)){
				action.add(Action.createPrimitiveMove(unit.getId(), direction));
			}}
		return null;
    }
    
    /*
     * helper method for getChildren(), check if unit can move into certain direction
     */
    private boolean isValidDirection(GameUnit unit,Direction direction) {
    	int targetX=+direction.xComponent();
    	int targetY=+direction.yComponent();
    	//check boundary
    	if (targetX<0 || targetY<0 || targetX>xExtent || targetY>yExtent){
    		return false;
    	}
    	//if meet obstacles
    	for (ResourceView r:obstacles){
    		if (r.getXPosition()==targetX && r.getYPosition()==targetY){
    			return false;
    		}
    	}
    	//if meet other people
    	return true;
    }
}
