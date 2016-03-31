package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.Unit.UnitView;

/**
 * Contains the useful information of a TownHall unit and allows
 * the information to be easily accessed, changed, and copied. Created
 * for ease of use in handling GameState objects.
 */

public class TownHallInfo{
	public int wood;
	public int gold;
	public int food;
	public Position location;
	
	public TownHallInfo(UnitView uv){
		wood = 0;
		gold = 0;
		food = 3;
		location = new Position(uv.getXPosition(), uv.getYPosition());
	}
	
	public TownHallInfo(TownHallInfo thi){
		wood = thi.wood;
		gold = thi.gold;
		food = thi.food;
		location = new Position(thi.location.x, thi.location.y);
	}
}