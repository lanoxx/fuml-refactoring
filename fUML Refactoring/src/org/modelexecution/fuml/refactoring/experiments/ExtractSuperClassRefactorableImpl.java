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
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class ExtractSuperClassRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT =
        "self.namespace.member->selectByType(Class).name->forAll(o | o <> '%s')";

    private static final String OCL_POST_CONSTRAINT = "newSuperClass.visibility = uml::VisibilityKind::public"
        + " and self.general->includes(newSuperClass)";

    private final RefactoringData data;

    public ExtractSuperClassRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("newSuperClassName") != null);
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
        String newSuperClassName = (String) data.get("newSuperClassName");

        OCLExpression<EClassifier> query;
        query = helper.createQuery(String.format(OCL_PRE_CONSTRAINT, newSuperClassName));

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
        Class selectedElement = (Class) data.get("selectedElement");
        String newSuperClassName = (String) data.get("newSuperClassName");
        Package pkg = selectedElement.getPackage();

        Class superClass = UMLFactory.eINSTANCE.createClass();
        superClass.setName(newSuperClassName);

        if (pkg.getPackagedElement(newSuperClassName) == null) {
            // the owning package does not own a class with the inserted name
            // create new class named 'className'
            Class newSuperclass = UMLFactory.eINSTANCE.createClass();
            newSuperclass.setName(newSuperClassName);
            pkg.getPackagedElements().add(newSuperclass);
            // create generalization from context class to new class
            Generalization gen = UMLFactory.eINSTANCE.createGeneralization();
            selectedElement.getGeneralizations().add(gen);
            gen.setGeneral(newSuperclass);
            data.set("newSuperClass", newSuperclass);
        } else { // the owning package owns a class with the inserted name
            Class existingClass = (Class) pkg.getPackagedElement(newSuperClassName);
            // create generalization from context class to existing class
            Generalization gen = UMLFactory.eINSTANCE.createGeneralization();
            selectedElement.getGeneralizations().add(gen);
            gen.setGeneral(existingClass);
            data.set("newSuperClass", existingClass);
        }

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getClass_());

        Class selectedElement = (Class) data.get("selectedElement");
        Class superClass = (Class) data.get("newSuperClass");

        if (superClass == null) {
            return false;
        }

        Variable<EClassifier, EParameter> variable = ExpressionsFactory.eINSTANCE.createVariable();
        variable.setName("newSuperClass");
        variable.setType(UMLPackage.Literals.CLASSIFIER);
        ocl.getEnvironment().addElement(variable.getName(), variable, true);

        OCLExpression<EClassifier> query = helper.createQuery(OCL_POST_CONSTRAINT);
        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);
        eval.getEvaluationEnvironment().add("newSuperClass", superClass);
        if (!eval.check(selectedElement)) {
            return false;
        }

        return true;
    }
}
