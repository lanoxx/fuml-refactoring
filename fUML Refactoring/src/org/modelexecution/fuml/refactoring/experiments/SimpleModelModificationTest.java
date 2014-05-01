package org.modelexecution.fuml.refactoring.experiments;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    private static final String OCL_PRE_CONSTRAINT = "self.name <> '%s'";
    // FIXME Query works in OCL console but returns an exception here.
    private static final String OCL_POST_CONSTRAINT = "self.oclIsKindOf(%s)";

    /** The current resource. */
    private ResourceSet resourceSet;
    private Resource resource;
    private OCL<?, EClassifier, ?, ?, ?, ?, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private OCLHelper<EClassifier, ?, ?, Constraint> helper;

    /**
     * Initialize a {@link ResourceSet}, load an UML model file and retrieve a {@link Resource}.
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

    /**
     * Initialize {@link OCL} and {@link OCLHelper} for queries.
     */
    @Before
    public void initializeOCLInfrastructure() {
        ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        helper = ocl.createOCLHelper();
    }

    /**
     * Loads the root {@link Model} of an UML file.
     * 
     * @return the root {@link Model}.
     */
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

    /**
     * Loads all {@link Class} instances except of {@link Activity} instances from the loaded {@link Resource}.
     * 
     * @return a {@link Set} that contains all {@link Class} instances.
     */
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

    @Test
    public void testSuperclassExtrationWithPreAndPostChecks_shouldSucceed() {
        Model model = loadModel();
        String superClassName = "Vehicle";

        try {
            assertTrue(checkPreconstraints(superClassName));
        } catch (ParserException pre) {
            fail("Preconstraints failed with ParserException");
        }

        Class superClass = model.createOwnedClass(superClassName, false);
        Set<Class> contents = loadAllClasses();

        for (Class clazz : contents) {
            String className = clazz.getName();
            if (className.equals("Truck") || className.equals("Car")) {
                clazz.createGeneralization(superClass);
            }
        }

        try {
            assertTrue(checkPostconstraints(superClass));
        } catch (ParserException post) {
            post.printStackTrace();
            fail("Postconstraints failed with ParserException");
        }

        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            System.out.println(eObject.toString());
        }

    }

    /**
     * Checks the preconstraints for the superclass extraction.
     * 
     * @param name of the new class to be created.
     * @return {@code true} if the preconstraints are met.
     * @throws ParserException an exception if the preconditions are incorrectly formulated.
     */
    private boolean checkPreconstraints(String name) throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getClass_());

        OCLExpression<EClassifier> query = helper.createQuery(String.format(OCL_PRE_CONSTRAINT, name));
        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

        for (Class clazz : loadAllClasses()) {
            if (!eval.check(clazz)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the postconstraints for the superclass extraction.
     * 
     * @param name of the new class to be created.
     * @return {@code true} if the preconstraints are met.
     * @throws ParserException an exception if the preconditions are incorrectly formulated.
     */
    private boolean checkPostconstraints(Class superClass) throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getClass_());

        for (Class clazz : loadAllClasses()) {
            if (clazz.getName().equals("Truck") || clazz.getName().equals("Car")) {
                // FIXME This query can not be parsed due to a SemanticException with message
                // "Unknown enumeration literal (Vehicle)"
                helper.createQuery(String.format(OCL_POST_CONSTRAINT, superClass.getQualifiedName()));
            }
        }
        return true;
    }

}
