package org.modelexecution.fuml.refactoring.experiments;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class RenamePropertyRefactorableImpl implements Refactorable {

    private OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT =
        "self.class.allParents().attribute->union(self.class.attribute)->forAll ( a | a . name <> '%s')";
    private static final String OCL_POST_CONSTRAINT = "self.general->includes(newSuperClass)";
    private RefactoringData data;

    public RenamePropertyRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("newAttributeName") != null);
        assert (data.get("selectedElement") != null);
    }

    /**
     * Loads all {@link Class} instances except of {@link Activity} instances from the loaded {@link Resource}.
     * 
     * @return a {@link Set} that contains all {@link Class} instances.
     */
    private Set<Class> loadAllClasses(Class selectedElement) {

        EList<NamedElement> elements = selectedElement.getPackage().getMembers();
        selectedElement.getOwner();

        Set<Class> contents = new HashSet<>();
        Iterator<NamedElement> iterator = elements.iterator();
        while (iterator.hasNext()) {
            NamedElement element = iterator.next();
            if (element instanceof Class && !(element instanceof Activity)) {
                contents.add((Class) element);
            }
        }
        return contents;
    }

    /**
     * Checks the preconstraints for the superclass extraction.
     * 
     * @param name of the new class to be created.
     * @return {@code true} if the preconstraints are met.
     * @throws ParserException an exception if the preconditions are incorrectly formulated.
     */
    @Override
    public boolean checkPreCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getProperty());

        Property selectedElement = (Property) data.get("selectedElement");
        String newAttributeName = (String) data.get("newAttributeName");

        OCLExpression<EClassifier> query;
        query = helper.createQuery(String.format(OCL_PRE_CONSTRAINT, newAttributeName));

        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

        if (!eval.check(selectedElement)) {
            return false;
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
    @Override
    public boolean performRefactoring() throws RefactoringException {
        // Model model = loadModel(resource);
        Property selectedElement = (Property) data.get("selectedElement");
        String newAttributeName = (String) data.get("newAttributeName");

        selectedElement.setName(newAttributeName);

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getProperty());

        return true;
    }
}
