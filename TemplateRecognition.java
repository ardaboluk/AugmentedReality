package source;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;

public class TemplateRecognition {
	
	private CornerDetection cornerDetection = null;
	private EdgeDetection edgeDetection = null;
	private ObjectRecognition objectRecognition = null;
	private TemplateLoader templateLoader = null;
	
	public TemplateRecognition(){
		
		// initialize objects that deal with computer vision
		cornerDetection = new CornerDetection();
		edgeDetection = new EdgeDetection();
		objectRecognition = new ObjectRecognition();
		templateLoader = new TemplateLoader();
	}
	
	@SuppressWarnings("unchecked")
	public Object[] recognizeTemplateInImage(BufferedImage image){
		
		// width and height of the image
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		
		// get corners and edges of the image
		LinkedList<Corner> imageCorners = cornerDetection.findCorners(image, 1000000000);
		int imageLabeledEdgeRegions[] = edgeDetection.detectEdges(image);
		
		// load templates from files
		ArrayList<Template> templates = templateLoader.loadTemplates();
		
		// check templates if any of them match the image
		for(int i = 0; i < templates.size(); i++){
			
			Template currentTemplate = templates.get(i);			
			Object matchingOuterCorners[] = objectRecognition.findMatchingPoints(currentTemplate, imageCorners, imageLabeledEdgeRegions, imageWidth, imageHeight);
			
			LinkedList<Corner> templateOuterCorners = null;
			LinkedList<Corner> imageOuterCorners = null;
			if(matchingOuterCorners != null){
				
				templateOuterCorners = (LinkedList<Corner>)matchingOuterCorners[0];
				imageOuterCorners = (LinkedList<Corner>)matchingOuterCorners[1];
				if(templateOuterCorners != null && imageOuterCorners != null){
					
					if(templateOuterCorners.size() > 0 && imageOuterCorners.size() > 0){

						return new Object[]{templateOuterCorners, imageOuterCorners, currentTemplate.getTemplateName()};
					}

				}
			}		
			
		}		
		
		return null;
	}

}
