package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;

/**
 * Contains the useful information of a resource (wood or gold) and allows
 * the information to be easily accessed, changed, and copied. Created for
 * ease of use in handling GameState objects.
 */

public class ResourceInfo{
	public ResourceNode.Type type;
	public int amount,id;
	public Position location;
	
	public ResourceInfo(ResourceView rv){
		type = rv.getType();
		amount = rv.getAmountRemaining();
		location = new Position(rv.getXPosition(), rv.getYPosition());
		id=rv.getID();
	}
	
	public ResourceInfo(ResourceInfo ri){
		type = ri.type;
		amount = ri.amount;
		location = new Position(ri.location.x, ri.location.y);
		id=ri.id;
	}
}