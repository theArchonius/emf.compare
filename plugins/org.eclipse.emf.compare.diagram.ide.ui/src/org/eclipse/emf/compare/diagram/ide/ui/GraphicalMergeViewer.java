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
package org.eclipse.emf.compare.diagram.ide.ui;

import org.eclipse.gef.ui.parts.AbstractEditPartViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
public abstract class GraphicalMergeViewer extends DMergeViewer {

	private final ISelectionChangedListener fForwardingSelectionListener;

	/**
	 * @param parent
	 * @param side
	 */
	public GraphicalMergeViewer(Composite parent, MergeViewerSide side) {
		super(side);

		createControl(parent);
		hookControl();

		fForwardingSelectionListener = new ForwardingViewerSelectionListener();
		getGraphicalViewer().addSelectionChangedListener(fForwardingSelectionListener);
	}

	@Override
	protected void handleDispose(DisposeEvent event) {
		getGraphicalViewer().removeSelectionChangedListener(fForwardingSelectionListener);
		super.handleDispose(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.diagram.ide.ui#getGraphicalViewer()
	 */
	@Override
	protected abstract AbstractEditPartViewer getGraphicalViewer();

	private class ForwardingViewerSelectionListener implements ISelectionChangedListener {
		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			fireSelectionChanged();
		}

	}
}
