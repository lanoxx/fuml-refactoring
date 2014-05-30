package org.modelexecution.fuml.refactoring.experiments;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
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
import org.eclipse.ocl.expressions.ExpressionsFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.expressions.Variable;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityParameterNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ReadSelfAction;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class EncapsulateFieldRefactorableImpl implements Refactorable {

    private OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT =
        "self.visibility <> uml::VisibilityKind::private"
            + " and self.owner.oclAsType(Class).ownedOperation->forAll(o | o.isDistinguishableFrom(setOperation, self.namespace)"
            + " and o.isDistinguishableFrom(getOperation, self.namespace))";
    private static final String OCL_POST_CONSTRAINT = "";
    private RefactoringData data;
    private Operation setOperation;
    private Operation getOperation;

    public EncapsulateFieldRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("selectedElement") != null);
    }

    /**
     * Loads the root {@link Model} of an UML file.
     * 
     * @return the root {@link Model}.
     */
    private Model loadModel(Resource resource) {
        TreeIterator<EObject> iterator = resource.getAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof Model) {
                return (Model) eObject;
            }
        }
        return null;
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
        helper.setContext(UMLPackage.eINSTANCE.getClass_());

        Property selectedElement = (Property) data.get("selectedElement");

        OCLExpression<EClassifier> query;

        Variable<EClassifier, EParameter> getVariable = ExpressionsFactory.eINSTANCE.createVariable();
        getVariable.setName("getOperation");
        getVariable.setType(UMLPackage.Literals.PROPERTY);
        ocl.getEnvironment().addElement(getVariable.getName(), getVariable, true);

        Variable<EClassifier, EParameter> setVariable = ExpressionsFactory.eINSTANCE.createVariable();
        setVariable.setName("setOperation");
        setVariable.setType(UMLPackage.Literals.PROPERTY);
        ocl.getEnvironment().addElement(setVariable.getName(), setVariable, true);

        query = helper.createQuery(OCL_PRE_CONSTRAINT);

        Property property = (Property) data.get("selectedElement");
        Type propertyType = property.getType();
        String propertyName = property.getName();
        String normalizedName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

        setOperation = UMLFactory.eINSTANCE.createOperation();
        setOperation.setName("set" + normalizedName);
        setOperation.createOwnedParameter("policyNumber", propertyType);

        getOperation = UMLFactory.eINSTANCE.createOperation();
        getOperation.setName("get" + normalizedName);

        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);
        eval.getEvaluationEnvironment().add("getOperation", getOperation);
        eval.getEvaluationEnvironment().add("setOperation", setOperation);

        return eval.check(selectedElement);
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

        Class class_ = selectedElement.getClass_();

        class_.getOwnedOperations().add(getOperation);
        class_.getOwnedOperations().add(setOperation);

        selectedElement.setVisibility(VisibilityKind.PRIVATE_LITERAL);

        // ReadStructuralFeATURE -> calloperation
        // WriteStructuralFeature -> Calloperation

        Activity getActivity = creategetActivity(class_, selectedElement);
        Activity setActivity = createSetActivity(class_, selectedElement);

        class_.getOwnedBehaviors().add(getActivity);
        class_.getOwnedBehaviors().add(setActivity);

        return true;
    }

    private Activity creategetActivity(Class parent, Property property, boolean set) {
        Activity activity = UMLFactory.eINSTANCE.createActivity();

        ReadSelfAction selfAction = UMLFactory.eINSTANCE.createReadSelfAction();
        selfAction.setActivity(activity);

        OutputPin selfOutputPin = UMLFactory.eINSTANCE.createOutputPin();
        selfAction.setResult(selfOutputPin);

        if (set) {

        } else {
            ReadStructuralFeatureAction readFeature = UMLFactory.eINSTANCE.createReadStructuralFeatureAction();
            readFeature.setStructuralFeature(property);

            OutputPin readFeatureOutputPin = UMLFactory.eINSTANCE.createOutputPin();
            readFeature.setResult(readFeatureOutputPin);

            InputPin readFeatureInputPin = UMLFactory.eINSTANCE.createInputPin();
            readFeature.setObject(readFeatureInputPin);

            ObjectFlow selfToStructuralFeatureFlow = UMLFactory.eINSTANCE.createObjectFlow();
            LiteralBoolean selfToStructuralGuard = UMLFactory.eINSTANCE.createLiteralBoolean();
            selfToStructuralGuard.setValue(true);
            selfToStructuralFeatureFlow.setGuard(selfToStructuralGuard);
            selfToStructuralFeatureFlow.setTarget(readFeatureInputPin);
            selfToStructuralFeatureFlow.setSource(selfOutputPin);

            ActivityParameterNode parameterNode = UMLFactory.eINSTANCE.createActivityParameterNode();
            parameterNode.setActivity(activity);
            parameterNode.setType(property.getType());
            LiteralInteger integer = UMLFactory.eINSTANCE.createLiteralInteger();
            integer.setValue(1);
            parameterNode.setUpperBound(integer);

            ObjectFlow readToParameter = UMLFactory.eINSTANCE.createObjectFlow();
            readToParameter.setSource(readFeatureOutputPin);
            readToParameter.setTarget(parameterNode);
        }

        return activity;
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
