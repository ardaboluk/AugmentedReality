package source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class TemplateLoader {
	
	public void saveTemplate(Template template){
		
		try{
			
			if(template != null){
				
				// get the name of the template (for filename)
				String templateName = template.getTemplateName();
				
				// serialize the template
				FileOutputStream fileOut = new FileOutputStream("./templates/" + templateName + ".tmp");
				ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
				objectOut.writeObject(template);
				objectOut.close();
				fileOut.close();
				
			}
		}catch(Exception e){
			
			System.err.println(e);
			System.exit(1);
		}		
		
	}
	
	public ArrayList<Template> loadTemplates(){
		
		// list of the templates
		ArrayList<Template> templates = null;
		
		try{
			
			templates = new ArrayList<Template>();
			
			// deserialize files in the folder
			File templatesFolder = new File("./templates");
			File templateFiles[] = templatesFolder.listFiles();
			
			FileInputStream fileIn = null;
			ObjectInputStream objectIn = null;
			
			for(int i = 0; i < templateFiles.length; i++){
				
				fileIn = new FileInputStream(templateFiles[i]);
				objectIn = new ObjectInputStream(fileIn);
				Template currentTemplate = (Template)objectIn.readObject();
				
				if(currentTemplate != null){
					templates.add(currentTemplate);
				}
			}
			
			if(fileIn != null){
				fileIn.close();
			}
			
			if(objectIn != null){
				objectIn.close();
			}
			
			
		}catch(Exception e){
		
			System.err.println(e);
			System.exit(1);
		}		
		
		return templates;
	}

}
