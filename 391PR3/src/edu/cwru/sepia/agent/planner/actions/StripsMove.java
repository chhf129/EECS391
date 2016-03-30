package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.AstarAgent;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Peasant;
import edu.cwru.sepia.agent.planner.GameState.ResourceInfo;
import edu.cwru.sepia.agent.planner.Position;

public class StripsMove implements StripsAction {
	private Position start, end;
	private int unitID;
	
	public StripsMove (int id, Position s, Position e){
		start = s;
		end = e;
		unitID = id;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		//test that end is not out of bounds
		if (end.inBounds(state.xBound, state.yBound)){
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
					validPeasant = false;
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
		newState.cost += a.findPath(state, unitID, end).size();
		newState.heuristic = newState.heuristic();
		newState.cause = this;
		return newState;
	}

}
