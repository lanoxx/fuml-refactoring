package org.modelexecution.fuml.refactoring

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

class ExtractSuperClass {
	/** The current resource. */
	private Resource resource
	
	def loadResourceSet() {
		def resourceSet = new ResourceSetImpl()
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE)
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE)
	}
	
	def loadModel(String path) {
		resource = resourceSet.getResource(getFileURI(path), true)
	}
	
	def loadOcl() {

	}
	
	def doRefactoring() {
		//load model
		loadModel("models/...uml")
		//check ocl pre constrains
		//checken mit ECore
		//mit Queries: http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.ocl.doc%2Fhelp%2FOCLInterpreterTutorial.html&cp=45_3_1
		//...
		//perform refactoring
		//...
		//check ocl post constrains
		//...
		//save refactored model
		//...
		
	}
}
