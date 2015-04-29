package edu.usc.ict.nl.dm.fsm;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCInstance;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.apache.commons.scxml.model.Action;
import org.apache.commons.scxml.model.ModelException;
import org.apache.commons.scxml.model.TransitionTarget;

public class SCXMLDisableState extends Action {
	/** Serial version UID. */
	private static final long serialVersionUID = 1L;
	/** This is who we say hello to. */
	private String name;

	/** Public constructor is needed for the I in SCXML IO. */
	public SCXMLDisableState() {
		super();
	}

	/**
	 * Get the name.
	 *
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void execute(final EventDispatcher evtDispatcher,
            final ErrorReporter errRep, final SCInstance scInstance,
            final Log appLog, final Collection derivedEvents) throws ModelException, SCXMLExpressionException {
		SCXMLExecutor exe = scInstance.getExecutor();
        TransitionTarget parentTarget = getParentTransitionTarget();
        Context ctx = scInstance.getContext(parentTarget);
        ctx.setLocal(getNamespacesKey(), getNamespaces());
        Evaluator eval = scInstance.getEvaluator();
		exe.disableStateNamed((String) eval.eval(ctx, name));
	}

}
