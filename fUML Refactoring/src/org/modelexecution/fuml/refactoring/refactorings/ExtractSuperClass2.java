package org.modelexecution.fuml.refactoring.refactorings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.GeneralizationSet;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class ExtractSuperClass2 implements Refactorable {

	// Fields
	/**
	 * 1. Check that new class name does not exist 2. ...
	 * 
	 * Contains a list of OCL Query strings that assert the pre-conditions
	 */
	String[] oclPreConstrains;

	/**
	 * 1. Check that the new class name does exist 2. Check that the original
	 * class extends the new class 3. Check that the attribute exits in the
	 * super class not in any subclass 4. Check that the methods exit in the
	 * super class
	 * 
	 * Contains a list of OCL query strings that assert the post-conditions
	 */
	List<String> oclPostConstrains = new ArrayList<>();

	// Constructor
	public ExtractSuperClass2() {

	}

	// Methods
	@Override
	public boolean checkPreCondition(
			OCL<?, EClassifier, ?, ?, ?, ?, ?, ?, ?, Constraint, EClass, EObject> ocl,
			Resource resource, String name) {
		OCLExpression<EClassifier> query = null;
		try {
			OCLHelper<EClassifier, ?, ?, Constraint> helper = ocl
					.createOCLHelper();
			helper.setContext(UMLPackage.eINSTANCE.getClass_());
			// setContext(UMLPackage.eINSTANCE.getClass_());

			// 1.
			query = helper.createQuery("self.name <> '" + name + "'");

			Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);
			Set<Class> allClasses = getAllClasses(resource);
			Iterator<Class> allClassesIterator = allClasses.iterator();
			while (allClassesIterator.hasNext()) {
				Class class_ = allClassesIterator.next();
				if (!eval.check(class_)) {
					return false;
				}
			}

			// 2.

		} catch (ParserException e) {
			System.err.println(e.getLocalizedMessage());
			return false;
		}
		// check ocl pre constrains
		// checken mit ECore
		// mit Queries:
		// http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.ocl.doc%2Fhelp%2FOCLInterpreterTutorial.html&cp=45_3_1

		return true;
	}

	/**
	 * Get all classes from the resource set (which are not Activity objects).
	 */
	private Set<Class> getAllClasses(Resource modelResource) {
		Set<Class> allClasses = new HashSet<>();
		TreeIterator<EObject> iterator = modelResource.getAllContents();
		while (iterator.hasNext()) {
			EObject eObject = iterator.next();
			if (eObject instanceof Class && !(eObject instanceof Activity)) {
				allClasses.add((Class) eObject);
			}
		}
		return allClasses;
	}

	private Model getModel(Resource modelResource) {
		TreeIterator<EObject> iterator = modelResource.getAllContents();
		while (iterator.hasNext()) {
			EObject eObject = (EObject) iterator.next();
			if (eObject instanceof Model) {
				return (Model) eObject;
			}
		}
		return null;
	}

	/**
	 * TODO: needs additional parameters * Needs to pass name of the new super
	 * class * Needs to pass the names of attributes * Needs to pass the names
	 * of methods
	 */
	@Override
	public boolean performRefactoring(Resource resource, String superClassName,
			List<String> classNames) throws RefactoringException {
		// perform refactoring
		UMLFactory eInstance = UMLFactory.eINSTANCE;
		Class superClass = eInstance.createClass();
		superClass.setName(superClassName);

		Model model = getModel(resource);
		GeneralizationSet set = eInstance.createGeneralizationSet();

		Set<Class> allClasses = getAllClasses(resource);

		List<Class> specializations = new ArrayList<>();
		for (String className : classNames) {
			for (Class modelClass : allClasses) {
				if (modelClass.equals(className)) {
					specializations.add(modelClass);

					Generalization generalization = eInstance
							.createGeneralization();
					generalization.setGeneral(superClass);
					generalization.setSpecific(modelClass);
				}
			}
		}

		// 1. get reference to the class which should be refactored
		// 2. create new class with the new name
		// 3. move attribute from original class to super class (for each class)
		// 4. move methods from original class to super class (for each class)
		// 5. extend the original class from the new super class

		return false;
	}

	@Override
	public boolean checkPostCondition(OCL ocl) {
		// TODO Auto-generated method stub
		return false;
	}

}
