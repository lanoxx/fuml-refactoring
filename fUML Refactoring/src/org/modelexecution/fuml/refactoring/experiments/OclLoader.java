package org.modelexecution.fuml.refactoring.experiments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.CallOperationAction;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.ecore.SendSignalAction;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.ocl.uml.OCL;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.internal.impl.ActivityImpl;
import org.eclipse.uml2.uml.internal.impl.ClassImpl;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.modelexecution.fuml.refactoring.refactorings.ExtractSuperClass;

public class OclLoader {

	public List<org.eclipse.ocl.ecore.Constraint> loadEcoreOcl() {
		URI uri = URI.createFileURI("models/extractSuperclass/extractSuperclass.uml");
		final ResourceSet set = new ResourceSetImpl();
		
		/* We need to register a resource factory to avoid a runtime exception */
		set.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		set.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		EcoreEnvironmentFactory ecoreEnvFactory = new EcoreEnvironmentFactory();
		ecoreEnvFactory.loadEnvironment(set.getResource(uri, true));
		org.eclipse.ocl.OCL<EPackage,EClassifier,EOperation,EStructuralFeature,EEnumLiteral,EParameter,
		                    EObject,CallOperationAction,SendSignalAction,org.eclipse.ocl.ecore.Constraint,EClass,EObject>
			ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);

		OCLExpression<EClassifier> query = null;
		EClassifier context = null;
		try {
			for(EObject eclass : set.getResources().get(0).getContents()) {
				for (EObject object : eclass.eContents()) {
					System.out.println(object.getClass().getName());
					context = object.eClass();
//					if(object instanceof ClassImpl) {
//						ClassImpl clazz = (ClassImpl) object;
//						if(clazz.getName().equals("InsurancePolicy")) {
//							context = clazz.eClass();
//						}
//					}
				}
			}
			
		    // create an OCL helper object
		    OCLHelper<EClassifier, ?, ?, Constraint> helper = ocl.createOCLHelper();
		    // set the OCL context classifier
		    helper.setContext(context); //UMLPackage.Literals.CLASS
		    System.out.println(helper.getContextClassifier());
		    query = helper.createQuery("name.at(1)=name.at(1).toUpperCase()");
		} catch ( ParserException e) {
			e.printStackTrace();
		}
		
		if(query != null) {
		    // use the query expression parsed before to create a Query
		    Query<EClassifier, EClass, EObject> eval = ocl.createQuery(query);

		    //Collection<?> result = (Collection<?>) 
		    System.out.println(eval.evaluate(set));
		    //System.out.println(result);
		}
		return null;
		
		/*
		//Load file stream for ocl file
		InputStream stream;
		try {
			stream = new FileInputStream("models/extractSuperclass/extractSuperclass_eCore.ocl");
			OCLInput document = new OCLInput(stream);
			List<org.eclipse.ocl.ecore.Constraint> constraints = ocl.parse(document);
			return constraints; }
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (ParserException e) { e.printStackTrace(); }
		return null;*/
	}

	public static void main(String[] args) {
		OclLoader loader = new OclLoader();
		// Variante 1: OCL mit UMLEnvironmentFactory:
		// loader.loadOcl();
		// Variante 2: OCL mit EcoreEnvironmentFactory:
		loader.loadEcoreOcl();
	}

}
