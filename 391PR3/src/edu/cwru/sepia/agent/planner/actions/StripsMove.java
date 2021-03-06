package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.AstarAgent;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.ResourceInfo;

public class StripsMove implements StripsAction {
	public Position start, end;
	public int unitID;
	public int costOffSet;
	
	public StripsMove (int id, Position s, Position e, int costOffSet){
		start = s;
		end = e;
		unitID = id;
		this.costOffSet=costOffSet;
	}
	public StripsMove (StripsMove s){
		start = s.start;
		end = s.end;
		unitID = s.unitID;
		costOffSet=s.costOffSet;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		//test that end is not out of bounds
		if (!end.inBounds(state.xBound, state.yBound)){
			return false;
		}
		//test that the end isn't the town hall
		if (end.equals(state.townHall.location)){
			return false;
		}
		//test that end is not a resource
		for (ResourceInfo ri: state.goldmines){
			if (end.equals(ri.location)){
				return false;
			}
		}
		for (ResourceInfo ri: state.forests){
			if (end.equals(ri.location)){
				return false;
			}
		}
		boolean validPeasant = false;
		//test the unit is at the start position and the end is unoccupied
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				if (start.equals(p.location)){
					validPeasant = true;
				}
			}
			if (end.equals(p.location)){
				return false;
			}
		}
		if (validPeasant){
			return true;
		}
		return false;
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = new GameState(state);
		for (Peasant p: newState.peasants){
			if (p.id == unitID){
				p.location = new Position(end.x, end.y);
			}
		}
		//TODO add length of A* path to cost
		AstarAgent a = new AstarAgent();
		newState.cost += a.findPath(state, unitID, end).size()+costOffSet;
		newState.heuristic = newState.heuristic();
		newState.cause.add(this);
		newState.parent = state;
		return newState;
	}
	
	public String toString(){
		return "Move: peasant " + unitID + " from (" + start.x + ", " + start.y + ") to (" + end.x + ", " + end.y + ")";
		
		
	}

}
