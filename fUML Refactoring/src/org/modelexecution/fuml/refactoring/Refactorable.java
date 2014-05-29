package org.modelexecution.fuml.refactoring;

import org.eclipse.ocl.ParserException;

public interface Refactorable {
    boolean checkPreCondition() throws ParserException;

    boolean performRefactoring() throws RefactoringException;

    boolean checkPostCondition() throws ParserException;
}
