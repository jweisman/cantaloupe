package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.script.DelegateProxy;

import javax.script.ScriptException;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides information about redactions.
 */
public final class RedactionService {

    /**
     * @return Whether {@link Key#REDACTION_ENABLED} is true.
     */
    public boolean isEnabled() {
        return Configuration.getInstance().
                getBoolean(Key.REDACTION_ENABLED, false);
    }

    /**
     * Factory method that returns a list of {@link Redaction redactions}
     * based on the given parameters.
     *
     * @param proxy Delegate proxy for the current request.
     * @return      Redactions from the given proxy, or an empty list if there
     *              are none.
     */
    public List<Redaction> redactionsFor(DelegateProxy proxy)
            throws ScriptException {
        return proxy.getRedactions().stream()
                .map(def -> new Redaction(
                        new Rectangle(def.get("x").intValue(),
                                def.get("y").intValue(),
                                def.get("width").intValue(),
                                def.get("height").intValue())))
                .collect(Collectors.toList());
    }

}
