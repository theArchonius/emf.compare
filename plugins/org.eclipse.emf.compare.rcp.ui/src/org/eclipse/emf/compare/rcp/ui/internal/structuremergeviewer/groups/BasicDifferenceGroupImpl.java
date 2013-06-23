/*******************************************************************************
 * Copyright (c) 2012, 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.containmentReferenceChange;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasState;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.valueIs;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.compare.ResourceAttachmentChange;
import org.eclipse.emf.compare.provider.utils.ComposedStyledString;
import org.eclipse.emf.compare.provider.utils.IStyledString;
import org.eclipse.emf.compare.provider.utils.IStyledString.Style;
import org.eclipse.emf.compare.rcp.ui.EMFCompareRCPUIPlugin;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.tree.TreeFactory;
import org.eclipse.emf.edit.tree.TreeNode;
import org.eclipse.swt.graphics.Image;

/**
 * This implementation of a {@link IDifferenceGroup} uses a predicate to filter the whole list of differences.
 * <p>
 * This can be subclasses or used directly instead of {@link IDifferenceGroup}.
 * </p>
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 * @since 3.0
 */
public class BasicDifferenceGroupImpl extends AdapterImpl implements IDifferenceGroup {

	/** The filter we'll use in order to filter the differences that are part of this group. */
	protected final Predicate<? super Diff> filter;

	/** The name that the EMF Compare UI will display for this group. */
	protected final String name;

	/** The icon that the EMF Compare UI will display for this group. */
	protected final Image image;

	/** The comparison that is the parent of this group. */
	protected final Comparison comparison;

	/** The list of children of this group. */
	protected List<TreeNode> children;

	/**
	 * Instantiates this group given the comparison and filter that should be used in order to determine its
	 * list of differences.
	 * <p>
	 * This will use the default name and icon for the group.
	 * </p>
	 * 
	 * @param comparison
	 *            The comparison that is the parent of this group.
	 * @param unfiltered
	 *            The whole unfiltered list of differences.
	 * @param filter
	 *            The filter we'll use in order to filter the differences that are part of this group.
	 */
	public BasicDifferenceGroupImpl(Comparison comparison, Predicate<? super Diff> filter) {
		this(comparison, filter, "Group", EMFCompareRCPUIPlugin.getImage("icons/full/toolb16/group.gif")); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Instantiates this group given the comparison and filter that should be used in order to determine its
	 * list of differences. It will be displayed in the UI with the default icon and the given name.
	 * 
	 * @param comparison
	 *            The comparison that is the parent of this group.
	 * @param unfiltered
	 *            The whole unfiltered list of differences.
	 * @param filter
	 *            The filter we'll use in order to filter the differences that are part of this group.
	 * @param name
	 *            The name that the EMF Compare UI will display for this group.
	 */
	public BasicDifferenceGroupImpl(Comparison comparison, Predicate<? super Diff> filter, String name) {
		this(comparison, filter, name, EMFCompareRCPUIPlugin.getImage("icons/full/toolb16/group.gif")); //$NON-NLS-1$
	}

	/**
	 * Instantiates this group given the comparison and filter that should be used in order to determine its
	 * list of differences. It will be displayed in the UI with the given icon and name.
	 * 
	 * @param comparison
	 *            The comparison that is the parent of this group.
	 * @param unfiltered
	 *            The whole unfiltered list of differences.
	 * @param filter
	 *            The filter we'll use in order to filter the differences that are part of this group.
	 * @param name
	 *            The name that the EMF Compare UI will display for this group.
	 * @param image
	 *            The icon that the EMF Compare UI will display for this group.
	 */
	public BasicDifferenceGroupImpl(Comparison comparison, Predicate<? super Diff> filter, String name,
			Image image) {
		this.comparison = comparison;
		this.filter = filter;
		this.name = name;
		this.image = image;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.notify.impl.AdapterImpl#isAdapterForType(java.lang.Object)
	 */
	@Override
	public boolean isAdapterForType(Object type) {
		return type == IDifferenceGroup.class;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.IDifferenceGroup#getComparison()
	 */
	public Comparison getComparison() {
		return comparison;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.IDifferenceGroup#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.IDifferenceGroup#getStyledName()
	 */
	public IStyledString.IComposedStyledString getStyledName() {
		final IStyledString.IComposedStyledString ret = new ComposedStyledString(getName());
		Iterator<EObject> eAllContents = concat(transform(getGroupTree().iterator(), E_ALL_CONTENTS));
		Iterator<EObject> eAllData = transform(eAllContents, TREE_NODE_DATA);
		int unresolvedDiffs = size(Iterators.filter(Iterators.filter(eAllData, Diff.class),
				hasState(DifferenceState.UNRESOLVED)));
		ret.append(" [" + unresolvedDiffs + " unresolved difference", Style.DECORATIONS_STYLER);
		if (unresolvedDiffs > 1) {
			ret.append("s", Style.DECORATIONS_STYLER);
		}
		ret.append("]", Style.DECORATIONS_STYLER);
		return ret;
	}

	/**
	 * Function that returns all contents of the given EObject.
	 */
	protected static final Function<EObject, Iterator<EObject>> E_ALL_CONTENTS = new Function<EObject, Iterator<EObject>>() {
		public Iterator<EObject> apply(EObject eObject) {
			return eObject.eAllContents();
		}
	};

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.IDifferenceGroup#getImage()
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.IDifferenceGroup#getGroupTree()
	 */
	public List<? extends TreeNode> getGroupTree() {
		if (children == null) {
			children = newArrayList();
			for (Match match : comparison.getMatches()) {
				List<? extends TreeNode> buildSubTree = buildSubTree(null, match);
				if (buildSubTree != null) {
					children.addAll(buildSubTree);
				}
			}
			for (MatchResource matchResource : comparison.getMatchedResources()) {
				TreeNode buildSubTree = buildSubTree(matchResource);
				if (buildSubTree != null) {
					children.add(buildSubTree);
				}
			}
		}
		return children;
	}

	/**
	 * Build the sub tree of the given {@link MatchResource}.
	 * 
	 * @param matchResource
	 *            the given MatchResource.
	 * @return the sub tree of the given MatchResource.
	 */
	protected TreeNode buildSubTree(MatchResource matchResource) {
		if (filter.equals(Predicates.alwaysTrue())) {
			return wrap(matchResource);
		}
		return null;
	}

	/**
	 * Build the sub tree of the given {@link Match}.
	 * 
	 * @param parentMatch
	 *            the parent of the given Match.
	 * @param match
	 *            the given Match.
	 * @return the sub tree of the given Match.
	 */
	protected List<TreeNode> buildSubTree(Match parentMatch, Match match) {
		final List<TreeNode> ret = Lists.newArrayList();
		boolean isContainment = false;

		if (parentMatch != null) {
			Collection<Diff> containmentChanges = filter(parentMatch.getDifferences(),
					containmentReferenceForMatch(match));
			if (!containmentChanges.isEmpty()) {
				isContainment = true;
				for (Diff diff : containmentChanges) {
					ret.add(wrap(diff));
				}
			} else {
				ret.add(wrap(match));
			}
		} else {
			Collection<Diff> resourceAttachmentChanges = filter(match.getDifferences(),
					resourceAttachmentChange());
			if (!resourceAttachmentChanges.isEmpty()) {
				for (Diff diff : resourceAttachmentChanges) {
					ret.add(wrap(diff));
				}
			} else {
				ret.add(wrap(match));
			}

		}

		Collection<TreeNode> toRemove = Lists.newArrayList();
		for (TreeNode treeNode : ret) {
			boolean hasDiff = false;
			boolean hasNonEmptySubMatch = false;
			for (Diff diff : filter(match.getDifferences(), and(filter, not(or(containmentReferenceChange(),
					resourceAttachmentChange()))))) {
				hasDiff = true;
				treeNode.getChildren().add(wrap(diff));
			}
			for (Match subMatch : match.getSubmatches()) {
				List<TreeNode> buildSubTree = buildSubTree(match, subMatch);
				if (!buildSubTree.isEmpty()) {
					hasNonEmptySubMatch = true;
					treeNode.getChildren().addAll(buildSubTree);
				}
			}
			if (!(isContainment || hasDiff || hasNonEmptySubMatch || filter.equals(Predicates.alwaysTrue()))) {
				toRemove.add(treeNode);
			}
		}

		ret.removeAll(toRemove);

		return ret;
	}

	/**
	 * This can be used to check whether a givan diff is a resource attachment change.
	 * 
	 * @return The created predicate.
	 */
	protected static Predicate<? super Diff> resourceAttachmentChange() {
		return Predicates.instanceOf(ResourceAttachmentChange.class);
	}

	/**
	 * Predicate to know if the given match contains containment refernce change according to the filter of
	 * the group.
	 * 
	 * @param subMatch
	 *            the given Match.
	 * @return a predicate to know if the given match contains containment refernce change according to the
	 *         filter of the group.
	 */
	@SuppressWarnings("unchecked")
	protected Predicate<Diff> containmentReferenceForMatch(Match subMatch) {
		return and(filter, containmentReferenceChange(), or(valueIs(subMatch.getLeft()), valueIs(subMatch
				.getRight()), valueIs(subMatch.getOrigin())));
	}

	/**
	 * Creates a TreeNode form the given EObject.
	 * 
	 * @param data
	 *            the given EObject.
	 * @return a TreeNode.
	 */
	protected TreeNode wrap(EObject data) {
		TreeNode treeNode = TreeFactory.eINSTANCE.createTreeNode();
		treeNode.setData(data);
		treeNode.eAdapters().add(this);
		return treeNode;
	}
}
