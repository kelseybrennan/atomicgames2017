package com.atomicobject.rts;

public class AStarNode {
	public AStarNode parent;
	public int x;
	public int y;
	
	public int gValue;
	public int hValue;
	public boolean isWall = false;
	
	public AStarNode(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void setGValue(int amt) {
		this.gValue = amt;
	}
	
	public void calculateHValue(AStarNode dest) {
		this.hValue = Math.abs(this.x - dest.x) + Math.abs(this.y - dest.y);
	}
	
	public void calculateGValue(AStarNode point) {
		this.gValue = point.gValue + 1;
	}
	
	public int getFValue() {
		return this.gValue + this.hValue;
	}

}