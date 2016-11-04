package source;

import java.io.Serializable;
import java.util.*;

public class Template implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String templateName;
	private int templateWidth;
	private int templateHeight;
	private LinkedList<Corner> templateCorners;
	private int templateLabeledEdgeRegions[];
	
	// these corners will be used for solving homography correctly
	private Corner upperLeftCorner = null;
	private Corner lowerLeftCorner = null;
	private Corner upperRightCorner = null;
	private Corner lowerRightCorner = null;
	
	public Template(String name, LinkedList<Corner> corners, int labeledEdgeRegions[], int width, int height, Corner upperLeftCorner, Corner lowerLeftCorner,
			Corner upperRightCorner, Corner lowerRightCorner){
		
		templateName = name;
		templateWidth = width;
		templateHeight = height;
		templateCorners = corners;
		templateLabeledEdgeRegions = labeledEdgeRegions;
		
		this.upperLeftCorner = upperLeftCorner;
		this.lowerLeftCorner = lowerLeftCorner;
		this.upperRightCorner = upperRightCorner;
		this.lowerRightCorner = lowerRightCorner;
	}
	
	public String getTemplateName(){
		return templateName;
	}
	
	public int getTemplateWidth(){
		return templateWidth;
	}
	
	public int getTemplateHeight(){
		return templateHeight;
	}
	
	public LinkedList<Corner> getTemplateCorners(){
		return templateCorners;
	}
	
	public int[] getTemplateLabeledEdgeRegions(){
		return templateLabeledEdgeRegions;
	}
	
	public Corner getUpperLeftCorner(){
		return upperLeftCorner;
	}
	
	public Corner getLowerLeftCorner(){
		return lowerLeftCorner;
	}
	
	public Corner getUpperRightCorner(){
		return upperRightCorner;
	}
	
	public Corner getLowerRightCorner(){
		return lowerRightCorner;
	}

}
