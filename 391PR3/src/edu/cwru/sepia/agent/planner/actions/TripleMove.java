package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

/**
 * Implements three moves in parallel by performing them sequentially
 * but only increasing the cost by the longest move.
 */
public class TripleMove extends DoubleMove implements StripsAction {
	public int unit1, unit2, unit3;
	public Position start1, start2, start3, end1, end2, end3;
	public StripsMove move1, move2, move3;
	
	
	public TripleMove(int id1, int id2, int id3, Position s1, Position s2, Position s3, Position e1, Position e2, Position e3){
		super(id1,id2,s1,s2,e1,e2);
		unit1 = id1;
		unit2 = id2;
		unit3 = id3;
		start1 = s1;
		start2 = s2;
		start3 = s3;
		end1 = e1;
		end2 = e2;
		end3 = e3;
		move1 = new StripsMove(unit1, start1, end1,0);
		move2 = new StripsMove(unit2, start2, end2,0);
		move3 = new StripsMove(unit3, start3, end3,0);
	}
	
	public TripleMove(StripsMove m1,StripsMove m2,StripsMove m3){
		super(m1,m2);
		unit3=m3.unitID;
		start3=m3.start;
		end3=m3.end;
		move3=new StripsMove(m3);
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		return move1.preconditionsMet(state) && move2.preconditionsMet(state) && move3.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = move1.apply(state);
		double cost = newState.cost - state.cost;
		newState.cost = state.cost;
		newState = move2.apply(newState);
		cost = Math.min(cost, newState.cost - state.cost);
		newState.cost = state.cost;
		newState = move3.apply(newState);
		cost = Math.min(cost, newState.cost - state.cost);
		newState.cost = state.cost + cost;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause = this;
		
		return newState;
	}

	public String toString(){
		return "TripleMove:\n\t" + move1.toString() + "\n\t" + move2.toString() + "\n\t" + move3.toString();
	}
}
