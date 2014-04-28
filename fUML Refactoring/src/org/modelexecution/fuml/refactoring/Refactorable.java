package org.modelexecution.fuml.refactoring;

import java.util.Set;

import org.eclipse.ocl.uml.OCL;
import org.eclipse.uml2.uml.Class;

public interface Refactorable {
	boolean checkPreCondition(OCL ocl);
	boolean performRefactoring(Set<Class> allClasses) throws RefactoringException;
	boolean checkPostCondition(OCL ocl);
}
