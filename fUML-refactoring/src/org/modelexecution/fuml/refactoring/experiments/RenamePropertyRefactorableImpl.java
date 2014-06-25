package org.modelexecution.fuml.refactoring.experiments;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class RenamePropertyRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT =
        "self.class.attribute->union(self.class.allParents().attribute)->forAll(a | a.name <> '%s')";

    private static final String OCL_POST_CONSTRAINT = "self.class.namespace.member->selectByType(Class).member"
        + "->selectByType(Activity).node->selectByKind(StructuralFeatureAction).structuralFeature"
        + "->forAll(n | n.qualifiedName <> '%s')"; // %s is the old name.
    private final RefactoringData data;

    public RenamePropertyRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("newAttributeName") != null);
        assert (data.get("selectedElement") != null);
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
        Property selectedElement = (Property) data.get("selectedElement");
        String newAttributeName = (String) data.get("newAttributeName");

        selectedElement.setName(newAttributeName);

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getProperty());

        Property selectedElement = (Property) data.get("selectedElement");

        OCLExpression<EClassifier> query;
        query = helper.createQuery(String.format(OCL_POST_CONSTRAINT, data.get("originalName")));

        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

        if (!eval.check(selectedElement)) {
            return false;
        }
        return true;
    }
}
