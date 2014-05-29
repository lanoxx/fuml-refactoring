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
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringData;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class RenamePropertyRefactorableImpl implements Refactorable {

    private OCL<?, EClassifier, ?, ?, ?, EParameter, ?, ?, ?, Constraint, EClass, EObject> ocl;
    private OCLHelper<EClassifier, ?, ?, Constraint> helper;

    private static final String OCL_PRE_CONSTRAINT = "self.namespace.member->"
        + "select(class | class.oclIsTypeOf(Class)).oclAsType(Class).name->forAll(o | o <> '%s')";
    private static final String OCL_POST_CONSTRAINT = "self.general->includes(newSuperClass)";
    private RefactoringData data;

    public RenamePropertyRefactorableImpl(RefactoringData data) {
        this.ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        this.helper = ocl.createOCLHelper();
        this.data = data;

        assert (data.get("newSuperClassName") != null);
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

        Class selectedElement = (Class) data.get("selectedElement");
        String newSuperClassName = (String) data.get("newSuperClassName");

        OCLExpression<EClassifier> query;
        query = helper.createQuery(String.format(OCL_PRE_CONSTRAINT, newSuperClassName));

        Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

        for (Class clazz : loadAllClasses(selectedElement)) {
            if (!eval.check(clazz)) {
                return false;
            }
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
