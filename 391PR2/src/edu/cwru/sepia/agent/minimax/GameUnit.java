package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.environment.model.state.Unit;

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