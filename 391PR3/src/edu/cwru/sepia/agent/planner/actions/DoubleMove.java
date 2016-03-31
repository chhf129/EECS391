package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

/**
 * Implements two moves in parallel by performing them sequentially
 * but only increasing the cost by the longer move.
 */
public class DoubleMove implements StripsAction{
	public int unit1, unit2;
	public Position start1, start2, end1, end2;
	public StripsMove move1, move2;
	
	
	public DoubleMove(int id1, int id2, Position s1, Position s2, Position e1, Position e2){
		unit1 = id1;
		unit2 = id2;
		start1 = s1;
		start2 = s2;
		end1 = e1;
		end2 = e2;
		move1 = new StripsMove(id1, s1, e1);
		move2 = new StripsMove(id2, s2, e2);
	}
	
	public DoubleMove(){}
	
	
	@Override
	public boolean preconditionsMet(GameState state) {
		return move1.preconditionsMet(state) && move2.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState mid = move1.apply(state);
		double cost = mid.cost-state.cost;
		GameState newState = move2.apply(mid);
		cost = Math.min(cost, newState.cost-mid.cost);
		newState.cost = state.cost + cost;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause = this;
		
		return newState;
	}

	public String toString(){
		return "DoubleMove:\n\t" + move1.toString() + "\n\t" + move2.toString();
	}
}
