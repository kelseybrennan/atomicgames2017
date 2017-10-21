package com.atomicobject.rts;

import java.util.ArrayList;

public class GameTile {

	boolean blocked = false;
	boolean visible = false;
	TileResource resource = null;
	ArrayList<Unit> units = null;
	
	public GameTile() {
		
	}
	
	public ArrayList<Unit> getUnits() {
		return units;
	}

	public void setUnits(ArrayList<Unit> units) {
		this.units = units;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public boolean isVisible() {
		return visible;
	}

	public TileResource getResource() {
		return resource;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setResource(TileResource resource) {
		this.resource = resource;
	}
	
	public void setunits(ArrayList<Unit> units) {
		this.units = units;
	}
}
