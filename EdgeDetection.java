package source;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EdgeDetection {

	@SuppressWarnings("unchecked")
	public int[] detectEdges(BufferedImage image){

		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		//get the grayscale of given image
		int[] grayscale = imageToGrayscale(image);

		//apply gaussian blur to the grayscale image
		int[] gaussianBlurredImage = gaussianBlur(grayscale, imageWidth, imageHeight, 1);

		//apply sobel operator to find edges
		int[] diffX = new int[imageWidth * imageHeight];
		int[] diffY = new int[imageWidth * imageHeight];
		int[] gradientMagnitude = sobelOperator(gaussianBlurredImage, diffX, diffY, imageWidth, imageHeight);

		//mark weak edges
		doubleThresholding(gradientMagnitude, imageWidth, imageHeight, 83, 168);

		//find connected edges
		Object[] connectedComponents = connectedComponentLabeling(gradientMagnitude, imageWidth, imageHeight);

		//eliminate connected edges which have no strong edge point
		int[] labeledEdgeRegions = (int[])connectedComponents[1];
		ArrayList<Integer> labels = (ArrayList<Integer>)connectedComponents[0];
		eliminateWeakEdges(gradientMagnitude, labeledEdgeRegions, labels, imageWidth, imageHeight);	

		//return connected strong edges
		return labeledEdgeRegions;

	}

	// this method eliminates weak edges from labeledEdgeRegions according to gradientMagnitude
	private void eliminateWeakEdges(int[] gradientMagnitude, int[] labeledEdgeRegions, ArrayList<Integer> labels, int imageWidth, int imageHeight){

		// this arrays holds if the corresponding regions are strong
		boolean strongEdgeRegions[] = new boolean[labels.size()];

		//find strong edge regions
		for(int countery = 0; countery <= imageHeight - 1; countery++){
			for(int counterx = 0; counterx <= imageWidth - 1; counterx++){

				if(gradientMagnitude[countery * imageWidth + counterx] > 0){

					int labelIndex = labels.indexOf(labeledEdgeRegions[countery * imageWidth + counterx]);
					if(labelIndex > -1){
						strongEdgeRegions[labelIndex] = true;
					}
				}
			}
		}

		//suppress weak edge regions
		for(int countery = 0; countery <= imageHeight - 1; countery++){
			for(int counterx = 0; counterx <= imageWidth - 1; counterx++){

				int currentLabel = labeledEdgeRegions[countery * imageWidth + counterx];
				if(currentLabel != 0){

					int labelIndex = labels.indexOf(currentLabel);
					if(labelIndex > -1){
						if(strongEdgeRegions[labelIndex] == false){

							labeledEdgeRegions[countery * imageWidth + counterx] = 0;
						}
					}

				}				
			}
		}

	}

	//connected component labeling using 8-connectivity
	//this method labels connected edge regions in gradientMagnitude and puts the result in a new array
	private Object[] connectedComponentLabeling(int[] gradientMagnitude, int imageWidth, int imageHeight){

		// all labels in the image
		Map<Integer, Label> labels = new HashMap<Integer, Label>();

		// labeled image
		int labeledEdgeRegions[] = new int[gradientMagnitude.length];

		// label counter
		int labelCounter = 1;			

		// first pass
		for(int countery = 1; countery < imageHeight - 1; countery++){
			for(int counterx = 1; counterx < imageWidth - 1; counterx++){

				int currentGradient = gradientMagnitude[countery * imageWidth + counterx];
				if(currentGradient != 0){

					int northEastToWest[] = new int[4];
					northEastToWest[0] = labeledEdgeRegions[(countery - 1) * imageWidth + (counterx + 1)];
					northEastToWest[1] = labeledEdgeRegions[(countery - 1) * imageWidth + (counterx)];
					northEastToWest[2] = labeledEdgeRegions[(countery - 1) * imageWidth + (counterx - 1)];
					northEastToWest[3] = labeledEdgeRegions[(countery) * imageWidth + (counterx - 1)];

					// if non of the neighbors are labeled
					if(northEastToWest[0] + northEastToWest[1] + northEastToWest[2] + northEastToWest[3] == 0){

						// uniquely label the current element, create a new label object, add the label object to the map and update the label counter
						labeledEdgeRegions[countery * imageWidth + counterx] = labelCounter;
						Label newLabel = new Label(labelCounter);
						labels.put(labelCounter, newLabel);
						labelCounter++;
					}else{

						// find the neighbor with the smallest label
						int smallestLabel = 100000;
						for(int i = 0; i < northEastToWest.length; i++){

							if(northEastToWest[i] != 0 && northEastToWest[i] < smallestLabel){

								smallestLabel = northEastToWest[i];
							}
						}

						// assign smallest label to the current pixel
						labeledEdgeRegions[countery * imageWidth + counterx] = smallestLabel;

						// store the equivalence between neighboring labels
						Label smallestLabelObject = labels.get(smallestLabel);
						for(int i = 0; i < northEastToWest.length; i++){							

							if(northEastToWest[i] != 0 && northEastToWest[i] != smallestLabel){

								Label currentNeighborLabelObject = labels.get(northEastToWest[i]);
								smallestLabelObject.union(currentNeighborLabelObject);
							}
						}						
					}
				}				
			}
		}

		// second pass
		for(int countery = 1; countery < imageHeight - 1; countery++){
			for(int counterx = 1; counterx < imageWidth - 1; counterx++){

				if(labeledEdgeRegions[countery * imageWidth + counterx] != 0){

					// set root label of the label of the current pixel as the label of the current pixel
					labeledEdgeRegions[countery * imageWidth + counterx] = labels.get(labeledEdgeRegions[countery * imageWidth + counterx]).getRoot().getLabelName();
				}
			}
		}

		// return the labels and the labeled image
		ArrayList<Integer> labelsArrayList = new ArrayList<Integer>();
		labelsArrayList.addAll(labels.keySet());
		return new Object[]{labelsArrayList, labeledEdgeRegions};
	}


	//this method makes values less than thresholdLow 0,makes values between thresholdLow and thresholdHigh negative (marks them) and doesn't change
	//values greater than thresholdHigh
	private void doubleThresholding(int[] gradientMagnitude, int imageWidth, int imageHeight, int thresholdLow, int thresholdHigh){

		for(int countery = 0; countery <= imageHeight - 1; countery++){
			for(int counterx = 0; counterx <= imageWidth - 1; counterx++){

				if(gradientMagnitude[countery * imageWidth + counterx] <= thresholdLow){

					gradientMagnitude[countery * imageWidth + counterx] = 0;
				}else if(gradientMagnitude[countery * imageWidth + counterx] > thresholdLow && gradientMagnitude[countery * imageWidth + counterx] <= thresholdHigh){
					gradientMagnitude[countery * imageWidth + counterx] *= -1;
				}
			}
		}
	}

	// applies sobel operator to grayscale image to find vertical and horizontal gradients
	private int[] sobelOperator(int[] grayscaleImage, int[] diffX, int[] diffY, int imageWidth, int imageHeight) {

		// use Sobel kernel edge detecion technique:
		// http://dasl.mem.drexel.edu/alumni/bGreen/www.pages.drexel.edu/_weg22/edge.html

		int[] gradientMagnitude = new int[imageWidth * imageHeight];

		// 3x3 GX Sobel mask
		int GX[][] = new int[3][3];
		// 3x3 GY Sobel mask
		int GY[][] = new int[3][3];

		GX[0][0] = -1; GX[0][1] = 0; GX[0][2] = 1;
		GX[1][0] = -2; GX[1][1] = 0; GX[1][2] = 2;
		GX[2][0] = -1; GX[2][1] = 0; GX[2][2] = 1;

		GY[0][0] = 1; GY[0][1] = 2; GY[0][2] = 1;
		GY[1][0] = 0; GY[1][1] = 0; GY[1][2] = 0;
		GY[2][0] = -1; GY[2][1] = -2; GY[2][2] = -1;

		for (int countery = 1; countery <= imageHeight - 2; countery++) {
			for (int counterx =  1; counterx <= imageWidth - 2; counterx++) {

				int sumX = 0;
				int sumY = 0;

				// X gradient approximation
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						sumX += grayscaleImage[counterx + i + (countery + j) * imageWidth] * GX[i + 1][j + 1];
					}
				}

				// Y gradient approximation
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						sumY += grayscaleImage[counterx + i + (countery + j) * imageWidth] * GY[i + 1][j + 1];
					}
				}

				gradientMagnitude[counterx + (countery * imageWidth)] = Math.abs(sumX) + Math.abs(sumY);

				diffX[counterx + (countery * imageWidth)] = sumX;
				diffY[counterx + (countery * imageWidth)] = sumY;

			}
		}

		return gradientMagnitude;
	}

	// applies gaussian filter to given image
	private int[] gaussianBlur(int[] grayscale, int imageWidth, int imageHeight, double sigma){
		//starting and ending points of the part of the image that will be taken into account and center of the image
		int startingX = 0, startingY = 0, endingX = 0, endingY = 0;

		int kernelWidth = (int)sigma * 6;

		startingX = kernelWidth / 2;
		startingY = kernelWidth / 2;
		endingX = (imageWidth - 1) - kernelWidth / 2;
		endingY = (imageHeight - 1) - kernelWidth / 2;

		//result image that will be returned
		int[] resultImage = grayscale.clone();
		//partially convolved image
		int partiallyConvolved[] = new int[imageWidth * imageHeight];

		//calculate gaussian kernel for given sigma and window size
		double gaussianKernel[] = new double[kernelWidth];
		double sumKernel = 0;
		for(int i = - (kernelWidth / 2); i < kernelWidth / 2; i++){
			gaussianKernel[i + kernelWidth / 2] = (1/Math.sqrt(2*Math.PI*sigma*sigma)) * Math.pow(Math.E, - (i*i) / (2*sigma*sigma));
			sumKernel += gaussianKernel[i + kernelWidth / 2];
		}
		for(int i = 0; i < kernelWidth; i++){
			gaussianKernel[i] = gaussianKernel[i] * (1/sumKernel);
		}

		//convolve the given image with gaussian kernel
		//make use of separability

		//convolve vertically
		for(int countery = startingY; countery <= endingY; countery++){
			for(int counterx = startingX; counterx <= endingX; counterx++){

				double sum = 0;
				for(int i = -(kernelWidth / 2); i < (kernelWidth / 2); i++){
					sum += gaussianKernel[i + kernelWidth / 2] * grayscale[(countery + i) * imageWidth + counterx];
				}
				partiallyConvolved[countery * imageWidth + counterx] = (int)sum;
			}
		}

		//convolve horizontally
		for(int countery = startingY; countery < endingY; countery++){
			for(int counterx = startingX; counterx < endingX; counterx++){

				double sum = 0;
				for(int i = -(kernelWidth / 2); i < (kernelWidth / 2); i++){
					sum += gaussianKernel[i + kernelWidth / 2] * partiallyConvolved[countery * imageWidth + (counterx + i)];
				}
				resultImage[countery * imageWidth + counterx] = (int)sum;
			}
		}		

		return resultImage;
	}

	// converts given image into grayscale
	// image should be an array of RGB values
	private int[] imageToGrayscale(BufferedImage image) {

		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		int greyscale[] = new int[imageHeight * imageWidth];

		// Use luminosity method. Reference:
		// http://www.johndcook.com/blog/2009/08/24/algorithms-convert-color-grayscale/

		for (int countery = 0; countery < imageHeight; countery++) {
			for (int counterx = 0; counterx < imageWidth; counterx++) {

				// get current pixel
				Color currentPixel = new Color(image.getRGB(counterx, countery));
				int currentPixelRed = currentPixel.getRed();
				int currentPixelGreen = currentPixel.getGreen();
				int currentPixelBlue = currentPixel.getBlue();

				greyscale[imageWidth * countery + counterx] = (int) (0.21 * currentPixelRed + 0.71 * currentPixelGreen + 0.07 * currentPixelBlue);
			}
		}

		return greyscale;
	}

}
