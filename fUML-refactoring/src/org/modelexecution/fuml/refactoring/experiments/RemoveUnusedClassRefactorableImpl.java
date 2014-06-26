package org.modelexecution.fuml.refactoring.experiments;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.expressions.ExpressionsFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.expressions.Variable;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class RemoveUnusedClassRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT =
        "(uml::Property.allInstances() - classToRemove.attribute).type.conformsTo(classToRemove)->forAll(atom | atom = false)";
    private static final String OCL_POST_CONSTRAINT = "";
    private final RefactoringData data;

    public RemoveUnusedClassRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("selectedElement") != null);
    }

    /**
     * Checks the preconstraints for remove unused class.
     * 
     * @return {@code true} if the preconstraints are met.
     * @throws ParserException an exception if the preconditions are incorrectly formulated.
     */
    @Override
    public boolean checkPreCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getClass_());
        Class selectedElement = (Class) data.get("selectedElement");

        Variable<EClassifier, EParameter> variable = ExpressionsFactory.eINSTANCE.createVariable();
        variable.setName("classToRemove");
        variable.setType(UMLPackage.Literals.CLASSIFIER);
        ocl.getEnvironment().addElement(variable.getName(), variable, true);

        OCLExpression<EClassifier> query;
        query = helper.createQuery(OCL_PRE_CONSTRAINT);

        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);
        eval.getEvaluationEnvironment().add("classToRemove", selectedElement);

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
        Class selectedElement = (Class) data.get("selectedElement");

        EcoreUtil.delete(selectedElement, true);

        return false;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        return true;
    }
}
