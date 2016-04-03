package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.BuildPeasant;
import edu.cwru.sepia.agent.planner.actions.DepositGold;
import edu.cwru.sepia.agent.planner.actions.DepositRes;
import edu.cwru.sepia.agent.planner.actions.DepositWood;
import edu.cwru.sepia.agent.planner.actions.DoubleDeposit;
import edu.cwru.sepia.agent.planner.actions.DoubleGather;
import edu.cwru.sepia.agent.planner.actions.DoubleMove;
import edu.cwru.sepia.agent.planner.actions.GatherGold;
import edu.cwru.sepia.agent.planner.actions.GatherRes;
import edu.cwru.sepia.agent.planner.actions.GatherWood;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.agent.planner.actions.StripsMove;
import edu.cwru.sepia.agent.planner.actions.TripleDeposit;
import edu.cwru.sepia.agent.planner.actions.TripleGather;
import edu.cwru.sepia.agent.planner.actions.TripleMove;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
	
	//useful information
	public int goldGoal, woodGoal;
	public int xBound, yBound;
	public double cost, heuristic;
	public int playerID;
	public List<ResourceInfo> goldmines, forests;
	public List<Peasant> peasants;
	public TownHallInfo townHall;
	public GameState parent;
	public StripsAction cause;
	public boolean buildPeasants;
	
    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
        playerID = playernum;
    	goldGoal = requiredGold;
        woodGoal = requiredWood;
        this.buildPeasants = buildPeasants;
        xBound = state.getXExtent();
        yBound = state.getYExtent();
        
        goldmines = new LinkedList<ResourceInfo>();
        forests = new LinkedList<ResourceInfo>();
        peasants = new LinkedList<Peasant>();
        List<ResourceView> resources = state.getAllResourceNodes();
        for (ResourceView rv: resources){
        	if (rv.getType() == ResourceNode.Type.GOLD_MINE){
        		goldmines.add(new ResourceInfo(rv));
        	} else {
        		forests.add(new ResourceInfo(rv));
        	}
        }
        List<UnitView> units = state.getUnits(playerID);
        for(UnitView uv: units){
        	if (uv.getTemplateView().getName().equals("TownHall")){
        		townHall = new TownHallInfo(uv);
        	} else {
        		peasants.add(new Peasant(uv));
        	}
        }
    }
    
    //clones a GameState
    public GameState (GameState gs){
    	playerID = gs.playerID;
    	goldGoal = gs.goldGoal;
    	woodGoal = gs.woodGoal;
    	buildPeasants = gs.buildPeasants;
    	xBound = gs.xBound;
    	yBound = gs.yBound;
    	townHall = new TownHallInfo(gs.townHall);
    	goldmines = new LinkedList<ResourceInfo>();
    	for (ResourceInfo ri: gs.goldmines){
    		goldmines.add(new ResourceInfo(ri));
    	}
    	forests = new LinkedList<ResourceInfo>();
    	for (ResourceInfo ri: gs.forests){
    		forests.add(new ResourceInfo(ri));
    	}
    	peasants = new LinkedList<Peasant>();
    	for (Peasant p: gs.peasants){
    		peasants.add(new Peasant(p));
    	}
    	parent=gs.parent;
    	cause=gs.cause;
    	cost = gs.cost;
    	heuristic = gs.heuristic;
    }
    
    /**
     * Checks if a Position is in bounds and unoccupied
     * 
     * @param p the Position to check
     * @return true it the position is in bounds and empty
     */
    public boolean checkOpenPosition(Position p){
    	boolean empty = p.inBounds(xBound, yBound) && !p.equals(townHall.location);
    	if (empty){
    		for(ResourceInfo ri: goldmines){
    			if (p.equals(ri.location)){
    				empty = false;
    				break;
    			}
    		}
    		for(ResourceInfo ri: forests){
    			if (p.equals(ri.location)){
    				empty = false;
    				break;
    			}
    		}
    		for(Peasant peas: peasants){
    			if (p.equals(peas.location)){
    				empty = false;
    				break;
    			}
    		}
    	}
    	return empty;
    }
    
    public void printStatus(){
    	System.out.println("State");
    	System.out.println("\tTH: " + townHall.gold + " gold, " + townHall.wood + " wood, " + townHall.food + " food");
    	System.out.println("\tPeasants:");
    	for (Peasant p: peasants){
    		System.out.print("\t\t" + p.id + " at ("+p.location.x+", "+p.location.y+")");
    		if (p.isCarrying){
    			System.out.print(" carrying " + p.resourceAmount + " " + p.resourceType.name());
    		}
    		System.out.println();
    	}
    	System.out.println("\tGoldmines");
    	for (ResourceInfo ri: goldmines){
    		System.out.println("\t\t" + ri.id + " has " + ri.amount);
    	}
    	System.out.println("\tForests");
    	for (ResourceInfo ri: forests){
    		System.out.println("\t\t" + ri.id + " has " + ri.amount);
    	}
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
    	System.out.println(goldGoal+","+woodGoal);
        return townHall.gold == goldGoal && townHall.wood == woodGoal;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
    	List<GameState> returnList=new LinkedList<>();
    	List<GameState> returnList1=new LinkedList<>();
    	returnList=getAllChildren(peasants.get(0));
    	if (peasants.size()==1){
        	for(GameState it:returnList){
        		System.out.println("children\n"+it.cause.toString());
        	}
    		return returnList;
    	}
    	else {
    		for(GameState gameState:returnList){
    			returnList1.addAll(gameState.getAllChildren(peasants.get(1)));
    		}
    		for(GameState gameState:returnList1){
    			gameState.combineState();
    		}
    	}
    	if (peasants.size()==3){
    		List<GameState> returnList2=new LinkedList<>();
    		for(GameState gameState:returnList1){
    			returnList2.addAll(gameState.getAllChildren(peasants.get(2)));
    		}
    		for(GameState gameState:returnList1){
    			gameState.combineState();
    		}
        	for(GameState it:returnList){
        		System.out.println("children\n"+it.cause.toString());
        	}
    		return returnList2;
    	}
    	else{
        	for(GameState it:returnList){
        		System.out.println("children\n"+it.cause.toString());
        	}
    		return returnList1;
    	}
    }
    
    //TODO: put move action together into deposit or gather
    private List<GameState> getAllChildren(Peasant peasnt){
      	List<GameState> returnList = new LinkedList<>();
    	if (peasnt.isCarrying){
    		DepositRes depositR=new DepositRes(peasnt.id);
    		if (depositR.preconditionsMet(this)){
    			returnList.add(depositR.apply(this));
    		}
    		/*
    		DepositGold depositG=new DepositGold(peasnt.id);
    		DepositWood depositW=new DepositWood(peasnt.id);
    		if (depositG.preconditionsMet(this)){
    			returnList.add(depositG.apply(this));
    		}
    		else if (depositW.preconditionsMet(this)){
        		returnList.add(depositW.apply(this));
    		}
    		*/
    		else{
    			Position minPos=findClosestTile(townHall.location,peasnt.location);
    			GameState afterMove=new GameState(this);
    			StripsMove move=new StripsMove(peasnt.id,peasnt.location,minPos,0);
    			if (move.preconditionsMet(afterMove)){//I can't figure when this if statement would be false
    				afterMove=move.apply(this);
    			}
    			returnList.add(afterMove);
    		}
    	}
    	
    	else{
			// goldTarget and woodTarget is the closest gold mine/tree to
			// peasant(0), while still has resource remain
			double minDis = Double.MAX_VALUE;
			ResourceInfo goldTarget = null;
			ResourceInfo woodTarget = null;
			if (townHall.gold < goldGoal) {
				for (ResourceInfo r : goldmines) {
					if (r.amount > 0) {
						if (r.location.euclideanDistance(townHall.location) < minDis) {
							minDis = r.location.euclideanDistance(townHall.location);
							goldTarget = r;
						}
					}
				}
				GatherRes gatherR = new GatherRes(peasnt.id, goldTarget.id);
				if (gatherR.preconditionsMet(this)) {
					returnList.add(gatherR.apply(this));
				} 
				else {
					Position closestG = findClosestTile(goldTarget.location, peasnt.location);
					StripsMove move = new StripsMove(peasnt.id, peasnt.location, closestG,-10);
					if (move.preconditionsMet(this)) { // this should always true,right?
						GameState afterMove = move.apply(this);
						returnList.add(afterMove);
					}
				}
			}
			
			if (townHall.wood < woodGoal) {
				minDis = Double.MAX_VALUE;
				
				for (ResourceInfo r : forests) {
					if (r.amount > 0) {
						if (r.location.euclideanDistance(townHall.location) < minDis) {
							minDis = r.location.euclideanDistance(townHall.location);
							woodTarget = r;
						}
					}
				}
				GatherRes gatherR = new GatherRes(peasnt.id, woodTarget.id);
				if (gatherR.preconditionsMet(this)) {
					returnList.add(gatherR.apply(this));
				} 
				else {
					Position closestW = findClosestTile(woodTarget.location, peasnt.location);
					StripsMove move = new StripsMove(peasnt.id, peasnt.location, closestW,10);
					if (move.preconditionsMet(this)) { // this should always true,right?
						GameState afterMove = move.apply(this);
						returnList.add(afterMove);
					}
				}
			}
		}
    	BuildPeasant build=new BuildPeasant();
    	if (build.preconditionsMet(this)){
    		GameState temp=build.apply(this);
    		returnList.add(temp);
    	}
    	
    	 return returnList;
    }
    
    private void combineState(){
    	StripsAction parentCause=parent.cause;
    	StripsAction result=null;;
    	if (cause instanceof DepositRes){
    		if (parentCause instanceof DepositRes){
    			result=new DoubleDeposit((DepositRes)parentCause,(DepositRes)cause);
    		}
    		else if (parentCause instanceof DoubleDeposit){
    			result=new TripleDeposit(((DoubleDeposit)parentCause).deposit1,((DoubleDeposit)parentCause).deposit2,(DepositRes)cause);
    		}
		}
		else if (cause instanceof GatherRes){
			if (parentCause instanceof GatherRes){
				result=new DoubleGather((GatherRes)parentCause,(GatherRes)cause);
			}
    		else if (parentCause instanceof DoubleGather){
    			result=new TripleGather(((DoubleGather)parentCause).gather1,((DoubleGather)parentCause).gather2,(GatherRes)cause);
    		}
		}
		else if (cause instanceof StripsMove){
			if (parentCause instanceof StripsMove){
				result=new DoubleMove((StripsMove)parentCause,(StripsMove)cause);
			}
    		else if (parentCause instanceof DoubleMove){
    			result=new TripleMove(((DoubleMove)parentCause).move1,((DoubleMove)parentCause).move2,(StripsMove)cause);
    		}
		}
    	if (result!=null){
    		cause=result;
    		parent=parent.parent;
    	}
    	else{
    		GameState gs=parent;
        	playerID = gs.playerID;
        	goldGoal = gs.goldGoal;
        	woodGoal = gs.woodGoal;
        	buildPeasants = gs.buildPeasants;
        	townHall = new TownHallInfo(gs.townHall);
        	goldmines = new LinkedList<ResourceInfo>();
        	for (ResourceInfo ri: gs.goldmines){
        		goldmines.add(new ResourceInfo(ri));
        	}
        	forests = new LinkedList<ResourceInfo>();
        	for (ResourceInfo ri: gs.forests){
        		forests.add(new ResourceInfo(ri));
        	}
        	peasants = new LinkedList<Peasant>();
        	for (Peasant p: gs.peasants){
        		peasants.add(new Peasant(p));
        	}
        	parent=gs.parent;
        	cause=gs.cause;
        	cost = gs.cost;
        	heuristic = gs.heuristic;
    	}
    }
    
    
    	private Position findClosestTile(Position a,Position b){
			List<Position> adjPosition = a.getAdjacentPositions();
			List<Position> toBeRemoved=new LinkedList<>();
			for (Position p:adjPosition){
				if (!checkOpenPosition(p) || !p.inBounds(xBound, yBound)){
					toBeRemoved.add(p);
				}
			}
			adjPosition.removeAll(toBeRemoved);
			Position minPos=adjPosition.get(0);
			for (Position p:adjPosition){
				if (p.euclideanDistance(b)<minPos.euclideanDistance(b) ){
					minPos=p;
				}
			}
			return minPos;
    	}
    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Estimates the cost based on the amount of resource that still need to be collected. Prioritizes gold
     * over wood (since it can be used to build peasants) and building peasants over other actions.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {
        double h = goldGoal - townHall.gold + woodGoal - townHall.wood;
        for (Peasant p: peasants){
        	if (p.isCarrying && p.resourceType == ResourceNode.Type.GOLD_MINE){
        		h -= 50;
        	} else if (p.isCarrying && p.resourceType == ResourceNode.Type.TREE){
        		h -= 25;
        	}
        }
        return h/peasants.size();
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * Cost is tracked during the A* search, so this just returns the tracked cost.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        return cost;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
        return Double.compare(this.cost+this.heuristic, o.cost+o.heuristic);
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * Determines equality based on town hall and resource contents as well as
     * peasant attributes including location.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
    	if (!(o instanceof GameState)){
    		return false;
    	}
    	GameState gs = (GameState)o;
    	boolean same = townHall.food == gs.townHall.food && townHall.gold ==
    			gs.townHall.gold && townHall.wood == gs.townHall.wood;
    	if (peasants.size() == gs.peasants.size()){
    		for (int i=0; i<peasants.size(); i++){
    			Peasant p1 = peasants.get(i);
    			Peasant p2 = gs.peasants.get(i);
    			same = same && (p1.isCarrying == p2.isCarrying && p1.resourceType ==
    					p2.resourceType && p1.resourceAmount == p2.resourceAmount &&
    					p1.location.equals(p2.location));
    		}
    	}
    	for(int i=0; i<forests.size(); i++){
    		same = same && forests.get(i).amount == gs.forests.get(i).amount;
    	}
    	for(int i=0; i<goldmines.size(); i++){
    		same = same && goldmines.get(i).amount == gs.goldmines.get(i).amount;
    	}
        return same;
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        // TODO: Implement me!
    	System.out.println("ERROR USED GAMESTATE.HASHCODE()");
        return 0;
    }
}
