/***********************************************/
/**										      **/
/** ExternalToolExecuter.java                 **/
/**						- Asztrik Bakos 2017  **/
/**										      **/
/**	  - Custom Action for running arbitrary   **/
/**		preservation tools in Alfresco	      **/
/**										      **/
/***********************************************/

package at.ac.tuwien.action.executer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sun.xml.txw2.Document;

import at.ac.tuwien.platformsample.HelloWorldWebScript;

public class MigrationToolExecuter extends ActionExecuterAbstractBase {

	// Configuration variables
    public static final String NAME = "migration-tool";
    public static final String PARAM_TOOL_NAME = "fits";
    private static Log logger = LogFactory.getLog(MigrationToolExecuter.class);
    private String tempPath = "/home/asztrik/Documents/temp/";
    private String migrOutDir = "/home/asztrik/Documents/results/";
   
    private NodeService nodeService;
    public ContentService contentService;
    
    // Setter method for the content servce
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
	

   
    
    // The actually executed code
    @Override
	protected void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
        
    	// Log for debugging
    	logger.fatal("MigrationToolExecuter Info: Action called ");
	
	    // Getting a shell
    	Runtime rt = Runtime.getRuntime();
    
        // create a noderef
      	NodeRef targetNodeRef = actionedUponNodeRef;
      	

      	// if the node exists
        if (this.nodeService.exists(targetNodeRef) == true) {
	        try {
	        	
	        	// First get the actual node as a content node 
	        	ContentReader contentReader = contentService.getReader(targetNodeRef, ContentModel.PROP_CONTENT);
				InputStream is = contentReader.getContentInputStream();
				
	        	// Log for debugging
	        	logger.fatal("MigrationToolExecuter Info: ContentReader created ");
				
				// Write the node to the host system
				byte[] outWr = new byte[is.available()];
				is.read(outWr);
				
				// Get the node's filename and prepare a temp file
				String tempFname = (String) nodeService.getProperty(targetNodeRef, ContentModel.PROP_NAME);

	        	// Log for debugging
	        	logger.fatal("ExternalToolExecuter Info: Node name acquired ");
				
				// Write out
				File tempTargetFile = new File(tempPath+tempFname);
				FileOutputStream os = new FileOutputStream(tempTargetFile);
	        	os.write(outWr);
				
	        	// Log for debugging
	        	logger.fatal("MigrationToolExecuter Info: "+tempPath+tempFname+" created ");
	        	
	        	// Run the mv in a shell to "migrate the file"
				Process pr = rt.exec(" mv " + tempPath + tempFname + " " + migrOutDir + tempFname);
				
				// Log for debugging
	        	logger.fatal("MigrationToolExecuter Info: "+" mv " + tempPath + tempFname + " " + migrOutDir + tempFname +" command run ");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

  
		
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
//		  paramList.add(
//		        	new ParameterDefinitionImpl(PARAM_TOOL_NAME,
//		        								DataTypeDefinition.NODE_REF,
//		        								true,
//		getParamDisplayLabel(PARAM_TOOL_NAME)));
		
	}
	
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
}
	

}