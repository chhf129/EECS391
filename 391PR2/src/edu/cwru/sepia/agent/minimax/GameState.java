package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.minimax.AstarAgent.MapLocation;
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
	boolean isFootmenTurn=true;
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
    public GameState(GameState gameState){
    	footmen=new LinkedList<GameUnit>();
    	for (int i=0;i<gameState.footmen.size(); i++){
    		footmen.add(new GameUnit(gameState.footmen.get(i)));
    	}
    	archers=new LinkedList<GameUnit>();
    	for (int i=0;i<gameState.archers.size(); i++){
    		archers.add(new GameUnit(gameState.archers.get(i)));
    	}
    	obstacles=new LinkedList<ResourceView>(gameState.obstacles);
    	isFootmenTurn=gameState.isFootmenTurn;
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
    		//total += distanceUtility(f, archers);
    		total += 5*aStarUtility(f, archers);
    		//total += footmanHPUtility(f);
    	}
    	for (GameUnit a: archers){
    		total += 100*archerHPUtility(a);
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
    public double aStarUtility(GameUnit footman, List<GameUnit> archers){
    	AstarAgent aStar = new AstarAgent(0);
    	AstarAgent.MapLocation f = aStar.new MapLocation(footman.getXPosition(), footman.getYPosition(), 0);
    	Set<AstarAgent.MapLocation> obstructions = new HashSet<AstarAgent.MapLocation>();
    	for (ResourceView r: obstacles){
    		AstarAgent.MapLocation m = aStar.new MapLocation(r.getXPosition(), r.getYPosition(), 0);
    		obstructions.add(m);
    	}
    	double util = Double.POSITIVE_INFINITY;
    	for (GameUnit a: archers){
    		AstarAgent.MapLocation ar = aStar.new MapLocation(a.getXPosition(), a.getYPosition(), 0);
    		util = Math.min(util, aStar.AstarSearch(f, ar, xExtent, yExtent, f, obstructions).size());
    	}
    	System.out.println("footman " + footman.id + " path = " + util);
    	return Math.pow(util, -1);
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
    	List<GameStateChild> gameStateChildren=new LinkedList<GameStateChild>();
    	Map<Integer,Action> actionMap;
    	//collect all possible action
    	if (isFootmenTurn){
    		unitOneAction=getAllAction(footmen.get(0));
    		if (footmen.size()==2){
    			unitTwoAction=getAllAction(footmen.get(1));
    		}
    	}
    	else {
    		unitOneAction=getAllAction(archers.get(0));
    		if (archers.size()==2){
    			unitTwoAction=getAllAction(archers.get(1));
    		}
    	}
    	//generate child state by combine the possible actions from two units
    	if (!unitTwoAction.isEmpty()){
    		for (Action actionOne : unitOneAction) {
    			for (Action actionTwo : unitTwoAction) {
    				actionMap=new HashMap<Integer,Action>();
					actionMap.put(0, actionOne);  //0 and 1 are footmen's id, because archers' action are never executed by speia. So put 0 and 1 as unit id for archers' turn should be fine.
					actionMap.put(1, actionTwo);
					//check if two units moving to same location
			    	if (actionOne.getType()==ActionType.PRIMITIVEMOVE && actionTwo.getType()==ActionType.PRIMITIVEMOVE){
			    		if(moveToSameLocation(actionOne,actionTwo)){
			    			continue;
			    		}
			    	}
	    			GameState newState=new GameState(this);
	    			newState.simulateAction(actionMap);
	    			gameStateChildren.add(new GameStateChild(new HashMap<Integer,Action>(actionMap),newState));
				}
			}
    	}
    	//only one unit left
    	else{
    		for (Action actionOne : unitOneAction){
				actionMap=new HashMap<Integer,Action>();
				actionMap.put(footmen.get(0).getId(), actionOne);
					GameState newState=new GameState(this);
					newState.simulateAction(actionMap);
					gameStateChildren.add(new GameStateChild(new HashMap<Integer,Action>(actionMap), new GameState(newState)));
				}
    	}
        return gameStateChildren;
    }
    
    
    /**
     * helper method for getChildren(),get all possible moves for one unit
     * @param unit current iterated GameUnit
     * @return list of all possible actions
     */
    private List<Action> getAllAction(GameUnit unit){
    	List<Action> actionList =new LinkedList<Action>();
    	Direction[] possibleDirections={Direction.EAST,Direction.SOUTH,Direction.NORTH,Direction.WEST};
    	outerloop:
        //iterate all four directions
    	for (Direction direction: possibleDirections){
	    	int targetX=unit.getXPosition()+direction.xComponent();
	    	int targetY=unit.getYPosition()+direction.yComponent();
	    	//check boundary
	    	if (targetX<0 || targetY<0 || targetX>xExtent || targetY>yExtent){
	    		continue outerloop;
	    	}
	    	//if meet obstacles
	    	for (ResourceView r:obstacles){
	    		if (r.getXPosition()==targetX && r.getYPosition()==targetY){
	    			continue outerloop;
	    		}
	    	}
	    	//if meet other people
	    	for (GameUnit u:footmen){
	    		if (u.getId()!=unit.getId()){
	    			if (targetX==u.getXPosition() && targetY==u.getYPosition()){
	    				continue outerloop;
	    			}
	    		}
	    	}
	    	for (GameUnit u:archers){
	    		if (u.getId()!=unit.getId()){
	    			if (targetX==u.getXPosition() && targetY==u.getYPosition()){
	    				continue outerloop;
	    			}
	    			
	    		}
	    	}
	    	//for footmen, see if archer in attackrange and add attack action
	    	if (isFootmen(unit)){
	    		for (GameUnit enemy:archers){
	    	    	if (  Math.abs(enemy.getXPosition()-targetX)<=footmenAttackRange &&
	    	    			Math.abs(enemy.getYPosition()-targetY)<=footmenAttackRange){
	    	    		actionList.add(Action.createCompoundAttack(unit.getId(), enemy.getId()));
	    	    		continue outerloop;
	    	    	}
	    		}
	    	}
	    	actionList.add(Action.createPrimitiveMove(unit.getId(), direction));
    	}
    	//for archer, see if footmen in attackrange and add attack action
    	if (!isFootmen(unit)){
    		for (GameUnit enemy:footmen){
    	    	if (  Math.abs(enemy.getXPosition()-unit.getXPosition())<=archerAttackRange &&
    	    			Math.abs(enemy.getYPosition()-unit.getYPosition())<=archerAttackRange){
    	    		actionList.add(Action.createCompoundAttack(unit.getId(), enemy.getId()));
    	    	}
    		}
    	}
    	//check duplicate compound attack action in actionList
    	for (int i=0; i<actionList.size();i++){
    		for (int j=i+1; j<actionList.size(); j++){
    			if (actionList.get(i).deepEquals(actionList.get(j))){
    				actionList.remove(i);
    			}
    		}
    	}
    	
		return actionList;
    }
    
    private boolean isFootmen(GameUnit unit){
    	return unit.getId()==0 || unit.getId()==1;
    }
    
    /**
     * helper method of getChildren
     * find if unit1, unit2 moves to same location in the next state, based on their actions, only called when both action is PRIMITIVEMOE
     * @param actionOne action for unit one
     * @param actionTwo action for unit two
     * @return true if two action cause two units move to same location
     */
    private boolean moveToSameLocation(Action actionOne, Action actionTwo){
    	int x1=((DirectedAction)actionOne).getDirection().xComponent();
    	int x2=((DirectedAction)actionTwo).getDirection().xComponent();
    	int y1=((DirectedAction)actionOne).getDirection().yComponent();
    	int y2=((DirectedAction)actionTwo).getDirection().yComponent();
    	GameUnit unit1,unit2;
    	if (isFootmenTurn){
    		unit1=footmen.get(0);
    		unit2=footmen.get(1);
    	}
    	else{
    		unit1=archers.get(0);
    		unit2=archers.get(1);
    	}
    	boolean result=unit1.getXPosition()+x1 == unit2.getXPosition()+x2 && unit1.getYPosition()+y1 == unit2.getYPosition()+y2;
    	
    	return result;
    }
    
    /**
     * helper method of getChildren(), generate new state in generated GameStateChild based on action. 
     * This method is called state in children GameStateChild
     * @param actionMap action in GameStateChild 
     */
    public void simulateAction(Map<Integer,Action> actionMap){
    	for (int key : actionMap.keySet()){
    		//move unit by direction
    		if (actionMap.get(key).getType()==ActionType.PRIMITIVEMOVE){
    			getGameUnitByID(key).xPosition+=((DirectedAction)actionMap.get(key)).getDirection().xComponent();
    			getGameUnitByID(key).yPosition+=((DirectedAction)actionMap.get(key)).getDirection().yComponent();
    		}
    		//if action is attack, decrease enemy hp and try to move unit
    		else if (actionMap.get(key).getType()==ActionType.COMPOUNDATTACK){
    			GameUnit enemy=getGameUnitByID(((TargetedAction)actionMap.get(key)).getTargetId());
    			GameUnit unit=getGameUnitByID(((TargetedAction)actionMap.get(key)).getUnitId());
    			enemy.setHp(enemy.hp-enemy.maxHP/5);
    			if (enemy.getHp()<=0){
    				enemy.setHp(0);
    			}
    			//guess which move COMPOUNDATTACK does
    			if (enemy.xPosition==unit.xPosition){
    				if (enemy.yPosition-unit.yPosition==2){//unit is 2 tile above target, move south one tile and attack
    					unit.yPosition+=1;
    				}
    				else if (enemy.yPosition-unit.yPosition==-2){//unit is 2 tile below target, move north one tile and attack
    					unit.yPosition+=-1;
    				}
    				// else unit is already in attack position no need to move
    			}
    			else if (enemy.yPosition==unit.yPosition){
    				if (enemy.xPosition-unit.xPosition==2){//unit is 2 tile left target, move east one tile and attack
    					unit.xPosition+=1;
    				}
    				else if (enemy.xPosition-unit.xPosition==-2){//unit is 2 tile right target, move west one tile and attack
    					unit.xPosition+=-1;
    				}
    				// else unit is already in attack position no need to move
    			}
    		}
    	}
    	isFootmenTurn=!isFootmenTurn;
    }
    
    public GameUnit getGameUnitByID(int id){
    	if (id==0 || id==1){
    		for (GameUnit u: footmen){
    			if (u.getId()==id){
    				return u;
    			}
    		}
    	}
    	else{
    		for (GameUnit u: archers){
    			if (u.getId()==id){
    				return u;
    			}
    		}
    	}
    	return null;
    }
}
