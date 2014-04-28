package org.modelexecution.fuml.refactoring;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.uml.OCL;
import org.eclipse.ocl.uml.UMLEnvironmentFactory;
import org.eclipse.ocl.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.modelexecution.fuml.refactoring.refactorings.RefactoringFactory;

public class RefactoringController {
	
	/** The current resource. */
	private ResourceSet resourceSet;
	private Resource resource;
	
	/**
	 * Load a UML Model file and initialize a resource set.
	 * @param umlModelFile
	 */
	public void initializeResourceSet(String umlModelFile) {
		resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		File file = new File(umlModelFile);
		URI uri = URI.createFileURI(file.getAbsolutePath());
		resource = resourceSet.getResource(uri, true);
	}
	
	public OCL createAndInitializeOCL(Resource resource) {
	        /* we use the environment factory to load the uml model */
	        UMLEnvironmentFactory umlEnvFactory = new UMLEnvironmentFactory();
	        umlEnvFactory.loadEnvironment(resource);

	        /* we initialize the OCL with the resource set */
	        OCL.initialize(resourceSet);
	        /* we pass the factory instance to OCL and create a new instance */
	        return OCL.newInstance(umlEnvFactory);
	}
	
	/**
	 * Get all classes from the resource set (which are not Activity objects).
	 */
	public Set<Class> getAllClasses(Resource modelResource) {
		Set<Class> allClasses = new HashSet<>();
		TreeIterator<EObject> iterator = modelResource.getAllContents();
		while (iterator.hasNext()) {
			EObject eObject = iterator.next();
			if(eObject instanceof Class && !(eObject instanceof Activity)) {
				allClasses.add((Class) eObject);
			}
		}
		return allClasses;
	}
	
	/**
	 * Get a refactoring strategy from the RefactoringFactory
	 */
	public void performRefactoring(String refactoring, String params)throws RefactoringException {
		Refactorable refactoringStrategy = RefactoringFactory.getInstance(refactoring);
		OCL ocl = createAndInitializeOCL(resource);
		if(!refactoringStrategy.checkPreCondition(ocl)) {
			throw new RefactoringException("Precondition not met.");
		}
		Set<Class> allClasses = getAllClasses(resource);
		refactoringStrategy.performRefactoring(allClasses);
		if(!refactoringStrategy.checkPostCondition(ocl)) {
			throw new RefactoringException("Postcondition not met.");
		}
		
		//TODO: safe refactored model
	}
}

