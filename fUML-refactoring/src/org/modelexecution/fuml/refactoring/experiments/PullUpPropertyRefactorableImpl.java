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
import org.eclipse.ocl.expressions.ExpressionsFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.expressions.Variable;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class PullUpPropertyRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    // Context Property
    private static final String OCL_PRE_CONSTRAINT = "self.class.superClass->select(s | s.name = selectedSuperClass.name)->notEmpty()"
    		+ " and self.visibility <> uml::VisibilityKind::private"
    		+ " and self.class.inheritedMember->selectByType(Property)"
    		+ "->forAll(prop | prop.isDistinguishableFrom(self, self.class.namespace))";
    // "self.class.superClass->select(s | s.name = superClass.name)->notEmpty()";
    // + " and self.visibility <> uml::VisibilityKind::private"
    // + " and self.class.inheritedMember->selectByType(Property)"
    // + "->forAll(prop | prop.isDistinguishableFrom(self, self.class.namespace))";

    private static final String OCL_POST_CONSTRAINT = "";

    private final RefactoringData data;

    public PullUpPropertyRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("selectedElement") != null);
        assert (data.get("superClass") != null);
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
        Class superClass = (Class) data.get("superClass");

        Variable<EClassifier, EParameter> variable = ExpressionsFactory.eINSTANCE.createVariable();
        variable.setName("selectedSuperClass");
        variable.setType(UMLPackage.Literals.CLASSIFIER);
        ocl.getEnvironment().addElement(variable.getName(), variable, true);

        OCLExpression<EClassifier> expression = helper.createQuery(OCL_PRE_CONSTRAINT);

        Query<EClassifier, EClass, EObject> query = ocl.createQuery(expression);
        ocl.getEvaluationEnvironment().add("selectedSuperClass", superClass);

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
        Property selectedElement = (Property) data.get("selectedElement");
        Class superClass = (Class) data.get("superClass");

        superClass.getOwnedAttributes().add(selectedElement);

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        return true;
    }
}
