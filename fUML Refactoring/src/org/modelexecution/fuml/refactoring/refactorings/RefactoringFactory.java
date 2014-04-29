package org.modelexecution.fuml.refactoring.refactorings;

import org.modelexecution.fuml.refactoring.Refactorable;

public class RefactoringFactory {

	public static Refactorable getInstance(String refactoring) {
		switch(refactoring) {
		case "extractSuperclass": {
			 return new ExtractSuperClass2();
		}
		default: return null;
		}
	}
}
