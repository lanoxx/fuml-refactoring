package org.modelexecution.fuml.refactoring;

import java.io.File;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

public class RefactoringController {

    /** The current resource. */
    private ResourceSet resourceSet;
    private Resource resource;

    /**
     * Load a UML Model file and initialize a resource set.
     * 
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

    private OCL<?, EClassifier, ?, ?, ?, ?, ?, ?, ?, Constraint, EClass, EObject> createAndInitializeOCL(
        Resource resource) {

        /* we pass the factory instance to OCL and create a new instance */
        return OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
    }

    /**
     * Get a refactoring strategy from the RefactoringFactory
     */
    public void performRefactoring(String refactoring, String params, List<String> params2)
        throws RefactoringException {
        Refactorable refactoringStrategy = RefactoringFactory.getInstance(refactoring);
        OCL ocl = createAndInitializeOCL(resource);
        if (!refactoringStrategy.checkPreCondition(ocl, resource, params)) {
            throw new RefactoringException("Precondition not met.");
        }
        refactoringStrategy.performRefactoring(resource, params, params2);
        if (!refactoringStrategy.checkPostCondition(ocl)) {
            throw new RefactoringException("Postcondition not met.");
        }

        // TODO: safe refactored model
    }
}
