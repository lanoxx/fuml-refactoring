package org.modelexecution.fuml.refactoring.uml;

import java.io.File;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.uml.OCL;
import org.eclipse.ocl.uml.OCL.Helper;
import org.eclipse.ocl.uml.OCLExpression;
import org.eclipse.ocl.uml.UMLEnvironmentFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;

public class UmlLoader {
    private static final String MODEL_PATH = "models/extractSuperclass/extractSuperclass.uml";

    private static final String OCL_CONSTRAINT = "name.at(1)=name.at(1).toUpperCase()";

    private ResourceSet resourceSet;

    @Before
    public void initializeResoureSet() {
        resourceSet = new ResourceSetImpl();
        /* We need to register a resource factory to avoid a runtime exception */
        resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
    }

    @Test
    public void test() {
        loadFile();
    }

    private void loadFile() {
        Resource modelResource = loadModel(MODEL_PATH);
        OCL ocl = createAndInitializeOCL(modelResource);
        OCLExpression query = null;
        Helper helper = ocl.createOCLHelper();
        TreeIterator<EObject> iterator = modelResource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            System.out.println(eObject.getClass());
        }
    }

    private Resource loadModel(String modelPath) {
        File modelFile = new File(MODEL_PATH);
        Resource resource = resourceSet.getResource(URI.createFileURI(modelFile.getAbsolutePath()), true);
        return resource;
    }

    private OCL createAndInitializeOCL(Resource resource) {
        /* we use the environment factory to load the uml model */
        UMLEnvironmentFactory umlEnvFactory = new UMLEnvironmentFactory();
        umlEnvFactory.loadEnvironment(resource);

        /* we initialize the OCL with the resource set */
        OCL.initialize(resourceSet);
        /* we pass the factory instance to OCL and create a new instance */
        return OCL.newInstance(umlEnvFactory);
    }

}
