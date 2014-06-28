package org.modelexecution.fuml.refactoring.experiments;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.ParserException;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringDataImpl;
import org.modelexecution.fuml.refactoring.RefactoringException;
import org.modelexecution.fuml.refactoring.rename.RenameClassRefactorableImpl;
import org.modelexecution.fuml.refactoring.rename.RenameOperationRefactorableImpl;
import org.modelexecution.fuml.refactoring.rename.RenamePropertyRefactorableImpl;

public class SimpleModelModificationTest {
    private static final String MODEL_PATH = "models/insurancemodel/insurancemodel.uml";
    private static final String MODEL_SUPERCLASS_PATH = "models/insurancemodel/insurancemodel_extractClass.uml";
    private static final String MODEL_RENAME_PROPERTY_PATH =
        "models/insurancemodel/insurancemodel_renameProperty.uml";
    private static final String MODEL_RENAME_OPERATION_PATH =
        "models/insurancemodel/insurancemodel_renameOperation.uml";
    private static final String MODEL_RENAME_CLASS_PATH =
            "models/insurancemodel/insurancemodel_renameClass.uml";
    private static final String MODEL_ENCAPSULATE_PATH = "models/insurancemodel/insurancemodel_encapsulate.uml";
    private static final String MODEL_REMOVE_UNUSED_CLASS_PATH =
        "models/insurancemodel/insurancemodel_removeunusedclass.uml";

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
     * Loads an element with the given {@code name} and {@code clazz} from the model.
     * 
     * @param qualifiedName the {@code name} of the element.
     * @param clazz the {@link java.lang.Class} of the model.
     * @return the first element of the model with the given {@code name} and {@code clazz}.
     */
    private Object loadElement(String qualifiedName, java.lang.Class<? extends NamedElement> clazz) {
        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (!(eObject instanceof NamedElement)) {
                continue;
            }
            if (!clazz.isAssignableFrom(eObject.getClass())) {
                // inspected element is not from target class type
                continue;
            }
            NamedElement element = (NamedElement) eObject;
            if (qualifiedName.equals(element.getQualifiedName())) {
                return eObject;
            }
        }
        return null;
    }

    @Test
    public void testSuperclassExtrationWithPreAndPostChecks_shouldSucceed() {
        String superClassName = "Vehicle";

        RefactoringData data = new RefactoringDataImpl();
        data.set("newSuperClassName", superClassName);
        Class clazz = (Class) loadElement("Model::insurance::Car", Class.class);
        data.set("selectedElement", clazz);
        
        Refactorable extractSuperClassRefactoring = new ExtractSuperClassRefactorableImpl(data);

        try {
            assertTrue("Precondition not met!", extractSuperClassRefactoring.checkPreCondition());
        } catch (ParserException pre) {
            fail("Preconstraints failed with ParserException");
        }

        try {
            extractSuperClassRefactoring.performRefactoring();
        } catch (RefactoringException e) {
            e.printStackTrace();
        }

        try {
            assertTrue("Post condition not met!", extractSuperClassRefactoring.checkPostCondition());
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
        RefactoringData data = new RefactoringDataImpl();
        data.set("newAttributeName", "refactorednumberOfCars");
        Property property = (Property) loadElement("Model::insurance::InsurancePolicy::numberOfCars", Property.class);
        data.set("selectedElement", property);

        Refactorable rename = new RenamePropertyRefactorableImpl(data);

        String originalName = property.getQualifiedName();
        data.set("originalName", originalName);
        try {
            assertTrue("Precondition not met!", rename.checkPreCondition());
        } catch (ParserException e) {
            fail("Precondition error");
        }
        try {
            rename.performRefactoring();
        } catch (RefactoringException e) {
            fail("Refactoring error");
        }
        try {
            assertTrue("Post condition not met!", rename.checkPostCondition());
        } catch (ParserException e) {
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_RENAME_PROPERTY_PATH)), null);
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
        RefactoringData data = new RefactoringDataImpl();
        Property property = (Property) loadElement("Model::insurance::InsurancePolicy::customer", Property.class);
        data.set("selectedElement", property);

        Refactorable encapsulate = new EncapsulateFieldRefactorableImpl(data);

        try {
            assertTrue("Precondition not met!", encapsulate.checkPreCondition());
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
            // assertTrue("Post condition not met!", encapsulate.checkPostCondition());
            encapsulate.checkPostCondition();
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_ENCAPSULATE_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testRenameOperation_shouldSucceed() {
        RefactoringData data = new RefactoringDataImpl();
        Operation operation =
            (Operation) loadElement("Model::insurance::InsurancePolicy::calculatePremium", Operation.class);
        data.set("selectedElement", operation);
        data.set("newOperationName", "calculatePremiumRefactored");
        String originalName = operation.getQualifiedName();
        data.set("originalName", originalName);

        Refactorable renameOperation = new RenameOperationRefactorableImpl(data);

        try {
            assertTrue("Precondition not met!", renameOperation.checkPreCondition());
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Precondition error");
        }
        try {
            renameOperation.performRefactoring();
        } catch (RefactoringException e) {
            e.printStackTrace();
            fail("Refactoring error");
        }
        try {
            assertTrue("Post condition not met!", renameOperation.checkPostCondition());
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_RENAME_OPERATION_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testRenameClass_shouldSucceed() {
        RefactoringData data = new RefactoringDataImpl();
        Class clazz =
            (Class) loadElement("Model::insurance::InsurancePolicy", Class.class);
        data.set("selectedElement", clazz);
        data.set("newClassName", "InsurancePolicyRef");

        Refactorable renameClass = new RenameClassRefactorableImpl(data);

        try {
            assertTrue("Precondition not met!", renameClass.checkPreCondition());
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Precondition error");
        }
        try {
            renameClass.performRefactoring();
        } catch (RefactoringException e) {
            e.printStackTrace();
            fail("Refactoring error");
        }
        try {
            assertTrue("Post condition not met!", renameClass.checkPostCondition());
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_RENAME_CLASS_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testRemoveUnusedClass_shouldSucceed() {
        RefactoringData data = new RefactoringDataImpl();
        Class operation = (Class) loadElement("Model::insurance::UnusedClass", Class.class);
        data.set("selectedElement", operation);

        Refactorable removeClass = new RemoveUnusedClassRefactorableImpl(data);

        try {
            assertTrue("Precondition not met!", removeClass.checkPreCondition());
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Precondition error");
        }
        try {
            removeClass.performRefactoring();
        } catch (RefactoringException e) {
            e.printStackTrace();
            fail("Refactoring error");
        }
        try {
            assertTrue("Post condition not met!", removeClass.checkPostCondition());
        } catch (ParserException e) {
            e.printStackTrace();
            fail("Postcondition error");
        }

        try {
            resource.save(new FileOutputStream(new File(MODEL_REMOVE_UNUSED_CLASS_PATH)), null);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
