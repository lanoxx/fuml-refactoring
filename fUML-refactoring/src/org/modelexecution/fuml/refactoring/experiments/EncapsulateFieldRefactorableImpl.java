package org.modelexecution.fuml.refactoring.experiments;

import java.util.Collection;

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
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityParameterNode;
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
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

    private final OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private final OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT = "self.visibility <> uml::VisibilityKind::private"
        + " and self.class.ownedOperation->forAll(o | o.isDistinguishableFrom(setOperation, self.namespace)"
        + " and o.isDistinguishableFrom(getOperation, self.namespace))";
    private static final String OCL_POST_CONSTRAINT = "";
    private final RefactoringData data;
    private Operation setOperation;
    private Parameter setOperationInParameter;
    private Operation getOperation;
    private Parameter getOperationOutParameter;

    public EncapsulateFieldRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

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

        Type propertyType = selectedElement.getType();
        String propertyName = selectedElement.getName();
        String normalizedName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

        setOperation = UMLFactory.eINSTANCE.createOperation();
        setOperation.setName("set" + normalizedName);
        setOperationInParameter = setOperation.createOwnedParameter(propertyName, propertyType);
        setOperationInParameter.setDirection(ParameterDirectionKind.IN_LITERAL);

        getOperation = UMLFactory.eINSTANCE.createOperation();
        getOperation.setName("get" + normalizedName);
        getOperationOutParameter = getOperation.createOwnedParameter(propertyName, propertyType);
        getOperationOutParameter.setDirection(ParameterDirectionKind.RETURN_LITERAL);

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
        Property selectedElement = (Property) data.get("selectedElement");

        Class class_ = selectedElement.getClass_();

        class_.getOwnedOperations().add(getOperation);
        class_.getOwnedOperations().add(setOperation);

        selectedElement.setVisibility(VisibilityKind.PRIVATE_LITERAL);

        // ReadStructuralFeature -> Calloperation
        // AddStructuralFeatureValue -> Calloperation

        Activity getActivity = createActivity(class_, selectedElement, false);
        Activity setActivity = createActivity(class_, selectedElement, true);

        class_.getOwnedBehaviors().add(getActivity);
        class_.getOwnedBehaviors().add(setActivity);

        // Get all ReadStructuralFeatureActions from model
        // Context Package
        // self.member->selectByType(Class).member->selectByType(Activity).node->selectByType(ReadStructuralFeatureAction).structuralFeature.qualifiedName

        // Get all AddStructuralFeatureValueActions
        // Context Package
        // self.member->selectByType(Class).member->selectByType(Activity).node->selectByType(AddStructuralFeatureValueAction).structuralFeature.qualifiedName

        OCLExpression<EClassifier> query01 = null;
        try {
            query01 =
                helper.createQuery(String
                        .format("self.class.namespace.member->selectByType(Class).member->selectByType(Activity).node"
                            + "->selectByType(ReadStructuralFeatureAction)->select(r|r.structuralFeature.qualifiedName='%s')",
                                selectedElement.getQualifiedName()));
        } catch (ParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query01);
        Collection<ReadStructuralFeatureAction> object =
            (Collection<ReadStructuralFeatureAction>) eval.evaluate(selectedElement);
        for (ReadStructuralFeatureAction a : object) {
            a.getObject();
            a.getResult();
            CallOperationAction coa = UMLFactory.eINSTANCE.createCallOperationAction();
            coa.setActivity(getActivity);
            coa.setOperation(getOperation);
            coa.getArguments().add(a.getObject());
            coa.getResults().add(a.getResult());
            EcoreUtil.delete(a, true);
        }

        OCLExpression<EClassifier> query02 = null;
        try {
            query02 =
                helper.createQuery(String
                        .format("self.class.namespace.member->selectByType(Class).member->selectByType(Activity).node"
                            + "->selectByType(AddStructuralFeatureValueAction)->select(r|r.structuralFeature.qualifiedName='%s')",
                                selectedElement.getQualifiedName()));
        } catch (ParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        eval = ocl.createQuery(query02);
        Collection<AddStructuralFeatureValueAction> actions =
            (Collection<AddStructuralFeatureValueAction>) eval.evaluate(selectedElement);
        for (AddStructuralFeatureValueAction b : actions) {
            b.getObject();
            b.getValue();
            CallOperationAction coa = UMLFactory.eINSTANCE.createCallOperationAction();
            coa.setActivity(setActivity);
            coa.setOperation(setOperation);
            coa.getArguments().add(b.getObject());
            coa.getArguments().add(b.getValue());
            EcoreUtil.delete(b, true);
        }
        return true;
    }

    /**
     * Creates an {@link Activity} for the new getter and setter.
     * 
     * @param parent the parent {@link Class} of the property to encapsulate.
     * @param property the {@link Property} to encapsulate.
     * @param set {@code true} to create a {@code setter} activity, {@code false} to create a {@code getter} activity.
     * @return the created {@link Activity}.
     */
    private Activity createActivity(Class parent, Property property, boolean set) {
        Activity activity = UMLFactory.eINSTANCE.createActivity();

        // Create a ReadSelfAction
        ReadSelfAction selfAction = UMLFactory.eINSTANCE.createReadSelfAction();
        selfAction.setActivity(activity);

        // Create an OutputPin for the ReadSelfAction
        OutputPin selfOutputPin = UMLFactory.eINSTANCE.createOutputPin();
        selfAction.setResult(selfOutputPin);
        LiteralInteger upperBound = UMLFactory.eINSTANCE.createLiteralInteger();
        upperBound.setValue(1);
        selfOutputPin.setUpperBound(upperBound);
        selfOutputPin.setType(parent);

        if (set) {
            // Creates a setter activity
            // Create a ParameterNode for activity input
            activity.setName(setOperation.getName());
            ActivityParameterNode parameterNodeIn = UMLFactory.eINSTANCE.createActivityParameterNode();
            parameterNodeIn.setActivity(activity);
            parameterNodeIn.setType(property.getType());
            parameterNodeIn.setParameter(setOperationInParameter);
            LiteralInteger parameterNodeInUpper = UMLFactory.eINSTANCE.createLiteralInteger();
            parameterNodeInUpper.setValue(1);
            parameterNodeIn.setUpperBound(parameterNodeInUpper);

            // Create an AddStructuralFeatureValueAction
            AddStructuralFeatureValueAction addFeature = UMLFactory.eINSTANCE.createAddStructuralFeatureValueAction();
            addFeature.setStructuralFeature(property);
            addFeature.setActivity(activity);

            // Create an InputPin for AddStructuralValueAction
            InputPin addFeatureObjectInputPin = UMLFactory.eINSTANCE.createInputPin();
            LiteralInteger addFeatureObjectInputPinUpper = UMLFactory.eINSTANCE.createLiteralInteger();
            addFeatureObjectInputPinUpper.setValue(1);
            addFeature.setObject(addFeatureObjectInputPin);

            // Create ObjectFlow from ReadSelfAction OutputPin to AddStructuralFeatureValueAction InputPin
            ObjectFlow selfToStructuralFeatureFlow = UMLFactory.eINSTANCE.createObjectFlow();
            selfToStructuralFeatureFlow.setSource(selfOutputPin);
            selfToStructuralFeatureFlow.setTarget(addFeatureObjectInputPin);
            // Create the Guard for the ObjectFlow
            LiteralBoolean selfToStructuralGuard = UMLFactory.eINSTANCE.createLiteralBoolean();
            selfToStructuralGuard.setValue(true);
            selfToStructuralFeatureFlow.setGuard(selfToStructuralGuard);
            // Create the Weight for the ObjectFlow
            LiteralInteger selfToStructuralWeight = UMLFactory.eINSTANCE.createLiteralInteger();
            selfToStructuralWeight.setValue(1);
            selfToStructuralFeatureFlow.setWeight(selfToStructuralWeight);
            selfToStructuralFeatureFlow.setActivity(activity);

            // Create an InputPin for AddStructuralValueAction
            InputPin addFeatureValueInputPin = UMLFactory.eINSTANCE.createInputPin();
            LiteralInteger addFeatureValueInputPinUpper = UMLFactory.eINSTANCE.createLiteralInteger();
            addFeatureValueInputPinUpper.setValue(1);
            addFeature.setValue(addFeatureValueInputPin);

            // Create ObjectFlow from ParameterNode OutputPin to AddStructuralFeatureValueAction InputPin
            ObjectFlow parameterNodeToStructuralFeatureFlow = UMLFactory.eINSTANCE.createObjectFlow();
            parameterNodeToStructuralFeatureFlow.setSource(parameterNodeIn);
            parameterNodeToStructuralFeatureFlow.setTarget(addFeatureObjectInputPin);
            // Create the Guard for the ObjectFlow
            LiteralBoolean parameterNodeToStructuralFeatureGuard = UMLFactory.eINSTANCE.createLiteralBoolean();
            parameterNodeToStructuralFeatureGuard.setValue(true);
            parameterNodeToStructuralFeatureFlow.setGuard(parameterNodeToStructuralFeatureGuard);
            // Create the Weight for the ObjectFlow
            LiteralInteger parameterNodeToStructuralFeatureWeight = UMLFactory.eINSTANCE.createLiteralInteger();
            parameterNodeToStructuralFeatureWeight.setValue(1);
            parameterNodeToStructuralFeatureFlow.setWeight(parameterNodeToStructuralFeatureWeight);
            parameterNodeToStructuralFeatureFlow.setActivity(activity);

            // Create an ActivityFinalNode for the activity
            ActivityFinalNode finalNode = UMLFactory.eINSTANCE.createActivityFinalNode();
            finalNode.setActivity(activity);

            ControlFlow controlFlow = UMLFactory.eINSTANCE.createControlFlow();
            controlFlow.setSource(addFeature);
            controlFlow.setTarget(finalNode);
            // Create the Guard for the ObjectFlow
            LiteralBoolean controlFlowGuard = UMLFactory.eINSTANCE.createLiteralBoolean();
            controlFlowGuard.setValue(true);
            controlFlow.setGuard(controlFlowGuard);
            // Create the Weight for the ObjectFlow
            LiteralInteger controlFlowWeight = UMLFactory.eINSTANCE.createLiteralInteger();
            controlFlowWeight.setValue(1);
            controlFlow.setWeight(controlFlowWeight);
            controlFlow.setActivity(activity);
        } else {
            // Creates a getter activity
            // Create a ReadStructuralFeatureAction
            activity.setName(getOperation.getName());
            ReadStructuralFeatureAction readFeature = UMLFactory.eINSTANCE.createReadStructuralFeatureAction();
            readFeature.setStructuralFeature(property);
            readFeature.setActivity(activity);
            readFeature.setName("read" + getOperation.getName().substring(3));

            // Create an InputPin for the ReadStructuralFeatureAction
            InputPin readFeatureInputPin = UMLFactory.eINSTANCE.createInputPin();
            readFeature.setObject(readFeatureInputPin);
            LiteralInteger readFeatureInputPinUpperBound = UMLFactory.eINSTANCE.createLiteralInteger();
            readFeatureInputPinUpperBound.setValue(1);
            readFeatureInputPin.setUpperBound(readFeatureInputPinUpperBound);
            readFeatureInputPin.setType(parent);

            // Create an OutputPin for the ReadStructuralFeatureAction
            OutputPin readFeatureOutputPin = UMLFactory.eINSTANCE.createOutputPin();
            readFeature.setResult(readFeatureOutputPin);
            LiteralInteger readFeatureOutputPinUpperBound = UMLFactory.eINSTANCE.createLiteralInteger();
            readFeatureOutputPinUpperBound.setValue(1);
            readFeatureOutputPin.setUpperBound(readFeatureOutputPinUpperBound);
            readFeatureOutputPin.setType(parent);
            readFeatureOutputPin.setType(property.getType());

            // Create ObjectFlow from ReadSelfAction OutputPin to ReadStructuralFeatureAction InputPin
            ObjectFlow selfToStructuralFeatureFlow = UMLFactory.eINSTANCE.createObjectFlow();
            selfToStructuralFeatureFlow.setSource(selfOutputPin);
            selfToStructuralFeatureFlow.setTarget(readFeatureInputPin);
            // Create the Guard for the ObjectFlow
            LiteralBoolean selfToStructuralGuard = UMLFactory.eINSTANCE.createLiteralBoolean();
            selfToStructuralGuard.setValue(true);
            selfToStructuralFeatureFlow.setGuard(selfToStructuralGuard);
            // Create the Weight for the ObjectFlow
            LiteralInteger selfToStructuralWeight = UMLFactory.eINSTANCE.createLiteralInteger();
            selfToStructuralWeight.setValue(1);
            selfToStructuralFeatureFlow.setWeight(selfToStructuralWeight);
            selfToStructuralFeatureFlow.setActivity(activity);

            // Create ParameterNode for activity result
            ActivityParameterNode parameterNodeOut = UMLFactory.eINSTANCE.createActivityParameterNode();
            parameterNodeOut.setActivity(activity);
            parameterNodeOut.setType(property.getType());
            LiteralInteger parameterNodeUpperBound = UMLFactory.eINSTANCE.createLiteralInteger();
            parameterNodeUpperBound.setValue(1);
            parameterNodeOut.setUpperBound(parameterNodeUpperBound);
            parameterNodeOut.setParameter(getOperationOutParameter);

            // Create ObjectFlow from ReadStructuralFeatureAction OutputPin to ParameterNode
            ObjectFlow readFeatureToParameter = UMLFactory.eINSTANCE.createObjectFlow();
            readFeatureToParameter.setTarget(parameterNodeOut);
            readFeatureToParameter.setSource(readFeatureOutputPin);
            // Create the Guard for the ObjectFlow
            LiteralBoolean readToParameterGuard = UMLFactory.eINSTANCE.createLiteralBoolean();
            readToParameterGuard.setValue(true);
            readFeatureToParameter.setGuard(readToParameterGuard);
            // Create the Weight for the ObjectFlow
            LiteralInteger readFeatureToParameterWeight = UMLFactory.eINSTANCE.createLiteralInteger();
            readFeatureToParameterWeight.setValue(1);
            readFeatureToParameter.setWeight(readFeatureToParameterWeight);
            readFeatureToParameter.setActivity(activity);
        }

        return activity;
    }

    @Override
    public boolean checkPostCondition() throws ParserException {
        helper.setContext(UMLPackage.eINSTANCE.getClass_());

        Property selectedElement = (Property) data.get("selectedElement");

        Variable<EClassifier, EParameter> getVariable = ExpressionsFactory.eINSTANCE.createVariable();
        getVariable.setName("getOperation");
        getVariable.setType(UMLPackage.Literals.PROPERTY);
        ocl.getEnvironment().addElement(getVariable.getName(), getVariable, true);

        Variable<EClassifier, EParameter> setVariable = ExpressionsFactory.eINSTANCE.createVariable();
        setVariable.setName("setOperation");
        setVariable.setType(UMLPackage.Literals.PROPERTY);
        ocl.getEnvironment().addElement(setVariable.getName(), setVariable, true);

        OCLExpression<EClassifier> query = helper.createQuery(OCL_POST_CONSTRAINT);
        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

        eval.getEvaluationEnvironment().add("getOperation", getOperation);
        eval.getEvaluationEnvironment().add("setOperation", setOperation);
        return eval.check(selectedElement);
    }
}
