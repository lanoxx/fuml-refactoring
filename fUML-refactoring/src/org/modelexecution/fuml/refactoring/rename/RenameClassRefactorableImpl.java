package org.modelexecution.fuml.refactoring.rename;

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
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class RenameClassRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    // context Class
    private static final String OCL_PRE_CONSTRAINT = "self.namespace.member->selectByType(Class)->forAll(c | c.name <> '%s')";

    // no post constraint needed at all
    private static final String OCL_POST_CONSTRAINT = "";
    private final RefactoringData data;

    public RenameClassRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("newClassName") != null);
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
        helper.setContext(UMLPackage.eINSTANCE.getClass_());

        Class selectedElement = (Class) data.get("selectedElement");
        String newClassName = (String) data.get("newClassName");

        OCLExpression<EClassifier> expression;
        expression = helper.createQuery(String.format(OCL_PRE_CONSTRAINT, newClassName));

        Query<EClassifier, EClass, EObject> query = ocl.createQuery(expression);

        if (!query.check(selectedElement)) {
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
        Class selectedElement = (Class) data.get("selectedElement");
        String newClassName = (String) data.get("newClassName");

        selectedElement.setName(newClassName);

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        return true;
    }
}
