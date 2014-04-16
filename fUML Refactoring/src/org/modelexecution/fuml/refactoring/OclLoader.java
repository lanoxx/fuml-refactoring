package org.modelexecution.fuml.refactoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.ocl.OCLInput;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.ecore.CallOperationAction;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.ecore.SendSignalAction;
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
		InputStream stream;
		try {
			stream = new FileInputStream("models/extractSuperclass/extractSuperclass_uml.ocl");
			OCLInput document = new OCLInput(stream);
			List<Constraint> constraints = ocl.parse(document);
			return constraints; }
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (ParserException e) { e.printStackTrace(); }
		return null;
	}
	
	public List<org.eclipse.ocl.ecore.Constraint> loadEcoreOcl() {
		URI uri = URI.createFileURI("models/extractSuperclass/extractSuperclass.uml");
		final ResourceSet set = new ResourceSetImpl();
		
		/* We need to register a resource factory to avoid a runtime exception */
		set.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		set.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		EcoreEnvironmentFactory ecoreEnvFactory = new EcoreEnvironmentFactory();
		ecoreEnvFactory.loadEnvironment(set.getResource(uri, true));
		org.eclipse.ocl.OCL<EPackage,EClassifier,EOperation,EStructuralFeature,EEnumLiteral,EParameter,
		                    EObject,CallOperationAction,SendSignalAction,org.eclipse.ocl.ecore.Constraint,EClass,EObject>
			ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
		
		//Load file stream for ocl file
		InputStream stream;
		try {
			stream = new FileInputStream("models/extractSuperclass/extractSuperclass_eCore.ocl");
			OCLInput document = new OCLInput(stream);
			List<org.eclipse.ocl.ecore.Constraint> constraints = ocl.parse(document);
			return constraints; }
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (ParserException e) { e.printStackTrace(); }
		return null;
	}
	
	public static void main(String[] args) {
		OclLoader loader = new OclLoader();
		// Variante 1: OCL mit UMLEnvironmentFactory:
		loader.loadOcl();
		// Variante 2: OCL mit EcoreEnvironmentFactory:
		//loader.loadEcoreOcl();
	}
	
}
