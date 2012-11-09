package org.eclipse.emf.compare.diagram.ide.ui.internal.accessor;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.diagram.DiagramDiff;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.accessor.IEObjectAccessor;
import org.eclipse.gmf.runtime.notation.Diagram;

public interface IDiagramDiffAccessor extends IDiagramNodeAccessor, IEObjectAccessor {

	Comparison getComparison();

	Diagram getDiagram();

	DiagramDiff getDiff();

}