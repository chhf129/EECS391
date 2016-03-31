package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Position;

public class BuildPeasant implements StripsAction {

	@Override
	public boolean preconditionsMet(GameState state) {
		return state.townHall.food > 0 && state.townHall.gold >= 400 && findPeasantSpawn(state) != null;
	}

	@Override
	public GameState apply(GameState state) {
		Position spawn = findPeasantSpawn(state);
		GameState newState = new GameState(state);
		newState.townHall.gold -= 400;
		newState.townHall.food--;
		Peasant newPeasant = new Peasant(newState.peasants.get(newState.peasants.size()-1).id + 1, spawn);
		newState.peasants.add(newPeasant);
		newState.cost++;
		newState.heuristic = state.heuristic();
		return newState;
	}
	
	//returns an open Position next to the townhall to spawn a peasant in or null if no such Position exists
	public Position findPeasantSpawn(GameState state){
		for (Position pos: state.townHall.location.getAdjacentPositions()){
			if (state.checkOpenPosition(pos)){
				return pos;
			}
		}
		return null;
	}

}
