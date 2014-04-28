package org.modelexecution.fuml.refactoring.refactorings

import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.uml.OCL;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.modelexecution.fuml.refactoring.Refactorable;
import org.modelexecution.fuml.refactoring.RefactoringException;

public class ExtractSuperClass implements Refactorable {
	
	
	//Fields
	/**
	 * 1. Check that new class name does not exist
	 * 2. ...
	 * 
	 * Contains a list of OCL Query strings that assert the pre-conditions
	 */
	List<String> oclPreConstrains = new ArrayList();
	
	
	/**
	 * 1. Check that the new class name does exist
	 * 2. ...
	 * 
	 * Contains a list of OCL query strings that assert the post-conditions
	 */
	List<String> oclPostConstrains = new ArrayList();

	//Constructor
	public ExtractSuperclass() { }
	
	//Methods
	@Override
	public boolean checkPreCondition(OCL ocl) {
		//check ocl pre constrains
		//checken mit ECore
		//mit Queries: http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.ocl.doc%2Fhelp%2FOCLInterpreterTutorial.html&cp=45_3_1

		return false;
	}


	@Override
	public boolean performRefactoring(Set<Class> allClasses)
			throws RefactoringException {
		//...
		//perform refactoring
		return false;
	}


	@Override
	public boolean checkPostCondition(OCL ocl) {
		//check ocl post constrains
		//...
		// TODO Auto-generated method stub
		return false;
	}

}
