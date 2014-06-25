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
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class RenameOperationRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT = "self.class.ownedOperation->union(self.class.allParents()"
        + "->selectByType(Class).ownedOperation)->union(self.class.allParents()"
        + "->selectByType(Interface).ownedOperation)->forAll(a | a.name <> '%s')";

    private static final String OCL_POST_CONSTRAINT = ""
        + "self.class.namespace.member->selectByType(Class).member->selectByType(Activity).node"
        + "->selectByType(CallOperationAction)->isEmpty() or "
        + "self.class.namespace.member->selectByType(Class).member"
        + "->selectByType(Activity).node->selectByType(CallOperationAction).operation"
        + "->forAll(n | n.qualifiedName <> '%s')";
    private final RefactoringData data;

    public RenameOperationRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("newOperationName") != null);
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
        helper.setContext(UMLPackage.eINSTANCE.getOperation());

        Operation selectedElement = (Operation) data.get("selectedElement");
        String newOperationName = (String) data.get("newOperationName");

        OCLExpression<EClassifier> query;
        String queryString = String.format(OCL_PRE_CONSTRAINT, newOperationName);
        query = helper.createQuery(queryString);

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
        Operation selectedElement = (Operation) data.get("selectedElement");
        String newOperationName = (String) data.get("newOperationName");

        selectedElement.setName(newOperationName);

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getOperation());

        Operation selectedElement = (Operation) data.get("selectedElement");

        OCLExpression<EClassifier> query;
        String queryString = String.format(OCL_POST_CONSTRAINT, data.get("originalName"));
        query = helper.createQuery(queryString);

        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

        if (!eval.check(selectedElement)) {
            return false;
        }
        return true;
    }
}
