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
package org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer;

import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;
import java.util.Iterator;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareConstants;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareIDEUIPlugin;
import org.eclipse.emf.compare.ide.ui.internal.actions.filter.DifferenceFilter;
import org.eclipse.emf.compare.ide.ui.internal.actions.filter.FilterActionMenu;
import org.eclipse.emf.compare.ide.ui.internal.actions.group.DifferenceGrouper;
import org.eclipse.emf.compare.ide.ui.internal.actions.group.GroupActionMenu;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.CompareConfigurationExtension;
import org.eclipse.emf.compare.ide.ui.internal.util.EMFCompareEditingDomain;
import org.eclipse.emf.compare.ide.ui.logical.EMFSynchronizationModel;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class EMFCompareStructureMergeViewer extends DiffTreeViewer implements CommandStackListener {

	private final ICompareInputChangeListener fCompareInputChangeListener;

	private final ComposedAdapterFactory fAdapterFactory;

	private final CompareViewerSwitchingPane fParent;

	private Object fRoot;

	/**
	 * The difference filter that will be applied to the structure viewer. Note that this will be initialized
	 * from {@link #createToolItems(ToolBarManager)} since that method is called from the super-constructor
	 * and we cannot init ourselves beforehand.
	 */
	private DifferenceFilter differenceFilter;

	/**
	 * This will be used by our adapter factory in order to group together the differences located under the
	 * Comparison. Note that this will be initialized from {@link #createToolItems(ToolBarManager)} since that
	 * method is called from the super-constructor and we cannot init ourselves beforehand.
	 */
	private DifferenceGrouper differenceGrouper;

	/**
	 * @param parent
	 * @param configuration
	 */
	public EMFCompareStructureMergeViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);

		fAdapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		fAdapterFactory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());
		fAdapterFactory.addAdapterFactory(new ResourceItemProviderAdapterFactory());

		boolean leftIsLocal = CompareConfigurationExtension.getBoolean(configuration, "LEFT_IS_LOCAL", false); //$NON-NLS-1$
		setLabelProvider(new EMFCompareStructureMergeViewerLabelProvider(fAdapterFactory, this, leftIsLocal));
		setContentProvider(new EMFCompareStructureMergeViewerContentProvider(fAdapterFactory,
				differenceGrouper));

		if (parent instanceof CompareViewerSwitchingPane) {
			fParent = (CompareViewerSwitchingPane)parent;
		} else {
			fParent = null;
		}

		fCompareInputChangeListener = new ICompareInputChangeListener() {
			public void compareInputChanged(ICompareInput input) {
				EMFCompareStructureMergeViewer.this.compareInputChanged(input);
			}
		};

		// Wrap the defined comparer in our own.
		setComparer(new DiffNodeComparer(super.getComparer()));
	}

	/**
	 * Triggered by fCompareInputChangeListener
	 */
	void compareInputChanged(ICompareInput input) {
		if (input == null) {
			// When closing, we don't need a progress monitor to handle the input change
			compareInputChanged(null, new NullProgressMonitor());
			return;
		}
		CompareConfiguration cc = getCompareConfiguration();
		// The compare configuration is nulled when the viewer is disposed
		if (cc != null) {
			BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
				public void run() {
					try {
						inputChangedTask.run(new NullProgressMonitor());
					} catch (InvocationTargetException e) {
						EMFCompareIDEUIPlugin.getDefault().log(e.getTargetException());
					} catch (InterruptedException e) {
						// Ignore
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#getRoot()
	 */
	@Override
	protected Object getRoot() {
		return fRoot;
	}

	private IRunnableWithProgress inputChangedTask = new IRunnableWithProgress() {
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask("Computing Structure Differences", 100); //$NON-NLS-1$
			compareInputChanged((ICompareInput)getInput(), new SubProgressMonitor(monitor, 100));
			monitor.done();
		}
	};

	void compareInputChanged(ICompareInput input, IProgressMonitor monitor) {
		Object previousResult = getCompareConfiguration().getProperty(EMFCompareConstants.COMPARE_RESULT);
		if (previousResult instanceof Comparison) {
			compareInputChanged((Comparison)previousResult);
		} else if (input != null) {
			final ITypedElement left = input.getLeft();
			final ITypedElement right = input.getRight();
			final ITypedElement origin = input.getAncestor();
			final EMFSynchronizationModel syncModel = EMFSynchronizationModel.createSynchronizationModel(
					left, right, origin);
			syncModel.minimize();

			final ResourceSet leftResourceSet = syncModel.getLeftResourceSet();
			final ResourceSet rightResourceSet = syncModel.getRightResourceSet();
			final ResourceSet originResourceSet;
			if (origin == null) {
				// FIXME why would an empty resource set yield a different result ?
				originResourceSet = null;
			} else {
				originResourceSet = syncModel.getOriginResourceSet();
			}

			final IComparisonScope scope = EMFCompare.createDefaultScope(leftResourceSet, rightResourceSet,
					originResourceSet);
			final Comparison compareResult = EMFCompare.newComparator(scope).setMonitor(
					BasicMonitor.toMonitor(monitor)).compare();
			EMFCompareEditingDomain editingDomain = new EMFCompareEditingDomain(compareResult,
					leftResourceSet, rightResourceSet, originResourceSet);
			getCompareConfiguration().setProperty(EMFCompareConstants.EDITING_DOMAIN, editingDomain);

			editingDomain.getCommandStack().addCommandStackListener(this);
			compareInputChanged(compareResult);
		} else {
			fRoot = null;
		}
	}

	private static void unload(ResourceSet resourceSet) {
		if (resourceSet != null) {
			for (Resource resource : resourceSet.getResources()) {
				resource.unload();
			}
			resourceSet.getResources().clear();
		}
	}

	private static ResourceSet getResourceSet(EObject eObject) {
		if (eObject != null) {
			Resource eResource = eObject.eResource();
			if (eResource != null) {
				return eResource.getResourceSet();
			}
		}
		return null;
	}

	void compareInputChanged(final Comparison comparison) {
		getCompareConfiguration().setProperty(EMFCompareConstants.COMPARE_RESULT, comparison);
		fRoot = fAdapterFactory.adapt(comparison, IDiffElement.class);

		getCompareConfiguration().getContainer().runAsynchronously(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				String message = null;
				if (comparison.getDifferences().isEmpty()) {
					message = "No Differences"; //$NON-NLS-1$
				}

				if (Display.getCurrent() != null) {
					refreshAfterDiff(message, fRoot);
				} else {
					final String theMessage = message;
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							refreshAfterDiff(theMessage, fRoot);
						}
					});
				}
			}
		});
	}

	private void refreshAfterDiff(String message, Object root) {
		if (getControl().isDisposed()) {
			return;
		}

		if (fParent != null) {
			fParent.setTitleArgument(message);
		}

		refresh(root);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#setComparer(org.eclipse.jface.viewers.IElementComparer)
	 */
	@Override
	public void setComparer(IElementComparer comparer) {
		// Wrap this new comparer in our own
		super.setComparer(new DiffNodeComparer(comparer));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.structuremergeviewer.DiffTreeViewer#createToolItems(org.eclipse.jface.action.ToolBarManager)
	 */
	@Override
	protected void createToolItems(ToolBarManager toolbarManager) {
		super.createToolItems(toolbarManager);

		toolbarManager.add(new GroupActionMenu(getDifferenceGrouper()));
		toolbarManager.add(new FilterActionMenu(getDifferenceFilter()));
	}

	/**
	 * Returns the difference filter that is to be applied on the structure viewer.
	 * <p>
	 * Note that this will be called from {@link #createToolItems(ToolBarManager)}, which is called from the
	 * super-constructor, when we have had no time to initialize the {@link #differenceFilter} field.
	 * </p>
	 * 
	 * @return The difference filter that is to be applied on the structure viewer.
	 */
	protected DifferenceFilter getDifferenceFilter() {
		if (differenceFilter == null) {
			differenceFilter = new DifferenceFilter();
			differenceFilter.install(this);
		}
		return differenceFilter;
	}

	/**
	 * Returns the difference grouper that is to be applied on the structure viewer.
	 * <p>
	 * Note that this will be called from {@link #createToolItems(ToolBarManager)}, which is called from the
	 * super-constructor, when we have had no time to initialize the {@link #differenceGrouper} field.
	 * </p>
	 * 
	 * @return The difference grouper that is to be applied on the structure viewer.
	 */
	protected DifferenceGrouper getDifferenceGrouper() {
		if (differenceGrouper == null) {
			differenceGrouper = new DifferenceGrouper();
			differenceGrouper.install(this);
		}
		return differenceGrouper;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.structuremergeviewer.DiffTreeViewer#inputChanged(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		if (oldInput instanceof ICompareInput) {
			ICompareInput old = (ICompareInput)oldInput;
			old.removeCompareInputChangeListener(fCompareInputChangeListener);
		}
		if (input instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput)input;
			ci.addCompareInputChangeListener(fCompareInputChangeListener);
			compareInputChanged(ci);
			if (input != oldInput) {
				initialSelection();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.structuremergeviewer.DiffTreeViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		Comparison comparison = (Comparison)((Adapter)fRoot).getTarget();
		ResourceSet leftResourceSet = null;
		ResourceSet rightResourceSet = null;
		Iterator<Match> matchIt = comparison.getMatches().iterator();
		if (comparison.isThreeWay()) {
			ResourceSet originResourceSet = null;
			while (matchIt.hasNext()
					&& (leftResourceSet == null || rightResourceSet == null || originResourceSet == null)) {
				Match match = matchIt.next();
				if (leftResourceSet == null) {
					leftResourceSet = getResourceSet(match.getLeft());
				}
				if (rightResourceSet == null) {
					rightResourceSet = getResourceSet(match.getRight());
				}
				if (originResourceSet == null) {
					originResourceSet = getResourceSet(match.getOrigin());
				}
			}
			unload(originResourceSet);
		} else {
			while (matchIt.hasNext() && (leftResourceSet == null || rightResourceSet == null)) {
				Match match = matchIt.next();
				if (leftResourceSet == null) {
					leftResourceSet = getResourceSet(match.getLeft());
				}
				if (rightResourceSet == null) {
					rightResourceSet = getResourceSet(match.getRight());
				}
			}
		}

		unload(leftResourceSet);
		unload(rightResourceSet);

		Object input = getInput();
		if (input instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput)input;
			ci.removeCompareInputChangeListener(fCompareInputChangeListener);
		}
		compareInputChanged((ICompareInput)null);

		EMFCompareEditingDomain editingDomain = (EMFCompareEditingDomain)getCompareConfiguration()
				.getProperty(EMFCompareConstants.EDITING_DOMAIN);
		if (editingDomain != null) {
			editingDomain.getCommandStack().removeCommandStackListener(this);
		}

		super.handleDispose(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.command.CommandStackListener#commandStackChanged(java.util.EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		refresh();
	}
}
