/*******************************************************************************
 * Copyright (c) 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.internal.postprocessor.extension.sequence;

import static com.google.common.base.Predicates.instanceOf;

import com.google.common.collect.Iterables;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.uml2.internal.ExecutionSpecificationChange;
import org.eclipse.emf.compare.uml2.internal.UMLCompareFactory;
import org.eclipse.emf.compare.uml2.internal.UMLDiff;
import org.eclipse.emf.compare.uml2.internal.postprocessor.AbstractUMLChangeFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.Switch;
import org.eclipse.uml2.uml.ExecutionOccurrenceSpecification;
import org.eclipse.uml2.uml.ExecutionSpecification;
import org.eclipse.uml2.uml.InteractionFragment;
import org.eclipse.uml2.uml.Lifeline;

/**
 * Factory for execution specification changes.
 * 
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
public class UMLExecutionSpecificationChangeFactory extends AbstractUMLChangeFactory {

	/**
	 * Discriminants getter for the ExecutionSpecification change.
	 * 
	 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
	 */
	private class ExecutionSpecificationDiscriminantsGetter extends DiscriminantsGetter {

		/**
		 * {@inheritDoc}<br>
		 * Discriminants are the Execution Specifications and the Occurrence Specifications.
		 * 
		 * @see org.eclipse.uml2.uml.util.UMLSwitch#caseExecutionSpecification(org.eclipse.uml2.uml.ExecutionSpecification)
		 */
		@Override
		public Set<EObject> caseExecutionSpecification(ExecutionSpecification object) {
			Set<EObject> result = new HashSet<EObject>();
			result.add(object);
			if (object.getStart() != null) {
				result.add(object.getStart());
			}
			if (object.getFinish() != null) {
				result.add(object.getFinish());
			}
			return result;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.uml2.uml.util.UMLSwitch#caseExecutionOccurrenceSpecification(org.eclipse.uml2.uml.ExecutionOccurrenceSpecification)
		 */
		@Override
		public Set<EObject> caseExecutionOccurrenceSpecification(ExecutionOccurrenceSpecification object) {
			Set<EObject> result = new HashSet<EObject>();
			if (object.getExecution() != null) {
				result.addAll(caseExecutionSpecification(object.getExecution()));
			}
			return result;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.uml2.uml.util.UMLSwitch#caseLifeline(org.eclipse.uml2.uml.Lifeline)
		 */
		@Override
		public Set<EObject> caseLifeline(Lifeline object) {
			Set<EObject> result = new HashSet<EObject>();
			for (InteractionFragment fragment : object.getCoveredBys()) {
				result.addAll(doSwitch(fragment));
			}
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#getExtensionKind()
	 */
	@Override
	public Class<? extends UMLDiff> getExtensionKind() {
		return ExecutionSpecificationChange.class;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.internal.postprocessor.factories.AbstractChangeFactory#createExtension()
	 */
	@Override
	public UMLDiff createExtension() {
		return UMLCompareFactory.eINSTANCE.createExecutionSpecificationChange();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.uml2.internal.postprocessor.AbstractUMLChangeFactory#getDiscriminant(org.eclipse.emf.compare.Diff)
	 */
	@Override
	protected EObject getDiscriminant(Diff input) {
		return Iterables.find(getDiscriminants(input), instanceOf(ExecutionSpecification.class), null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.uml2.internal.postprocessor.AbstractUMLChangeFactory#getDiscriminantsGetter()
	 */
	@Override
	protected Switch<Set<EObject>> getDiscriminantsGetter() {
		return new ExecutionSpecificationDiscriminantsGetter();
	}

}
