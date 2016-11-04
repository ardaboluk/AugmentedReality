package source;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class CornerDetection {

	// imageSizeRatio parameter determines percentage of the image that will be taken into account from the center
	// e.g if the original image is 1920x1080 and imageSizeRatio is 80, a 1536x864 image whose center is 
	// the same as center of the original image will be taken into account
	public LinkedList<Corner> findCorners(BufferedImage image, double cornerThreshold){

		//width and height of the given image
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		//grayscale of the given image
		int grayscaleImage[] = convertImageToGrayscale(image); 

		//horizontal and vertical gradients grayscale image
		int[] gradientX = new int[imageWidth * imageHeight];
		int[] gradientY = new int[imageWidth * imageHeight];

		//find horizontal and vertical gradients of the grayscale image
		applySobelOperator(grayscaleImage, gradientX, gradientY, imageWidth, imageHeight);

		//harris corner response of the grayscale image
		LinkedList<Corner> harrisCornerResponse = harrisCornerDetection(gradientX, gradientY, imageWidth, imageHeight, cornerThreshold);

		//non-max suppressed harris corner response
		nonmaxSuppression(harrisCornerResponse, 8);

		return harrisCornerResponse;

	}

	//suppresses pixels other than local maxima
	private void nonmaxSuppression(LinkedList<Corner> harrisCornerResponse, int radius){

		// get a corner and look for it's neighbors in a circular region
		// remove the corner if its corner response is lesser than or equal to its neighbors
		for(Iterator<Corner> i = harrisCornerResponse.listIterator(); i.hasNext();){

			Corner currentCorner = i.next();
			if(currentCorner.isValid()){
				for(ListIterator<Corner> j = harrisCornerResponse.listIterator(); j.hasNext();){

					Corner currentNeighborCorner = j.next();

					if(currentNeighborCorner.isValid()){
						if(currentCorner.getDistance(currentNeighborCorner) <= radius){
							
							if(currentCorner.getCornerResponse() <= currentNeighborCorner.getCornerResponse()){

								if(currentCorner != currentNeighborCorner){ 
									currentCorner.invalidate();
									break;
								}
							}
						}
					}
				}
			}
		}
		
		// remove invalidated corners
		for(ListIterator<Corner> i = harrisCornerResponse.listIterator(); i.hasNext();){
			
			if(i.next().isValid() == false){
				i.remove();
			}
		}

	}

	//find corners in the image
	//if cornerThreshold is decreased corner detector will be more sensitive, i.e more corners will be found
	private LinkedList<Corner> harrisCornerDetection(int gradientX[], int gradientY[], int imageWidth, int imageHeight, double cornerThreshold){

		//corner responses which will be returned
		LinkedList<Corner> harrisCornerResponse = new LinkedList<Corner>();

		//harris corner detection
		//Ix2, Iy2, IxIy respectively
		double A = 0;
		double B = 0;
		double C = 0;
		double Mc = 0;
		double k = 0.04;

		for (int countery = 1; countery <= imageHeight - 2; countery++) {
			for (int counterx =  1; counterx <= imageWidth - 2; counterx++) {

				// reset A, B and C
				A = 0; B = 0; C = 0;

				//find corner response
				for(int windowy = -1 ; windowy <= 1; windowy++){
					for(int windowx = -1; windowx <= 1; windowx++){

						A+= gradientX[counterx + windowx + (countery + windowy) * imageWidth] * gradientX[counterx + windowx + (countery + windowy) * imageWidth];
						B+= gradientY[counterx + windowx + (countery + windowy) * imageWidth] * gradientY[counterx + windowx + (countery + windowy) * imageWidth];
						C+= gradientX[counterx + windowx + (countery + windowy) * imageWidth] * gradientY[counterx + windowx + (countery + windowy) * imageWidth];
					}
				}

				double determinant = (A * B - C * C);
				double trace = A + B;
				Mc = determinant - k * Math.pow(trace, 2);

				//if corner response is bigger than threshold, record it
				//the bigger the threshold the fewer(and more strong) corners will be found
				if(Mc > cornerThreshold){
					
					harrisCornerResponse.add(new Corner(counterx, countery, Mc));
				}
			}
		}

		return harrisCornerResponse;

	}

	// applies sobel operator to grayscale image to find vertical and horizontal gradients
	private void applySobelOperator(int[] grayscaleImage, int[] diffX, int[] diffY, int imageWidth, int imageHeight) {

		// use Sobel kernel edge detecion technique:
		// http://dasl.mem.drexel.edu/alumni/bGreen/www.pages.drexel.edu/_weg22/edge.html

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

				diffX[counterx + (countery * imageWidth)] = sumX;
				diffY[counterx + (countery * imageWidth)] = sumY;

			}
		}
	}

	// converts given image into grayscale
	// image should be an array of RGB values
	private int[] convertImageToGrayscale(BufferedImage image) {

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
