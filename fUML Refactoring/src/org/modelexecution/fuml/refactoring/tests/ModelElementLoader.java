package org.modelexecution.fuml.refactoring.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;

public class ModelElementLoader {
    private static final String MODEL_PATH = "models/insurancemodel/insurancemodel.uml";

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
     * @param name the {@code name} of the element.
     * @param clazz the {@link java.lang.Class} of the model.
     * @return the first element of the model with the given {@code name} and {@code clazz}.
     */
    private Object loadElement(String name, java.lang.Class<? extends NamedElement> clazz) {
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
            if (!name.equals(element.getName())) {
                // element name doesn't match so discard all elements below
                iterator.prune();
                continue;
            } else {
                // element is from target class type and name matches
                return eObject;
            }
        }
        return null;
    }

    /**
     * Loads an element with the given {@code name} and {@code clazz} from the model that has corresponding owner with
     * {@code ownerName} and type {@code ownerClazz}.
     * 
     * @param name the {@code name} of the element.
     * @param clazz the {@link java.lang.Class} of the model.
     * @param ownerName the name of the owner.
     * @param ownerClazz the {@link java.lang.Class} of the owner.
     * @return the first element of the model with the given {@code name} and {@link java.lang.Class} that has
     *         corresponding owner with {@code ownerName} and {@link java.lang.Class} of the owner.
     */
    private Object loadOwnedElement(String name, java.lang.Class<? extends NamedElement> clazz, String ownerName,
        java.lang.Class<? extends NamedElement> ownerClazz) {
        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject ownerEObject = iterator.next();
            if (!(ownerEObject instanceof NamedElement)) {
                continue;
            }
            if (!ownerClazz.isAssignableFrom(ownerEObject.getClass())) {
                // inspected owning element is not from target class type of the owner
                continue;
            }
            NamedElement ownerElement = (NamedElement) ownerEObject;
            if (!ownerName.equals(ownerElement.getName())) {
                // owning element name doesn't match so discard all elements below
                iterator.prune();
                continue;
            }
            // owning element is from target class type and name matches so search deeper
            while (iterator.hasNext()) {
                EObject eObject = iterator.next();
                if (!clazz.isAssignableFrom(eObject.getClass())) {
                    // inspected element is not from target class type of the owner
                    continue;
                }
                NamedElement element = (NamedElement) eObject;
                if (!name.equals(element.getName())) {
                    // element name doesn't match so discard all elements below
                    iterator.prune();
                    continue;
                }
                // finally owner matches and element matches
                return eObject;
            }
        }
        return null;
    }

    @Test
    public void testGetClassOfModel_shouldSucceed() {
        String name = "Car";
        Object object = loadElement(name, Class.class);
        assertNotNull(object);
        boolean isClass = (object instanceof Class);
        assertTrue(isClass);
        Class clazz = (Class) object;
        assertTrue(name.equals(clazz.getName()));
    }

    @Test
    public void testGetPropertyOfModel_shouldSucceed() {
        String ownerName = "Car";
        String name = "registration";
        Object object = loadOwnedElement(name, Property.class, ownerName, Class.class);
        assertNotNull(object);
        boolean isProperty = (object instanceof Property);
        assertTrue(isProperty);
        Property property = (Property) object;
        assertTrue(name.equals(property.getName()));
    }
}
