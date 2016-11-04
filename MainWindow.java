package source;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainWindow extends JFrame{

	private static final long serialVersionUID = 1L;

	public MainWindow(){	

		// filters
		final CornerDetection cornerDetection = new CornerDetection();
		final EdgeDetection edgeDetection = new EdgeDetection();
		final TemplateLoader templateLoader = new TemplateLoader();

		// set title and size of main window
		setTitle("AugmentedReality");		
		setSize(670, 640);
		//setDefaultCloseOperation(EXIT_ON_CLOSE);

		// BorderLayout as main layout
		BorderLayout mainLayout = new BorderLayout();
		// GridLayout for buttons
		GridLayout buttonsLayout = new GridLayout(4, 1);

		// JPanel as main panel
		JPanel mainPanel = new JPanel(mainLayout);
		add(mainPanel);

		final LiveCam liveCam = new LiveCam(mainPanel);
		new Thread(liveCam).start();

		// JPanel for displaying buttons
		JPanel buttonsPanel = new JPanel(buttonsLayout);
		mainPanel.add(buttonsPanel, BorderLayout.PAGE_END);

		// button for saving a new template
		final JButton buttonLoadTemplate = new JButton("Load New Template");
		buttonLoadTemplate.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){

				try{
					JFileChooser fileChooser = new JFileChooser();
					int fileChooserStatus = fileChooser.showOpenDialog(null);

					if(fileChooserStatus == JFileChooser.APPROVE_OPTION){

						// save the template
						File selectedFile = fileChooser.getSelectedFile();
						BufferedImage templateImage = ImageIO.read(selectedFile);
						// find corners of the template
						LinkedList<Corner> templateCorners = cornerDetection.findCorners(templateImage, 1000000000);
						// find labeled edge regions of the template
						int templateLabeledEdgeRegions[] = edgeDetection.detectEdges(templateImage);
						// get the file name of the template
						String templateName = selectedFile.getName().split("\\.")[0];
						// find upperLeft, lowerLeft, upperRight and lowerRight corners of the template
						Corner upperLeftCorner = new Corner(100000, 100000, 100);
						Corner lowerLeftCorner = new Corner(100000, 0, 100);
						Corner upperRightCorner = new Corner(0, 100000, 100);
						Corner lowerRightCorner = new Corner(0, 0, 100);
						for(int i = 0; i < templateCorners.size(); i++){
							Corner currentCorner = templateCorners.get(i);
							int currentCornerX = currentCorner.getX();
							int currentCornerY = currentCorner.getY();						
							double currentCornerResponse = currentCorner.getCornerResponse();
							if(currentCornerX <= upperLeftCorner.getX() && currentCornerY <= upperLeftCorner.getY()){
								upperLeftCorner = new Corner(currentCornerX, currentCornerY, currentCornerResponse);
							}
							if(currentCornerX <= lowerLeftCorner.getX() && currentCornerY >= lowerLeftCorner.getY()){
								lowerLeftCorner = new Corner(currentCornerX, currentCornerY, currentCornerResponse);
							}
							if(currentCornerX >= upperRightCorner.getX() && currentCornerY <= upperRightCorner.getY()){
								upperRightCorner = new Corner(currentCornerX, currentCornerY, currentCornerResponse);
							}
							if(currentCornerX >= lowerRightCorner.getX() && currentCornerY >= lowerRightCorner.getY()){
								lowerRightCorner = new Corner(currentCornerX, currentCornerY, currentCornerResponse);
							}
						}

						// save the template
						Template template = new Template(templateName, templateCorners, templateLabeledEdgeRegions, templateImage.getWidth(), templateImage.getHeight(),
								upperLeftCorner, lowerLeftCorner, upperRightCorner, lowerRightCorner);
						
						templateLoader.saveTemplate(template);
					}
				}catch(Exception ex){
					System.out.println(ex.getMessage());
				}


			}
		});
		buttonsPanel.add(buttonLoadTemplate);

		// button for starting AR
		final JButton buttonMatchFeatures = new JButton("Augment Reality");
		buttonMatchFeatures.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ 

				liveCam.runIt();
				liveCam.startRecognition();
			}
		});
		buttonsPanel.add(buttonMatchFeatures);
		
		// button for snapshot
		final JButton buttonSnaphot = new JButton("Snaphot");
		buttonSnaphot.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				liveCam.takeSnaphot();
			}
		});
		buttonsPanel.add(buttonSnaphot);

		// button for closing the program
		final JButton buttonClose = new JButton("Close");
		buttonClose.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){ 

				liveCam.stopIt();
				System.exit(0);
			}
		});
		buttonsPanel.add(buttonClose);

	}


}
