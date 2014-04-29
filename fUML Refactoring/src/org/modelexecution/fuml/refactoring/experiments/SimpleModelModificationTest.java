package org.modelexecution.fuml.refactoring.experiments;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;

public class SimpleModelModificationTest {
    private static final String MODEL_PATH = "models/extractSuperclass/extractSuperclass.uml";

    private static final String OCL_CONSTRAINT = "name.at(1)=name.at(1).toUpperCase()";

    /** The current resource. */
    private ResourceSet resourceSet;
    private Resource resource;

    /**
     * Load a UML Model file and initialize a resource set.
     * 
     * @param umlModelFile
     */
    @Before
    public void initializeResourceSet() {
        resourceSet = new ResourceSetImpl();
        resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        File file = new File(MODEL_PATH);
        URI uri = URI.createFileURI(file.getAbsolutePath());
        resource = resourceSet.getResource(uri, true);
    }

    private Model loadModel() {
        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof Model) {
                return (Model) eObject;
            }
        }
        return null;
    }

    @Test
    public void test() {
        Model model = loadModel();
        String superClassName = "Vehicle";
        Class superClass = model.createOwnedClass(superClassName, false);

        System.out.println(checkPreconstraints(model, superClassName));

        Set<Class> contents = loadAllClasses();

        for (Class clazz : contents) {
            String className = clazz.getName();
            if (className.equals("Truck") || className.equals("Car")) {
                clazz.createGeneralization(superClass);
            }
        }

        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            System.out.println(eObject.toString());
        }

    }

    private boolean checkPreconstraints(Model model, String name) {
        OCL<?, EClassifier, ?, ?, ?, ?, ?, ?, ?, Constraint, EClass, EObject> ocl;
        ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        OCLHelper<EClassifier, ?, ?, Constraint> helper = ocl.createOCLHelper();
        helper.setContext(UMLPackage.eINSTANCE.getClass_());
        OCLExpression<EClassifier> query = null;

        try {
            query = helper.createQuery("self.name <> '" + name + "'");
            Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

            for (Class clazz : loadAllClasses()) {
                if (!eval.check(clazz)) {
                    return false;
                }
            }
        } catch (ParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    private Set<Class> loadAllClasses() {
        Set<Class> contents = new HashSet<>();
        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof Class && !(eObject instanceof Activity)) {
                contents.add((Class) eObject);
            }
        }
        return contents;
    }

}
