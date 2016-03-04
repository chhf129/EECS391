package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.environment.model.state.Unit;

/*
 * Used to keep track of a unit's information throughout alpha beta search
 */
public class GameUnit {
	public int xPosition,yPosition;
	public int hp;
	public int maxHP;
	public int id;
	public GameUnit(Unit.UnitView unit){
		xPosition=unit.getXPosition();
		yPosition=unit.getYPosition();
		hp=unit.getHP();
		maxHP=unit.getTemplateView().getBaseHealth();
		id=unit.getID();
	}
	public GameUnit(GameUnit unit){
		xPosition=unit.getXPosition();
		yPosition=unit.getYPosition();
		hp=unit.getHp();
		maxHP=unit.maxHP;
		id=unit.getId();
	}
	public int getXPosition() {
		return xPosition;
	}
	public void setXPosition(int xPosition) {
		this.xPosition = xPosition;
	}
	public int getYPosition() {
		return yPosition;
	}
	public void setYPosition(int yPosition) {
		this.yPosition = yPosition;
	}
	public int getHp() {
		return hp;
	}
	public void setHp(int hp) {
		this.hp = hp;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
}
