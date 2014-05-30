package org.modelexecution.fuml.refactoring.experiments;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.ParserException;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringDataImpl;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class SimpleModelModificationTest {
    private static final String MODEL_PATH = "models/extractSuperclass/extractSuperclass.uml";
    private static final String MODEL_SUPERCLASS_PATH = "models/extractSuperclass/extractSuperclass_ref.uml";
    private static final String MODEL_RENAME_PATH = "models/extractSuperclass/extractSuperclass_renameAttr_ref.uml";

    /** The current resource. */
    private ResourceSet resourceSet;
    private Resource resource;

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

    private Class getCar(Model model) {
        EList<Element> elements = model.getOwnedElements();
        Iterator<Element> it = elements.iterator();
        while (it.hasNext()) {
            Element element = it.next();
            if (element instanceof Class && ((Class) element).getName().equals("Car")) {
                return (Class) element;
            }
        }
        return null;
    }

    private Property getProperty(Model model, String className, String propertyName) {
        EList<Element> elements = model.getOwnedElements();
        for (Element element : elements) {
            if (element instanceof Class && ((Class) element).getName().equals(className)) {
                Class car = (Class) element;
                for (Property p : car.getAllAttributes()) {
                    if (p.getName().equals(propertyName)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    @Test
    public void testSuperclassExtrationWithPreAndPostChecks_shouldSucceed() {
        Model model = loadModel();
        String superClassName = "Vehicle";

        RefactoringData data = new RefactoringDataImpl();
        data.set("newSuperClassName", superClassName);
        data.set("selectedElement", getCar(model));
        ExtractSuperClassRefactorableImpl extractSuperClassRefactoring = new ExtractSuperClassRefactorableImpl(data);

        try {
            assertTrue(extractSuperClassRefactoring.checkPreCondition());
        } catch (ParserException pre) {
            fail("Preconstraints failed with ParserException");
        }

        try {
            extractSuperClassRefactoring.performRefactoring();
        } catch (RefactoringException e) {
            e.printStackTrace();
        }

        try {
            assertTrue(extractSuperClassRefactoring.checkPostCondition());
        } catch (ParserException post) {
            post.printStackTrace();
            fail("Postconstraints failed with ParserException");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_SUPERCLASS_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof Class) {
                System.out.println(eObject.toString());
            }
        }
    }

    @Test
    public void testRenameProperty_shouldSucceed() {
        Model model = loadModel();

        RefactoringData data = new RefactoringDataImpl();
        data.set("newAttributeName", "FastCar");
        data.set("selectedElement", getProperty(model, "Car", "registration"));

        RenamePropertyRefactorableImpl rename = new RenamePropertyRefactorableImpl(data);

        try {
            rename.checkPreCondition();
        } catch (ParserException e) {
            fail("Precondition error");
        }
        try {
            rename.performRefactoring();
        } catch (RefactoringException e) {
            fail("Refactoring error");
        }
        try {
            rename.checkPostCondition();
        } catch (ParserException e) {
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_RENAME_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testEncapsulateField_shouldSucceed() {
        Model model = loadModel();

        RefactoringData data = new RefactoringDataImpl();
        Property property = getProperty(model, "InsurancePolicy", "policyNumber");
        data.set("selectedElement", property);

        EncapsulateFieldRefactorableImpl encapsulate = new EncapsulateFieldRefactorableImpl(data);

        try {
            encapsulate.checkPreCondition();
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Precondition error");
        }
        try {
            encapsulate.performRefactoring();
        } catch (RefactoringException e) {
            e.printStackTrace();
            fail("Refactoring error");
        }
        try {
            encapsulate.checkPostCondition();
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_RENAME_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
