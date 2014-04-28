package org.modelexecution.fuml.refactoring.refactorings

import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
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
	 * 2. Check that the original class extends the new class
	 * 3. Check that the attribute exits in the super class not in any subclass
	 * 4. Check that the methods exit in the super class
	 * 
	 * Contains a list of OCL query strings that assert the post-conditions
	 */
	List<String> oclPostConstrains = new ArrayList();

	//Constructor
	public ExtractSuperclass() { }
	
	//Methods
	@Override
	public boolean checkPreCondition(OCL ocl) {
		
		try {
			Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);
			Iterator<Class> allClassesIterator = allClasses.iterator();
			while(allClassesIterator.hasNext()) {
				Class class_ = allClassesIterator.next();
				boolean result = eval.check(class_);
				System.out.println(class_.getName() + ": " + result);
			}
		
		} catch (ParserException e) {
			System.err.println(e.getLocalizedMessage());
		}
		//check ocl pre constrains
		//checken mit ECore
		//mit Queries: http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.ocl.doc%2Fhelp%2FOCLInterpreterTutorial.html&cp=45_3_1

		return false;
	}


	/**
	 * TODO: needs additional parameters 
	 *  * Needs to pass name of the new super class
	 *  * Needs to pass the names of attributes
	 *  * Needs to pass the names of methods
	 */
	@Override
	public boolean performRefactoring(Set<Class> allClasses)
			throws RefactoringException
	{
		//...
		//perform refactoring
				
		// 1. get reference to the class which should be refactored
		// 2. create new class with the new name
		// 3. move attribute from original class to super class (for each class)
		// 4. move methods from original class to super class (for each class)
		// 5. extend the original class from the new super class

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
