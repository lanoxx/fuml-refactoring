package org.modelexecution.fuml.refactoring.experiments;

import java.util.Collection;
import java.util.Set;

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
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class PullUpPropertyRefactorableImpl implements Refactorable {

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    // Context Property
    private static final String OCL_PRE_CONSTRAINT =
        "self.class.superClass->select(s | s.name = selectedSuperClass.name)->notEmpty()"
            + " and self.visibility <> uml::VisibilityKind::private"
            + " and self.class.inheritedMember->selectByType(Property)"
            + "->forAll(prop | prop.name = self.name and prop.type = self.type"
            + " and prop.upper = self.upper and prop.lower = self.lower)";
    private static final String OCL_PRE_CONSTRAINT_ADDITIONAL_ELEMENTS =
        "self.name = otherProperty.name and self.type = otherProperty.type and self.upper = otherProperty.upper and self.lower = otherProperty.lower";

    private static final String OCL_POST_CONSTRAINT = "";

    private final RefactoringData data;

    public PullUpPropertyRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("selectedElement") != null);
        assert (data.get("selectedSuperClass") != null);
        assert (data.get("additionalElements") != null);
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
        Set<Property> additionalElements = (Set<Property>) data.get("additionalElements");

        Variable<EClassifier, EParameter> variable = ExpressionsFactory.eINSTANCE.createVariable();
        variable.setName("selectedSuperClass");
        variable.setType(UMLPackage.Literals.CLASSIFIER);
        ocl.getEnvironment().addElement(variable.getName(), variable, true);

        OCLExpression<EClassifier> expression = helper.createQuery(OCL_PRE_CONSTRAINT);

        Query<EClassifier, EClass, EObject> query = ocl.createQuery(expression);
        ocl.getEvaluationEnvironment().add("selectedSuperClass", superClass);

        boolean success = query.check(selectedElement);

        /*
         * If there are additional properties, then we also need to check for each additional property that it is
         * identical to the selected element and that it satisfies the normal preconstraint.
         */
        if (additionalElements != null && additionalElements.size() > 0) {
            for (Property property : additionalElements) {
                variable = ExpressionsFactory.eINSTANCE.createVariable();
                variable.setName("selectedSuperClass");
                variable.setType(UMLPackage.Literals.CLASSIFIER);
                ocl.getEnvironment().addElement(variable.getName(), variable, true);

                expression = helper.createQuery(OCL_PRE_CONSTRAINT);

                query = ocl.createQuery(expression);

                success = success && query.check(property);

                variable = ExpressionsFactory.eINSTANCE.createVariable();
                variable.setName("otherProperty");
                variable.setType(UMLPackage.Literals.PROPERTY);
                ocl.getEnvironment().addElement(variable.getName(), variable, true);

                expression = helper.createQuery(OCL_PRE_CONSTRAINT_ADDITIONAL_ELEMENTS);

                query = ocl.createQuery(expression);
                ocl.getEvaluationEnvironment().add("otherProperty", property);

                success = success && query.check(selectedElement);
            }
        }

        return success;
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
        Set<Property> additionalElements = (Set<Property>) data.get("additionalElements");

        superClass.getOwnedAttributes().add(selectedElement);

        if (additionalElements != null && additionalElements.size() > 0) {
            for (Property property : additionalElements) {

                // for each element that references the property we need to replace it by the selectedElement
                OCLExpression<EClassifier> readStructuralExpression = null;
                try {
                    readStructuralExpression =
                        helper.createQuery(String
                                .format("uml::StructuralFeatureAction.allInstances()->select(r|r.structuralFeature.qualifiedName='%s')",
                                        property.getQualifiedName()));
                } catch (ParserException e) {
                    e.printStackTrace();
                }

                Query<EClassifier, EClass, EObject> query = ocl.createQuery(readStructuralExpression);

                Collection<ReadStructuralFeatureAction> object =
                    (Collection<ReadStructuralFeatureAction>) query.evaluate(selectedElement);

                for (ReadStructuralFeatureAction action : object) {
                    action.setStructuralFeature(selectedElement);
                }

                // remove the property from its owning class
                property.getClass_().getOwnedAttributes().remove(property);

            }
        }

        return true;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        return true;
    }
}
