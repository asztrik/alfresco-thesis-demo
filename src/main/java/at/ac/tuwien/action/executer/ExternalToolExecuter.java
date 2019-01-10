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

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
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
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
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

public class ExternalToolExecuter extends ActionExecuterAbstractBase {

	// Configuration variables
    public static final String NAME = "external-tool";
    public static final String PARAM_TOOL_NAME = "fits";
    private static Log logger = LogFactory.getLog(ExternalToolExecuter.class);
    private String tempPath = "/home/asztrik/Documents/temp/";
    private String fitsPath = "/home/asztrik/fits/fits-1.1.1/fits.sh";
    private String fitsOutDir = "/home/asztrik/Documents/results/";
   
    private NodeService nodeService;
    private WorkflowService workflowService;
    public ContentService contentService;
    public RuntimeService runtimeService;
    public ProcessInstance processInstance;
    
    // Setter method for the content service
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    // Setter method for the runtime servcie
    public void setWorkflowService(WorkflowService workflowService)
    {
        this.workflowService = workflowService;
    }    
    

    public String parseXML(String path, NodeRef targetNodeRef) throws ParserConfigurationException, SAXException, IOException {
    	
    	File xmlfile = new File(path);
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	DocumentBuilder db = dbf.newDocumentBuilder();
    	org.w3c.dom.Document d = db.parse(xmlfile);
    	
    	d.getDocumentElement().normalize();
    	
    	// Log for debugging
    	logger.fatal("ExternalToolExecuter Info: Result read OK ");
    	
    	// get the FITS results...
    	
    	String size = d.getElementsByTagName("size").item(0).getTextContent();
    	String md5sum = d.getElementsByTagName("md5checksum").item(0).getTextContent(); 
    	
    	// Log for debugging
    	logger.fatal("ExternalToolExecuter Info: Tags fetched!");
    	
    	Node identityNode = d.getElementsByTagName("identity").item(0);
    	Element elem = (Element) identityNode;

    	// Log for debugging
    	logger.fatal("ExternalToolExecuter Info: Id node found!");
    	  	
    	String format = elem.getAttribute("format");
    	String mimetype = elem.getAttribute("mimetype");
    	
    	// Log for debugging
    	logger.fatal("ExternalToolExecuter Info: Updating properties...");
    	
    	// Update properties
    	QName PROP_QNAME_SC_DESCRIPTION = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "description");
    	nodeService.setProperty(targetNodeRef, PROP_QNAME_SC_DESCRIPTION, "to be added");
    	
    	QName PROP_QNAME_SC_FORMAT = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "format");
    	nodeService.setProperty(targetNodeRef, PROP_QNAME_SC_FORMAT, format);

    	QName PROP_QNAME_SC_TYPE = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "type");
    	nodeService.setProperty(targetNodeRef, PROP_QNAME_SC_TYPE, mimetype);
    	
    	QName PROP_QNAME_SC_DIGEST = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "digest");
    	nodeService.setProperty(targetNodeRef, PROP_QNAME_SC_DIGEST, md5sum);

    	QName PROP_QNAME_SC_SIZE = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "size");
    	nodeService.setProperty(targetNodeRef, PROP_QNAME_SC_SIZE, size);

    	//QName PROP_QNAME_SC_DATE = QName.createQName("http://pres-demo.tuwien.ac.at/model/content/1.0", "date");
    	//nodeService.setProperty(targetNodeRef, PROP_QNAME_SC_DATE, new SimpleDateFormat("yyyy/MM/dd").format(new Date()));
    	
    	// Log for debugging
    	logger.fatal("ExternalToolExecuter Info: Updating properties OK!");
    	
    	
    	return mimetype;
    	
    }
    
    
    // The actually executed code
    @Override
	protected void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
    	
	    	// Log for debugging
	    	logger.fatal("ExternalToolExecuter Info: Action called ");
    	
    	    // Getting a shell
        	Runtime rt = Runtime.getRuntime();
        
            // create a noderef
          	NodeRef targetNodeRef = actionedUponNodeRef;
          	
          	String parseResultMime = null;

          	// if the node exists
            if (this.nodeService.exists(targetNodeRef) == true) {
		        try {
		        	
		        	// First get the actual node as a content node 
		        	ContentReader contentReader = contentService.getReader(targetNodeRef, ContentModel.PROP_CONTENT);
					InputStream is = contentReader.getContentInputStream();
					
		        	// Log for debugging
		        	logger.fatal("ExternalToolExecuter Info: ContentReader created ");
					
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
		        	logger.fatal("ExternalToolExecuter Info: "+tempPath+tempFname+" created ");
		        	
		        	// Run the tool in a shell
					Process pr = rt.exec(fitsPath + " -i " + tempPath + tempFname + " -o " + fitsOutDir + tempFname + ".xml");
					
					// Log for debugging
		        	logger.fatal("ExternalToolExecuter Info: "+fitsPath + " -i " + tempPath + tempFname + " -o " + fitsOutDir + tempFname + ".xml"+" command run ");
		        	
		        	try {
						parseResultMime = this.parseXML(fitsOutDir + tempFname + ".xml", targetNodeRef);
			        	// Log for debugging
			        	logger.fatal("ExternalToolExecuter Info: XML parsed OK ");
						
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}


		        	
		        	
		        	try {
		        		
						
		        		logger.fatal("ExternalToolExecuter Info: Listing...");
		        		
		        		 List<WorkflowInstance> workflowInstances = workflowService.getActiveWorkflows();
		        		
		        		for(WorkflowInstance w : workflowInstances)	{
		        			logger.fatal("ExternalToolExecuter Info: WFInst: "+w.getId());
		        			
		        		}
		        		
			        	//TODO
			        	// you have to set pres_isImage to 1 or 0 for the BPMN!!!
						if(isImage(parseResultMime)) {
							//runtimeService.setVariable(processInstance.getId() ,"isImage", true);
							// Log for debugging
				        	logger.fatal("ExternalToolExecuter Info: Setting isImage TRUE");
						} else {
							runtimeService.setVariable(processInstance.getId() ,"isImage", false);
							// Log for debugging
				        	//logger.fatal("ExternalToolExecuter Info: Setting isImage FALSE");
							
						}
					
		        	} catch(Exception e) {
		        		
						e.printStackTrace();		        		
		        	}
		        	

		        	
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
		
	}

	private boolean isImage(String mime) {
		if(mime.equals("image/jpeg"))
			return true;
		else		
			return false;
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