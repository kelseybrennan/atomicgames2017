package com.atomicobject.rts;

public class GameTile {

	boolean blocked = false;
	boolean visible = false;
	TileResource resource = null;
	
	public GameTile() {
		
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
}
