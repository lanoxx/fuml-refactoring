package org.modelexecution.fuml.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RefactoringTest {

	@Test
	public void test() {

		RefactoringController controller = new RefactoringController();
		controller
				.initializeResourceSet("models/extractSuperclass/extractSuperclass.uml");
		try {
			List<String> classNames = new ArrayList<>();
			classNames.add("Car");
			classNames.add("Truck");
			controller.performRefactoring("extractSuperclass", "Vehicle",
					classNames);
		} catch (RefactoringException e) {
			e.printStackTrace();
		}
	}

}
