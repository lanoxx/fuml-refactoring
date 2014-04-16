package org.modelexecution.fuml.refactoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.OCLInput;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.uml.OCL;
import org.eclipse.ocl.uml.UMLEnvironmentFactory;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

public class OclLoader {

	public List<Constraint> loadOcl() {
		URI uri = URI.createFileURI("models/extractSuperclass/extractSuperclass.uml");
		
		final ResourceSet set = new ResourceSetImpl();
		/* We need to register a resource factory to avoid a runtime exception */
		set.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		set.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		
		/* we use the environment factory to load the uml model */
		UMLEnvironmentFactory umlEnvFactory = new UMLEnvironmentFactory();
		umlEnvFactory.loadEnvironment(set.getResource(uri,true));
		
		/* we pass the factory instance to OCL */
		OCL ocl = OCL.newInstance(umlEnvFactory);
		//Load file stream for ocl file
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(new File("models/extractSuperclass/extractSuperclass.ocl")));
			OCLInput document = new OCLInput(reader);
			List<Constraint> constraints = ocl.parse(document);
			return constraints; }
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (ParserException e) { e.printStackTrace(); }
		return null;
	}
	
	public static void main(String[] args) {
		OclLoader loader = new OclLoader();
		loader.loadOcl();
	}
	
}
