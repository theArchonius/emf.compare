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
package org.eclipse.emf.compare.commands;

import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasState;

import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.ecore.change.util.ChangeRecorder;
import org.eclipse.emf.edit.command.ChangeCommand;

/**
 * This command can be used to copy a number of diffs (or a single one) in a given direction.
 * <p>
 * <b>Note</b> that this will merge <i>all</i> differences that are located on the <i>source</i> side (i.e :
 * LEFT if copying from left to right) and <b>not</b> in a conflicting state.
 * </p>
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public class CopyAllNonConflictingCommand extends ChangeCommand {
	/** The list of differences we are to merge. */
	private final List<? extends Diff> differences;

	/** Direction of the merge operation. */
	private final boolean leftToRight;

	/**
	 * Constructs an instance of this command given the list of differences that it needs to merge.
	 * 
	 * @param changeRecorder
	 *            The change recorder associated to this command.
	 * @param notifiers
	 *            The collection of notifiers that will be notified of this command's execution.
	 * @param differences
	 *            The list of differences that this command should merge.
	 * @param leftToRight
	 *            The direction in which {@code differences} should be merged.
	 */
	public CopyAllNonConflictingCommand(ChangeRecorder changeRecorder, Collection<Notifier> notifiers,
			List<? extends Diff> differences, boolean leftToRight) {
		super(changeRecorder, notifiers);
		this.differences = differences;
		this.leftToRight = leftToRight;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.common.command.AbstractCommand#canExecute()
	 */
	@Override
	public boolean canExecute() {
		return super.canExecute() && Iterables.any(differences, hasState(DifferenceState.UNRESOLVED));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.edit.command.ChangeCommand#doExecute()
	 */
	@Override
	protected void doExecute() {
		if (leftToRight) {
			for (Diff diff : differences) {
				if (diff.getSource() == DifferenceSource.LEFT && diff.getConflict() == null) {
					diff.copyLeftToRight();
				}
			}
		} else {
			for (Diff diff : differences) {
				if (diff.getSource() == DifferenceSource.RIGHT && diff.getConflict() == null) {
					diff.copyRightToLeft();
				}
			}
		}
	}
}
