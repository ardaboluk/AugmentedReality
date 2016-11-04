package source;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPicker;
import com.github.sarxos.webcam.WebcamResolution;
import com.sun.j3d.loaders.objectfile.ObjectFile;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.universe.PlatformGeometry;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

public class LiveCam implements Runnable{

	private boolean noTriangulate = false;
	private boolean noStripify = false;
	private double creaseAngle = 360.0;
	private SimpleUniverse simpleUniverse;
	private BranchGroup objRoot = null;
	private TransformGroup objTransform = null;
	private BoundingSphere bounds;
	private Canvas3D canvas3d = null;
	private OffScreenCanvas3D offScreenCanvas3d = null;
	private OrbitBehavior orbit = null;
	private Background background = null;
	private Switch modelsSwitch = null;
	
	private static final int OFF_SCREEN_SCALE = 3;

	private JPanel mainPanel = null;

	private TemplateRecognition templateRecognition = null;
	private CornerDetection cornerDetection = null;
	private Object matchingOuterCorners[] = null;

	private Webcam webcam = null;
	private WebcamPicker picker = null;

	private boolean isRunning = true;
	private boolean recognitionStarted = false;
	private boolean templateRecognized = false;
	private boolean renderImage = false;

	private Corner templateUpperLeftCorner = null;
	private Corner templateLowerLeftCorner = null;
	private Corner templateUpperRightCorner = null;
	private Corner templateLowerRightCorner = null;

	private Corner imageUpperLeftCorner = null;
	private Corner imageLowerLeftCorner = null;
	private Corner imageUpperRightCorner = null;
	private Corner imageLowerRightCorner = null;

	private String templateName = "";

	public LiveCam(JPanel mainPanel){

		this.mainPanel = mainPanel;
		init();

		templateRecognition = new TemplateRecognition();
		cornerDetection = new CornerDetection();

		picker = new WebcamPicker();
		webcam = picker.getSelectedWebcam();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open(false);
	}

	public void runIt(){
		isRunning = true;
	}

	public void stopIt(){
		isRunning = false;
	}

	public void startRecognition(){

		recognitionStarted = true;
	}

	public void takeSnaphot(){

		renderImage = true;
	}

	@SuppressWarnings("unchecked")
	public void run(){

		while(isRunning == true){

			try{

				// get image from the camera
				BufferedImage capturedImage = webcam.getImage();

				if(recognitionStarted == true && templateRecognized == false){

					matchingOuterCorners = templateRecognition.recognizeTemplateInImage(capturedImage);					

					if(matchingOuterCorners != null){

						templateRecognized = true;

						LinkedList<Corner> templateOuterCorners = (LinkedList<Corner>)matchingOuterCorners[0];
						LinkedList<Corner> imageOuterCorners = (LinkedList<Corner>)matchingOuterCorners[1];

						templateUpperLeftCorner = templateOuterCorners.get(0);
						templateLowerLeftCorner = templateOuterCorners.get(1);
						templateUpperRightCorner = templateOuterCorners.get(2);
						templateLowerRightCorner = templateOuterCorners.get(3);

						imageUpperLeftCorner = imageOuterCorners.get(0);
						imageLowerLeftCorner = imageOuterCorners.get(1);
						imageUpperRightCorner = imageOuterCorners.get(2);
						imageLowerRightCorner = imageOuterCorners.get(3);	

						templateName = (String)matchingOuterCorners[2];

						switch3DModel(templateName);

					}else{

						recognitionStarted = false;
					}
				}else if(recognitionStarted == true && templateRecognized == true){

					if(imageUpperLeftCorner != null && imageLowerLeftCorner != null && imageUpperRightCorner != null && imageLowerRightCorner != null){

						// detect corners of the current frame
						LinkedList<Corner> currentFrameCorners = cornerDetection.findCorners(capturedImage, 1000000000);

						// track outer image corners from previous frame
						double distanceTolerance = 10;
						boolean upperLeftMatched = false;
						boolean lowerLeftMatched = false;
						boolean upperRightMatched = false;
						boolean lowerRightMatched = false;
						for(Iterator<Corner> i = currentFrameCorners.iterator(); i.hasNext();){

							Corner currentCorner = i.next();
							if(currentCorner.getDistance(imageUpperLeftCorner) < distanceTolerance){

								imageUpperLeftCorner = currentCorner;
								upperLeftMatched = true;
							}else if(currentCorner.getDistance(imageLowerLeftCorner) < distanceTolerance){

								imageLowerLeftCorner = currentCorner;
								lowerLeftMatched = true;
							}else if(currentCorner.getDistance(imageUpperRightCorner) < distanceTolerance){

								imageUpperRightCorner = currentCorner;
								upperRightMatched = true;
							}else if(currentCorner.getDistance(imageLowerRightCorner) < distanceTolerance){

								imageLowerRightCorner = currentCorner;
								lowerRightMatched = true;
							}

						}
						
						// if at least one of the points is lost, remove the 3D object and make recognitionStarted false
						// to start the template searching process
						if(!(upperLeftMatched && lowerLeftMatched && upperRightMatched && lowerRightMatched)){
							recognitionStarted = false;
							templateRecognized = false;
							modelsSwitch.setWhichChild(-1);
						}


						// mark outer image corners (for debugging purposes)
						Graphics capturedImageGraphics = capturedImage.getGraphics();
						capturedImageGraphics.setColor(Color.red);
						for(Iterator<Corner> i = currentFrameCorners.iterator(); i.hasNext();){

							Corner currentCorner = i.next();
							if(currentCorner == imageUpperLeftCorner){

								capturedImageGraphics.drawString("upperLeft", currentCorner.getX(), currentCorner.getY());
							}else if(currentCorner == imageLowerLeftCorner){

								capturedImageGraphics.drawString("lowerLeft", currentCorner.getX(), currentCorner.getY());
							}else if(currentCorner == imageUpperRightCorner){

								capturedImageGraphics.drawString("upperRight", currentCorner.getX(), currentCorner.getY());
							}else if(currentCorner == imageLowerRightCorner){

								capturedImageGraphics.drawString("lowerRight", currentCorner.getX(), currentCorner.getY());
							}
						}							

						// compute homography and get pose of the marker in 3d
						double points1[][] = new double[][]{
								{templateUpperLeftCorner.getX(), templateLowerLeftCorner.getX(), templateUpperRightCorner.getX(), templateLowerRightCorner.getX()},
								{templateUpperLeftCorner.getY(), templateLowerLeftCorner.getY(), templateUpperRightCorner.getY(), templateLowerRightCorner.getY()}
						};
						double points2[][] = new double[][]{
								{imageUpperLeftCorner.getX(), imageLowerLeftCorner.getX(), imageUpperRightCorner.getX(), imageLowerRightCorner.getX()},
								{imageUpperLeftCorner.getY(), imageLowerLeftCorner.getY(), imageUpperRightCorner.getY(), imageLowerRightCorner.getY()}
						};
						double obj3dMapping[][] = solveHomography(points1, points2);

						// get position (we should divide by 100 because in java3D 1 corresponds to 1 meter, so we should make this 1 cm )
						double tx = obj3dMapping[0][3] * 0.01, ty = obj3dMapping[1][3] * 0.01, tz = obj3dMapping[2][3] * 0.01;

						// get rotation (euler angles from rotation matrix)
						double rx = Math.atan2(obj3dMapping[2][1], obj3dMapping[2][2]);
						double ry = Math.atan2(-obj3dMapping[2][0], Math.sqrt(Math.pow(obj3dMapping[2][1], 2) + Math.pow(obj3dMapping[2][2], 2)));
						double rz = Math.atan2(obj3dMapping[1][0], obj3dMapping[0][0]);

						System.out.println("Translation: " + tx + " " + ty + " " + tz);
						System.out.println("Rotation: " + rx + " " + ry + " " + rz);

						// transform the 3d model regarding rotation and translation
						Transform3D objTrans = new Transform3D();
						objTrans.setTranslation(new Vector3d(tx, ty, tz));
						Transform3D objRotX = new Transform3D();
						objRotX.rotX(rx);
						Transform3D objRotY = new Transform3D();
						objRotY.rotY(ry);
						Transform3D objRotZ = new Transform3D();
						objRotZ.rotZ(rz);
						Transform3D objScale = new Transform3D();
						objScale.setScale(3);

						objTrans.mul(objRotZ);
						objTrans.mul(objRotY);
						objTrans.mul(objRotX);
						objTrans.mul(objScale);

						objTransform.setTransform(objTrans);

						if(renderImage == true){

							offScreenCanvas3d.doRender(640, 480);							

							renderImage = false;
						}



					}else{

						recognitionStarted = false;
						templateRecognized = false;
					}					
				}

				// set the image as background
				background.setImage(new ImageComponent2D(ImageComponent2D.FORMAT_RGB, capturedImage));

			}catch(Exception e){

				System.err.println(e);
				recognitionStarted = false;
				templateRecognized = false;
			}
		}		
	}

	// this method computes homography matrix from given point correspondences and
	// returns Rotation and Translation vectors
	// homography transformation maps points on a plane to points on another plane in 3D space
	private double[][] solveHomography(double[][] points1, double[][] points2){

		// pose of the object in camera coordinate system
		double obj3dMapping[][] = new double[4][4];

		// get point coordinates
		// the center of the world coordinate system is the untransformed template's upper left corner
		// to center the 3D object on the marker, we should make the center of the world coordinate system 
		// correspond to the center of the untransformed marker
		double xa1 = points1[0][0] - 274, ya1 = points1[1][0] - 189, xa2 = points1[0][1] - 274, ya2 = points1[1][1] - 189;
		double xa3 = points1[0][2] - 274, ya3 = points1[1][2] - 189, xa4 = points1[0][3] - 274, ya4 = points1[1][3] - 189;
		double xb1 = points2[0][0], yb1 = points2[1][0], xb2 = points2[0][1], yb2 = points2[1][1];
		double xb3 = points2[0][2], yb3 = points2[1][2], xb4 = points2[0][3], yb4 = points2[1][3];

		// prepare Ax=b linear algebra system from point correspondences
		// prepare matrix A
		DenseMatrix64F A = new DenseMatrix64F(new double[][]{
				{xa1, ya1, 1, 0, 0, 0, -xa1*xb1, -ya1*xb1},
				{0, 0, 0, xa1, ya1, 1, -xa1*yb1, -ya1*yb1},
				{xa2, ya2, 1, 0, 0, 0, -xa2*xb2, -ya2*xb2},
				{0, 0, 0, xa2, ya2, 1, -xa2*yb2, -ya2*yb2},
				{xa3, ya3, 1, 0, 0, 0, -xa3*xb3, -ya3*xb3},
				{0, 0, 0, xa3, ya3, 1, -xa3*yb3, -ya3*yb3},
				{xa4, ya4, 1, 0, 0, 0, -xa4*xb4, -ya4*xb4},
				{0, 0, 0, xa4, ya4, 1, -xa4*yb4, -ya4*yb4}
		});
		// prepare matrix x
		DenseMatrix64F x = new DenseMatrix64F(8,1);
		// prepare matrix b
		DenseMatrix64F b = new DenseMatrix64F(new double[][]{
				{xb1},
				{yb1},
				{xb2},
				{yb2},
				{xb3},
				{yb3},
				{xb4},
				{yb4}
		});

		// solve for the homography matrix
		if(!CommonOps.solve(A, b, x)){

			System.err.println("Singular matrix");
			System.exit(1);
		}

		// make the homography matrix 3x3
		DenseMatrix64F homography = new DenseMatrix64F(new double[][]{
				{x.get(0,0),x.get(1,0),x.get(2,0)},
				{x.get(3,0),x.get(4,0),x.get(5,0)},
				{x.get(6,0),x.get(7,0),1.0},
		});

		// inverse of pre-calculated camera matrix
		DenseMatrix64F inverseK = new DenseMatrix64F(new double[][]{
				// from matlab (transposed intrinsic matrix taken from matlab camera calibration tool
				// (matlab gives the intrinsic matrix in a transposed format)
				{0.0015, 0, -0.4628},
				{0, 0.0015, -0.4002},
				{0, 0, 1}

		});

		// H = K[R|T] get rid of K to find rotation and translation	
		DenseMatrix64F orientation = new DenseMatrix64F(3,3);
		CommonOps.mult(inverseK,homography,orientation);

		// R
		double rx1 = orientation.get(0,0), rx2 = orientation.get(1,0), rx3 = orientation.get(2,0);
		// normalize rx
		double rxLength = Math.sqrt(rx1 * rx1 + rx2 * rx2 + rx3 * rx3);
		rx1 = rx1 / rxLength; rx2 = rx2 / rxLength; rx3 = rx3 / rxLength;
		
		double ry1 = orientation.get(0,1), ry2 = orientation.get(1,1), ry3 = orientation.get(2,1);
		// normalize ry
		double ryLength = Math.sqrt(ry1 * ry1 + ry2 * ry2 + ry3 * ry3);
		ry1 = ry1 / ryLength; ry2 = ry2 / ryLength; ry3 = ry3 / ryLength;
		
		double rz1 = rx2 * ry3 - rx3 * ry2, rz2 = rx3 * ry1 - rx1 * ry3, rz3 = rx1 * ry2 - rx2 * ry1;
		// normalize rz0
		double rzLength = Math.sqrt(rz1 * rz1 + rz2 * rz2 + rz3 * rz3);
		rz1 = rz1 / rzLength; rz2 = rz2 / rzLength; rz3 = rz3 / rzLength;

		// T
		double tx = orientation.get(0,2), ty = orientation.get(1,2), tz = orientation.get(2,2);
		// normalize T
		double tLength = (rxLength + ryLength) / 2;
		tx = tx / tLength; ty = ty / tLength; tz = tz / tLength;

		obj3dMapping[0][0] = rx1; obj3dMapping[1][0] = rx2; obj3dMapping[2][0] = rx3; obj3dMapping[3][0] = 0;
		obj3dMapping[0][1] = ry1; obj3dMapping[1][1] = ry2; obj3dMapping[2][1] = ry3; obj3dMapping[3][1] = 0;
		obj3dMapping[0][2] = rz1; obj3dMapping[1][2] = rz2; obj3dMapping[2][2] = rz3; obj3dMapping[3][2] = 0;
		obj3dMapping[0][3] = tx; obj3dMapping[1][3] = ty; obj3dMapping[2][3] = tz; obj3dMapping[3][3] = 1;

		return obj3dMapping;

	}

	private void switch3DModel(String templateName){

		modelsSwitch.setWhichChild(Integer.parseInt(templateName));
	}

	private BranchGroup createSceneGraph(){

		objRoot = new BranchGroup();
		objRoot.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		objRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

		// Create a Transformgroup to scale all objects so they
		// appear in the scene
		objTransform = new TransformGroup();
		objTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objRoot.addChild(objTransform);

		modelsSwitch = new Switch();
		objTransform.addChild(load3DModels());

		bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);

		// Get view platform
		ViewingPlatform viewingPlatform = simpleUniverse.getViewingPlatform();

		Transform3D camTransform = new Transform3D();
		camTransform.lookAt(new Point3d(0,0,-1), new Point3d(0, 0, 0), new Vector3d(0, -1, 0));
		viewingPlatform.getViewPlatformTransform().setTransform(camTransform);

		simpleUniverse.getViewer().getView().setBackClipDistance(2000);

		PlatformGeometry platformGeometry = new PlatformGeometry();

		// Set up the ambient light 
		Color3f ambientColor = new Color3f(0.1f, 0.1f, 0.1f);
		AmbientLight ambientLightNode = new AmbientLight(ambientColor);
		ambientLightNode.setInfluencingBounds(bounds);
		platformGeometry.addChild(ambientLightNode);

		// Set up the directional lights
		Color3f light1Color = new Color3f(1.0f, 1.0f, 0.9f);
		Vector3f light1Direction = new Vector3f(1.0f, 1.0f, 1.0f);
		Color3f light2Color = new Color3f(1.0f, 1.0f, 1.0f);
		Vector3f light2Direction = new Vector3f(-1.0f, -1.0f, -1.0f);

		DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
		light1.setInfluencingBounds(bounds);
		platformGeometry.addChild(light1);

		DirectionalLight light2 = new DirectionalLight(light2Color, light2Direction);
		light2.setInfluencingBounds(bounds);
		platformGeometry.addChild(light2);

		viewingPlatform.setPlatformGeometry(platformGeometry);

		return objRoot;

	}

	private BranchGroup load3DModels(){

		BranchGroup branchGroup = new BranchGroup();
		branchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		branchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		modelsSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		modelsSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		File modelsFolder = null;
		File[] modelFiles = null;
		ObjectFile objFile = null;

		try{
			modelsFolder = new File("./models");
			modelFiles = modelsFolder.listFiles();
			Arrays.sort(modelFiles);

			int flags = ObjectFile.RESIZE;
			if(!noTriangulate){
				flags |= ObjectFile.TRIANGULATE;
			}
			if(!noStripify){
				flags |= ObjectFile.STRIPIFY;
			}
			objFile = new ObjectFile(flags, (float)(creaseAngle * Math.PI / 180.0));

			for(int i = 0; i < modelFiles.length; i++){
				if(modelFiles[i].getName().split("\\.")[1].equals("obj")){				
					modelsSwitch.addChild(objFile.load(modelFiles[i].getAbsolutePath()).getSceneGroup());
				}

			}

			modelsSwitch.setWhichChild(-1);
			branchGroup.addChild(modelsSwitch);

		}catch(Exception e){
			System.out.println(e.getMessage());
		}		

		return branchGroup;

	}

	private void init(){

		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

		canvas3d = new Canvas3D(config);
		mainPanel.add(canvas3d, BorderLayout.CENTER);
		simpleUniverse = new SimpleUniverse(canvas3d);

		// Create the off-screen Canvas3D object
		offScreenCanvas3d = new OffScreenCanvas3D(config, true);
		// Set the off-screen size based on a scale factor times the
		// on-screen size
		Screen3D sOn = canvas3d.getScreen3D();
		Screen3D sOff = offScreenCanvas3d.getScreen3D();
		Dimension dim = sOn.getSize();
		dim.width *= OFF_SCREEN_SCALE;
		dim.height *= OFF_SCREEN_SCALE;
		sOff.setSize(dim);
		sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth()
				* OFF_SCREEN_SCALE);
		sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight()
				* OFF_SCREEN_SCALE);

		// attach the offscreen canvas to the view
		simpleUniverse.getViewer().getView().addCanvas3D(offScreenCanvas3d);

		BranchGroup scene = createSceneGraph();

		background = new Background();
		background.setApplicationBounds(bounds);
		background.setCapability(Background.ALLOW_IMAGE_WRITE);
		objRoot.addChild(background);

		orbit = new OrbitBehavior(canvas3d, OrbitBehavior.REVERSE_ALL);
		orbit.setSchedulingBounds(bounds);
		simpleUniverse.getViewingPlatform().setViewPlatformBehavior(orbit);

		// Create a simple scene and attach it to the virtual universe
		simpleUniverse.addBranchGraph(scene);

	}

}

class OffScreenCanvas3D extends Canvas3D {
	
	private static final long serialVersionUID = 1L;

	OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
			boolean offScreen) {

		super(graphicsConfiguration, offScreen);
	}

	void doRender(int width, int height) throws IOException {

		BufferedImage bImage = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);

		ImageComponent2D buffer = new ImageComponent2D(
				ImageComponent.FORMAT_RGBA, bImage);

		setOffScreenBuffer(buffer);
		renderOffScreenBuffer();
		waitForOffScreenRendering();
		bImage = getOffScreenBuffer().getImage();

		File outputFile = new File("AR.png");
		ImageIO.write(bImage, "png", outputFile);
	}

	public void postSwap() {
		// No-op since we always wait for off-screen rendering to complete
	}
}
