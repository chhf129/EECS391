package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

/**
 * Contains the useful information of a peasant unit and allows
 * the information to be easily accessed, changed, and copied.
 * Created for ease of use in handling GameState objects.
 */

public class Peasant{
	public int id;
	public boolean isCarrying;
	public ResourceNode.Type resourceType;
	public int resourceAmount;
	public Position location;
	
	public Peasant(UnitView uv){
		id = uv.getID();
		isCarrying = false;
		resourceType = null;
		resourceAmount = 0;
		location = new Position(uv.getXPosition(), uv.getYPosition());
	}
	
	public Peasant(Peasant p){
		id = p.id;
		isCarrying = p.isCarrying;
		resourceType = p.resourceType;
		resourceAmount = p.resourceAmount;
		location = new Position(p.location.x, p.location.y);
	}
	
	public Peasant(int id, Position pos){
		this.id = id;
		isCarrying = false;
		resourceType = null;
		resourceAmount = 0;
		location = new Position(pos.x, pos.y);
	}
}