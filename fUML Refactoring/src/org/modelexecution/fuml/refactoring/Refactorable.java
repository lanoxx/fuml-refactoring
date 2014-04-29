package org.modelexecution.fuml.refactoring;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ecore.Constraint;

public interface Refactorable {
	boolean checkPreCondition(
			OCL<?, EClassifier, ?, ?, ?, ?, ?, ?, ?, Constraint, EClass, EObject> ocl,
			Resource resource, String name);

	boolean performRefactoring(Resource resource, String superClassName,
			List<String> classNames) throws RefactoringException;

	boolean checkPostCondition(
			OCL<?, EClassifier, ?, ?, ?, ?, ?, ?, ?, Constraint, EClass, EObject> ocl);
}
