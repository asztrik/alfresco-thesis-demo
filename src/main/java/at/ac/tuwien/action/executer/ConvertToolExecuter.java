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
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
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

public class ConvertToolExecuter extends ActionExecuterAbstractBase {

	// Configuration variables
    public static final String NAME = "convert-tool";
    public static final String PARAM_TOOL_NAME = "fits";
    private static Log logger = LogFactory.getLog(ConvertToolExecuter.class);
    private String tempPath = "/home/asztrik/Documents/temp/";
    private String convOutDir = "/home/asztrik/Documents/results/";
   
    private NodeService nodeService;
    public ContentService contentService;
    
    // Setter method for the content service
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
	

   
    
    // The actually executed code
    @Override
	protected void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
        
	    	// Log for debugging
	    	logger.fatal("ConvertToolExecuter Info: Action called ");
    	
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
		        	logger.fatal("ConvertToolExecuter Info: ContentReader created ");
					
					// Write the node to the host system
					byte[] outWr = new byte[is.available()];
					is.read(outWr);
					
					// Get the node's filename and prepare a temp file
					String tempFname = (String) nodeService.getProperty(targetNodeRef, ContentModel.PROP_NAME);

		        	// Log for debugging
		        	logger.fatal("ConvertToolExecuter Info: Node name acquired ");
					
					// Write out
					File tempTargetFile = new File(tempPath+tempFname);
					FileOutputStream os = new FileOutputStream(tempTargetFile);
		        	os.write(outWr);
					
		        	// Log for debugging
		        	logger.fatal("ConvertToolExecuter Info: Node name acquired ");
		        	
		        	// Run the tool in a shell
					Process pr = rt.exec("convert " + tempPath + tempFname + " " + convOutDir + tempFname + ".tiff");

		        	// Log for debugging
		        	logger.fatal("ConvertToolExecuter Info:" + "convert " + tempPath + tempFname + " " + convOutDir + tempFname + ".tiff" + " run.");
					
					//TODO
					// create NEW object and new properties
					// Disable Integrity Check!!
		        	
		        	
		        	Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
		        	
					//set derivedfrom object as the original
		        	QName PROP_QNAME_SC_DERIVEDFROM = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "derivedFrom");		        	
		        	props.put(PROP_QNAME_SC_DERIVEDFROM, targetNodeRef);
		        	
		        	// Log for debugging
		        	logger.fatal("ConvertToolExecuter Info:  Property created. ");	

		        	
					NodeRef newNodeRef = nodeService.createNode(
							targetNodeRef, 
							ContentModel.ASSOC_CONTAINS, 
							QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,  tempFname), 
							ContentModel.TYPE_CONTENT, props).getChildRef();
					
		        	// Log for debugging
		        	logger.fatal("ConvertToolExecuter Info:  New Node created ");					
					
					ContentWriter writer = contentService.getWriter(newNodeRef, ContentModel.PROP_CONTENT, true);
					//writer.setLocale(CONTENT_LOCALE);
					File file = new File(convOutDir + tempFname + ".tiff");
					writer.setMimetype("image/tiff");
					writer.putContent(file);
					
		        	// Log for debugging
		        	logger.fatal("ConvertToolExecuter Info:  New Node content written ");						

			    	
	        	
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
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