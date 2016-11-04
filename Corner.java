package source;

import java.io.Serializable;

public class Corner implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int x;		// x coordinate
	private int y;		//y coordinate
	private double harrisCornerResponse;		// harris corner response a.k.a "cornerness"
	private boolean valid;			// true if corner should be taken into consideration (instead of removing the corner, non-maximum suppression makes this false)
	private int id;			// used for matching corners between subsequent frames (tracing)
	
	public Corner(int x, int y, double cornerResponse){
		
		this.x = x;
		this.y = y;
		this.harrisCornerResponse = cornerResponse;
		this.valid = true;
	}
	
	public int getX(){
		return this.x;
	}
	
	public int getY(){
		return this.y;
	}
	
	public double getCornerResponse(){
		return this.harrisCornerResponse;
	}
	
	public int getID(){
		
		return this.id;
	}
	
	public double getDistance(Corner c){
		
		double distance = 0;
		
		distance = Math.sqrt(Math.pow(this.x - c.getX(), 2) + Math.pow(this.y - c.getY(), 2));
		
		return distance;
	}
	
	public void setID(int id){
		
		this.id = id;
	}
	
	public void invalidate(){
		
		this.valid = false;
	}
	
	public boolean isValid(){
		
		return this.valid;
	}

}
