package source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;


public class ObjectRecognition {


	// finds template points in the image and the affine transformation that matches the template points to the image points
	public Object[] findMatchingPoints(Template template, LinkedList<Corner> imageCorners, int[] imageLabeledEdgeRegions,
			int imageWidth, int imageHeight){
		
		LinkedList<Corner> templateCorners = template.getTemplateCorners();
		int[] templateLabeledEdgeRegions = template.getTemplateLabeledEdgeRegions();
		int templateWidth = template.getTemplateWidth();
		int templateHeight = template.getTemplateHeight();

		// corners on the template that match the ones on the image
		LinkedList<Corner> matchedTemplateCorners = null;
		// points on the image that match with the ones on the template
		LinkedList<Corner> matchedImageCorners = null;

		// determine the threshold
		int inlierThreshold = (templateCorners.size() * 90) / 100;

		// find connected corner groups on the template 
		Map<Integer, ArrayList<Corner>> templateConnectedCornerGroups = findConnectedCorners(templateCorners, templateLabeledEdgeRegions, templateWidth, templateHeight);
		// find connected corner groups on the image
		Map<Integer, ArrayList<Corner>> imageConnectedCornerGroups = findConnectedCorners(imageCorners, imageLabeledEdgeRegions, imageWidth, imageHeight);

		// this variable states the number of corners in the group which is currently being checked
		int cornersInAGroup = 5;

		// try groups with cornersInAGroup number of corners from both template and image
		while(cornersInAGroup > 2){
			for(Map.Entry<Integer, ArrayList<Corner>> currentTemplateCornerGroupEntry : templateConnectedCornerGroups.entrySet()){

				if(currentTemplateCornerGroupEntry.getValue().size() == cornersInAGroup){

					for(Map.Entry<Integer, ArrayList<Corner>> currentImageCornerGroupEntry : imageConnectedCornerGroups.entrySet()){

						if(currentImageCornerGroupEntry.getValue().size() == cornersInAGroup){

							// get current template corner group
							ArrayList<Corner> currentTemplateCornerGroup = currentTemplateCornerGroupEntry.getValue();
							// get current image corner group
							ArrayList<Corner> currentImageCornerGroup = currentImageCornerGroupEntry.getValue();

							// get three corners from the template
							Corner templateCorner1 = currentTemplateCornerGroup.get(0);
							Corner templateCorner2 = currentTemplateCornerGroup.get(1);
							Corner templateCorner3 = currentTemplateCornerGroup.get(2);

							// get 3 corners from currentImageCornerGroup in combination
							for(int imageCornerIndex1 = 0; imageCornerIndex1 < currentImageCornerGroup.size(); imageCornerIndex1++){

								for(int imageCornerIndex2 = imageCornerIndex1 + 1; imageCornerIndex2 < currentImageCornerGroup.size(); imageCornerIndex2++){

									for(int imageCornerIndex3 = imageCornerIndex2 + 1; imageCornerIndex3 < currentImageCornerGroup.size(); imageCornerIndex3++){

										Corner imageCorner1 = currentImageCornerGroup.get(imageCornerIndex1);
										Corner imageCorner2 = currentImageCornerGroup.get(imageCornerIndex2);
										Corner imageCorner3 = currentImageCornerGroup.get(imageCornerIndex3);

										// track the current permutation of the image points
										int imagePointsPermutationCount = 0;
										while(imagePointsPermutationCount < 6){
											// find the affine transformation that match the points on the template to the points on the image
											double[] affineMatrixUpper = solve3x3Equation(new double[]{templateCorner1.getX(), templateCorner1.getY(), 1, imageCorner1.getX(), 
													templateCorner2.getX(), templateCorner2.getY(), 1, imageCorner2.getX(), templateCorner3.getX(), templateCorner3.getY(), 1, imageCorner3.getX()});
											double[] affineMatrixLower = solve3x3Equation(new double[]{templateCorner1.getX(), templateCorner1.getY(), 1, imageCorner1.getY(), 
													templateCorner2.getX(), templateCorner2.getY(), 1, imageCorner2.getY(), templateCorner3.getX(), templateCorner3.getY(), 1, imageCorner3.getY()});

											if(affineMatrixUpper != null && affineMatrixLower != null){

												// apply the affine transformation to other points on the template and get the inliers
												matchedTemplateCorners = new LinkedList<Corner>();
												matchedImageCorners = new LinkedList<Corner>();
												for(int i = 0; i < templateCorners.size(); i++){

													Corner currentTemplatePoint = templateCorners.get(i);
													double newTemplatePointX = affineMatrixUpper[0] * currentTemplatePoint.getX() + affineMatrixUpper[1] * currentTemplatePoint.getY() + affineMatrixUpper[2];
													double newTemplatePointY = affineMatrixLower[0] * currentTemplatePoint.getX() + affineMatrixLower[1] * currentTemplatePoint.getY() + affineMatrixLower[2];

													for(int j = 0; j < imageCorners.size(); j++){

														Corner currentImagePoint = imageCorners.get(j);
														double distanceTolerance = 10;
														if(Math.sqrt(Math.pow(currentImagePoint.getX() - newTemplatePointX, 2) + Math.pow(currentImagePoint.getY() - newTemplatePointY, 2)) < 
																distanceTolerance){
															currentTemplatePoint.setID(i);
															currentImagePoint.setID(i);
															matchedTemplateCorners.add(currentTemplatePoint);
															matchedImageCorners.add(currentImagePoint);
															break;
														}
													}
												}

												// if number of inliers exceeds the threshold return upperLeft, lowerLeft, upperRight and lowerRight corners, 
												// if it doesn't exceed change order of the image points and try again
												if(matchedImageCorners.size() >= inlierThreshold){
													
													// upperLeft, lowerLeft, upperRight and lowerRight corners of the template
													LinkedList<Corner> templateOuterCorners = new LinkedList<Corner>();
													// upperLeft, lowerLeft, upperRight and lowerRight corners of the image
													LinkedList<Corner> imageOuterCorners = new LinkedList<Corner>();

													// print the affine transformation
													System.out.println(affineMatrixUpper[0] + " " + affineMatrixUpper[1] + " " + affineMatrixUpper[2]);
													System.out.println(affineMatrixLower[0] + " " + affineMatrixLower[1] + " " + affineMatrixLower[2]);
													
													// get outer rectangular corners of the template in the image
													Corner imageUpperLeftCorner = null;
													Corner imageLowerLeftCorner = null;
													Corner imageUpperRightCorner = null;
													Corner imageLowerRightCorner = null;
													
													// transform outer rectangular corners of the template regarding the affine transformation
													int transformedUpperLeftX = (int)(affineMatrixUpper[0] * template.getUpperLeftCorner().getX() + 
															affineMatrixUpper[1] * template.getUpperLeftCorner().getY() + affineMatrixUpper[2]);
													int transformedUpperLeftY = (int)(affineMatrixLower[0] * template.getUpperLeftCorner().getX() +
															affineMatrixLower[1] * template.getUpperLeftCorner().getY() + affineMatrixLower[2]);
													int transformedLowerLeftX = (int)(affineMatrixUpper[0] * template.getLowerLeftCorner().getX() + 
															affineMatrixUpper[1] * template.getLowerLeftCorner().getY() + affineMatrixUpper[2]);
													int transformedLowerLeftY = (int)(affineMatrixLower[0] * template.getLowerLeftCorner().getX() +
															affineMatrixLower[1] * template.getLowerLeftCorner().getY() + affineMatrixLower[2]);
													int transformedUpperRightX = (int)(affineMatrixUpper[0] * template.getUpperRightCorner().getX() + 
															affineMatrixUpper[1] * template.getUpperRightCorner().getY() + affineMatrixUpper[2]);
													int transformedUpperRightY = (int)(affineMatrixLower[0] * template.getUpperRightCorner().getX() +
															affineMatrixLower[1] * template.getUpperRightCorner().getY() + affineMatrixLower[2]);
													int transformedLowerRightX = (int)(affineMatrixUpper[0] * template.getLowerRightCorner().getX() + 
															affineMatrixUpper[1] * template.getLowerRightCorner().getY() + affineMatrixUpper[2]);
													int transformedLowerRightY = (int)(affineMatrixLower[0] * template.getLowerRightCorner().getX() +
															affineMatrixLower[1] * template.getLowerRightCorner().getY() + affineMatrixLower[2]);
													Corner transformedTemplateUpperLeftCorner = new Corner(transformedUpperLeftX, transformedUpperLeftY, 0);
													Corner transformedTemplateLowerLeftCorner = new Corner(transformedLowerLeftX, transformedLowerLeftY, 0);
													Corner transformedTemplateUpperRightCorner = new Corner(transformedUpperRightX, transformedUpperRightY, 0);
													Corner transformedTemplateLowerRightCorner = new Corner(transformedLowerRightX, transformedLowerRightY, 0);
													
													// find corners near transformed corners in the image
													double distanceTolerance = 10;
													for(int i = 0; i < matchedImageCorners.size(); i++){
														
														Corner currentImageCorner = matchedImageCorners.get(i);
														
														if(transformedTemplateUpperLeftCorner.getDistance(currentImageCorner) < distanceTolerance){
															imageUpperLeftCorner = currentImageCorner;
														}else if(transformedTemplateLowerLeftCorner.getDistance(currentImageCorner) < distanceTolerance){
															imageLowerLeftCorner = currentImageCorner;
														}else if(transformedTemplateUpperRightCorner.getDistance(currentImageCorner) < distanceTolerance){
															imageUpperRightCorner = currentImageCorner;
														}else if(transformedTemplateLowerRightCorner.getDistance(currentImageCorner) < distanceTolerance){
															imageLowerRightCorner = currentImageCorner;
														}
													}
													
													templateOuterCorners.add(template.getUpperLeftCorner());
													templateOuterCorners.add(template.getLowerLeftCorner());
													templateOuterCorners.add(template.getUpperRightCorner());
													templateOuterCorners.add(template.getLowerRightCorner());
													
													imageOuterCorners.add(imageUpperLeftCorner);
													imageOuterCorners.add(imageLowerLeftCorner);
													imageOuterCorners.add(imageUpperRightCorner);
													imageOuterCorners.add(imageLowerRightCorner);

													return new Object[]{templateOuterCorners, imageOuterCorners};
												}else{

													// change order of the image points
													if(imagePointsPermutationCount % 2 == 0){

														Corner swappedCorners[] = swapCorners(imageCorner2, imageCorner3);
														imageCorner2 = swappedCorners[0];
														imageCorner3 = swappedCorners[1];
													}else{
														if(imagePointsPermutationCount == 1){

															Corner swappedCorners[] = swapCorners(imageCorner2, imageCorner3);
															imageCorner2 = swappedCorners[0];
															imageCorner3 = swappedCorners[1];
															swappedCorners = swapCorners(imageCorner1, imageCorner2);
															imageCorner1 = swappedCorners[0];
															imageCorner2 = swappedCorners[1];
														}else if(imagePointsPermutationCount == 3){

															Corner swappedCorners[] = swapCorners(imageCorner1, imageCorner2);
															imageCorner1 = swappedCorners[0];
															imageCorner2 = swappedCorners[1];
															swappedCorners = swapCorners(imageCorner2, imageCorner3);
															imageCorner2 = swappedCorners[0];
															imageCorner3 = swappedCorners[1];
														}
													}
													// increment the permutation counter
													imagePointsPermutationCount++;
												}
											}else{
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			cornersInAGroup--;
		}

		// return null when no affine transformation is found
		return null;
	}

	// this method finds connected corners and returns them as seperate ArrayLists
	// returned map is sorted by keys which are labels
	private Map<Integer, ArrayList<Corner>> findConnectedCorners(LinkedList<Corner> corners, int[] labeledEdgeRegions, int imageWidth,int imageHeight){

		Map<Integer, ArrayList<Corner>> connectedCorners = new HashMap<Integer, ArrayList<Corner>>();

		// find connected corner points
		for(ListIterator<Corner> cornerIterator = corners.listIterator(); cornerIterator.hasNext();){

			Corner currentCorner = cornerIterator.next();
			int currentCornerX = currentCorner.getX();
			int currentCornerY = currentCorner.getY();

			// get the label of the corner
			int labelOfTheCorner = labeledEdgeRegions[currentCornerY * imageWidth + currentCornerX];

			// if an arraylist is present for the label
			if(connectedCorners.containsKey(labelOfTheCorner)){

				// add the corner to that arraylist
				connectedCorners.get(labelOfTheCorner).add(currentCorner);
			}else{		// if an arraylist is not present for the label

				// add an arraylist for the label
				connectedCorners.put(labelOfTheCorner, new ArrayList<Corner>());

				// add the corner to that arraylist
				connectedCorners.get(labelOfTheCorner).add(currentCorner);
			}
		}
		
		return connectedCorners;
	}

	// this method is used for swapping given points
	private Corner[] swapCorners(Corner corner1, Corner corner2){
	
		return new Corner[]{corner2, corner1};
	}

	// given matrix equation must be in the form ax + by + cz = j;
	// dx + ey + fz = k; gx + hy + iz = l and matrix elements are 
	// a, b, c, j, d, e, f, k, g, h, i, l respectively
	private double[] solve3x3Equation(double[] equation){

		// solved unknowns x,y,z
		double result[] = new double[3];

		// get the coefficients and constants
		double a = equation[0], b = equation[1], c = equation[2], j = equation[3];
		double d = equation[4], e = equation[5], f = equation[6], k = equation[7];
		double g = equation[8], h = equation[9], i = equation[10], l = equation[11];

		// apply Cramer's rule		
		// find the denominator
		double denominator = (a * e * i) + (b * f * g) + (c * d * h) - (c * e * g) - (b * d * i) - (a * f * h);

		// if the denominator is not zero
		if(denominator != 0){
			// find the nominator for x
			double nominatorx = (j * e * i) + (b * f * l) + (c * k * h) - (c * e * l) - (b * k * i) - (j * f * h);
			// find the nominator for y
			double nominatory = (a * k * i) + (j * f * g) + (c * d * l) - (c * k * g) - (j * d * i) - (a * f * l);
			// find the nominator for z
			double nominatorz = (a * e * l) + (b * k * g) + (j * d * h) - (j * e * g) - (b * d * l) - (a * k * h);

			// find x, y and z
			double x = nominatorx / denominator;
			double y = nominatory / denominator;
			double z = nominatorz / denominator;

			// fill the result array
			result[0] = x; 
			result[1] = y; 
			result[2] = z;

			//return the result
			return result;
		}else{
			// if the denominator is zero, return null
			return null;
		}



	}

}
