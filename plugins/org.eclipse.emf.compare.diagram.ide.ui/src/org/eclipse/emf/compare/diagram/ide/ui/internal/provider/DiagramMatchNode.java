/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.diagram.ide.ui.internal.provider;

import org.eclipse.compare.ITypedElement;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.MatchNode;
import org.eclipse.emf.ecore.EObject;

/**
 * Specific AbstractEDiffNode for {@link Match} objects.
 * 
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
public class DiagramMatchNode extends MatchNode {

	/**
	 * Creates a node with the given factory.
	 * 
	 * @param adapterFactory
	 *            the factory given to the super constructor.
	 */
	public DiagramMatchNode(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.notify.impl.AdapterImpl#getTarget()
	 */
	@Override
	public Match getTarget() {
		return super.getTarget();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.MatchNode#getType()
	 */
	@Override
	public String getType() {
		return "view";
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.MatchNode#getAncestor()
	 */
	@Override
	public ITypedElement getAncestor() {
		EObject o = getTarget().getOrigin();
		if (o != null) {
			return new DiagramIDEEObjectNode(getAdapterFactory(), o);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.MatchNode#getLeft()
	 */
	@Override
	public ITypedElement getLeft() {
		EObject o = getTarget().getLeft();
		if (o != null) {
			return new DiagramIDEEObjectNode(getAdapterFactory(), o);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.MatchNode#getRight()
	 */
	@Override
	public ITypedElement getRight() {
		EObject o = getTarget().getRight();
		if (o != null) {
			return new DiagramIDEEObjectNode(getAdapterFactory(), o);
		}
		return null;
	}

}
