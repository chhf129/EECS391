//Haifeng Chen hxc334
//Michael Volkovitsch mtv25
package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

    static class MapLocation
    {
        public int x, y;
        public int utility=0;
        public float cost,fValue,hValue;//path cost, function cost, and heuristic cost for A* search
        public MapLocation cameFrom;// for A* search, the node that precedes this one in a path
        
        public MapLocation(int x, int y, MapLocation cameFrom, float cost) {
            this.x = x;
            this.y = y;
        }
        public MapLocation(int x, int y, MapLocation cameFrom, float cost, float hValue) {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
            this.hValue = hValue;
            this.fValue = this.cost + this.hValue;
        }
        public MapLocation(MapLocation map) {
            this.x = map.x;
            this.y = map.y;
            this.cameFrom = map.cameFrom;
            this.cost = map.cost;
            this.hValue = map.hValue;
            this.fValue = map.fValue;
            this.utility=map.utility;
        }
        public MapLocation(int x, int y, float hValue) {
            this.x = x;
            this.y = y;
            this.hValue = hValue;
        }
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                //System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        //System.out.println("Total turns: " + newstate.getTurnNumber());
        //System.out.println("Total planning time: " + totalPlanTime/1e9);
        //System.out.println("Total execution time: " + totalExecutionTime/1e9);
        //System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    
    //to check if enemy is moving, record enemy's position in previous frame
    private int enemyPreviousX = -1;
    private int enemyPreviousY = -1;
    private boolean enemyMoving = false;
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	//to check if running on a dynamic map
    	if(enemyFootmanID == -1)
    	{
    		return false;
    	}
        Unit.UnitView enemyFootman = state.getUnit(enemyFootmanID);
        //to check if enemy footman blocked our way
        for(MapLocation mapLocation : currentPath)
        {
        	if(mapLocation.x == enemyFootman.getXPosition() && mapLocation.y == enemyFootman.getYPosition())
        	{
        		return true;
        	}
        }
        //to check if enemy footman is staying
        if(enemyPreviousX == enemyFootman.getXPosition()&&enemyPreviousY == enemyFootman.getYPosition())
        {
        	//if yes, to check if enemy footman stopped for a long time, or just stopped
        	if(enemyMoving)
        	{
        		//if enemy footman was just stopped, we replan the path
        		enemyMoving = false;
        		return true;
        	}
        }
        else
        {
        	//if no, we know that the enemy is moving
        	enemyMoving = true;
            enemyPreviousX = enemyFootman.getXPosition();
            enemyPreviousY = enemyFootman.getYPosition();
        }
        return false;
    }


    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    public Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	//create MapLocation priority queue which prioritizes locations with smallest function value (path cost + heuristic)
    	class locComp implements Comparator<MapLocation>{
			@Override
			public int compare(MapLocation loc1, MapLocation loc2) {
				return Float.compare(loc1.fValue, loc2.fValue);
			}
    	}
    	PriorityQueue<MapLocation> openList=new PriorityQueue<MapLocation>(new locComp());
    	LinkedList<MapLocation> closedList = new LinkedList<MapLocation>();
    	//resource locations and the enemy footman can't be explored, and are equivalent to closed spots
    	closedList.addAll(resourceLocations);
    	if (enemyFootmanLoc != null){
    		closedList.add(enemyFootmanLoc);
    	}
    	//initialize extra values for start
    	start.hValue=Chebyshev(start,goal);
    	start.cost=0;
    	openList.add(start);
    	//will hold the path to return
    	Stack<MapLocation> path = new Stack<MapLocation>();
    	
    	//while there are nodes to explore, pop next location and explore it
    	while(!openList.isEmpty()){
        	MapLocation node = openList.poll();
    		if (this.ExploreNode(node, openList, closedList, goal, xExtent, yExtent)){
    			//ExploreNode returns true if it can reach the goal from the node it's exploring, so create the path and break out
    			path = CreatePath(node, start, closedList);
    			break;
    		}
    	}
    	if (path.isEmpty()){
    		//System.out.println("No available path.");
    		System.exit(0);
    	}
    	return path;
    }
    
    /**
     * This method takes a MapLocation and then checks its neighbors, making sure they are not out-of-bounds.
     * It then moves the MapLocatoin from the open list to closed list.
     *
     * @param node Location that is being explored
     * @param openList List of MapLocations to explore
     * @param closedList List of unexplorable/already explored locations
     * @param goal The town hall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @return returns true if it can reach the goal, else false
     */
    private boolean ExploreNode(MapLocation node, PriorityQueue<MapLocation> openList, LinkedList<MapLocation> closedList, MapLocation goal, int xExtent, int yExtent){
    	//cycle through all 8 neighbors
    	for (int x = node.x-1; x < node.x+2; x++){
    		for (int y = node.y-1; y < node.y+2; y++){
    			//don't check locations that are out-of-bounds or the current node
    			if ((Math.abs(node.x-x+node.y-y) == 1) && !(x<0 || x>=xExtent || y<0 || y>=yExtent || (x==node.x && y==node.y))){
    				if (x == goal.x && y == goal.y){
    					return true;
    				} else {
    					this.ExamineNeighbor(x, y, node, goal, openList, closedList);
    				}
    			}
    		}
    	}
    	openList.remove(node);
		closedList.addFirst(node);
    	return false;
    }
    
    /**
     * Examines a neighboring map location. If it is already in the open list, it updates the parent of that location according to which
     * previous location has the shortest path cost leading to it. If the location isn't in the open list or closed list, it initiates the
     * A* values of the location (path cost, heuristic cost, function cost, parent) and adds it to the open list.
     * 
     * @param x
     * @param y
     * @param node
     * @param goal
     * @param openList
     * @param closedList
     */
    private void ExamineNeighbor(int x, int y, MapLocation node, MapLocation goal, PriorityQueue<MapLocation> openList, LinkedList<MapLocation> closedList){
		boolean valid = true;
		//check in closed list
		for (MapLocation m: closedList){
			valid = valid && (x!=m.x || y!=m.y);
		}
		//check in open list
		if (valid) {
			for (MapLocation m : openList) {
				if (x == m.x && y == m.y) {
					valid = false;
					if (m.cost - 1 > node.cost) {
						m.cost = node.cost + 1;
						m.cameFrom = node;
					}
				}
			} 
		}
		//if not in open or closed lists, add location to the 
		if (valid){
			MapLocation l = new MapLocation(x, y, node, node.cost+1, 0);
			l.hValue = Chebyshev(l, goal);
			l.fValue = l.cost+l.hValue;
			openList.add(l);
		}
    }
    
    /**
     * Uses the closed list to create a path to the goal by tracing the path backwards from the goal by examining which
     * location came before it, and then before that one, ... , until it reaches the starting location
     * 
     * @param end The last location in the path
     * @param start The starting location
     * @param closedList The closed list from A*
     * @return a Stack containing the path to the goal (not including starting location or goal)
     */
    private static Stack<MapLocation> CreatePath(MapLocation end, MapLocation start, LinkedList<MapLocation> closedList){
    	Stack<MapLocation> path = new Stack<MapLocation>();
    	path.push(end);
    	if (end.cameFrom != null) {
			MapLocation loc = end.cameFrom;
			//trace path back until we find the starting location
			while (loc.x != start.x || loc.y != start.y) {
				for (MapLocation m : closedList) {
					if (m.x == loc.x && m.y == loc.y) {
						path.push(loc);
						loc = m.cameFrom;
						break;
					}
				}
			} 
		}
		return path;
    }
    
    /**
     * Computes the Chebyshev distance between two locations as the heuristic for A*
     * 
     * @param start Location to determine heuristic for
     * @param goal Location of the goal
     * @return the Chebyshev distance between the two locations
     */
    private static int Chebyshev(MapLocation start, MapLocation goal){
    	return Math.max(Math.abs(start.x-goal.x), Math.abs(start.y-goal.y));
    }

    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
