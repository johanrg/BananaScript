package com.github.johanrg.frontend;

import com.github.johanrg.ast.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author johan
 * @since 2016-07-08.
 */
public class Identifiers {
    private final Stack<Map<String, Identifier>> scopeStack = new Stack<>();
    private Map<String, Identifier> currentScope = null;

    public void newScope() {
        if (currentScope != null) {
            scopeStack.push(currentScope);
        }
        currentScope = new HashMap<>();
    }

    public Map<String, Identifier> popScope() {
        if (scopeStack.isEmpty()) {
            Map<String, Identifier> scope = currentScope;
            currentScope = null;
            return scope;
        } else {
            Map<String, Identifier> scope = currentScope;
            currentScope = scopeStack.pop();
            return scope;
        }
    }

    public boolean addIdentifier(Identifier identifier) {
        if (currentScope.get(identifier.getIdentifier()) == null) {
            currentScope.put(identifier.getIdentifier(), identifier);
            return true;
        } else {
           return false;
        }
    }

    public Identifier find(String name) {
        Identifier identifier = null;

        if (currentScope != null) {
            identifier = currentScope.get(name);
            if (identifier == null) {
                // Note(Johan): Java goes from oldest in stack to newest for som strange reason, so
                // go through it all to find the latest.
                for (Map<String, Identifier> map : scopeStack) {
                    Identifier find = map.get(name);
                    if (find != null) {
                        identifier = find;
                    }
                }
            }
        }
        return identifier;
    }
}
