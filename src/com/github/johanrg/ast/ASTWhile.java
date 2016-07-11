package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author Johan Gustafsson
 * @since 7/11/2016.
 */
public class ASTWhile extends ASTNode {
   private final ASTNode expression;
   private final ASTCompoundStatement whileScope;

   public ASTWhile(ASTNode expression, ASTCompoundStatement whileScope, Location location) {
      super(location);
      this.expression = expression;
      this.whileScope = whileScope;
   }

   public ASTNode getExpression() {
      return expression;
   }

   public ASTCompoundStatement getWhileScope() {
      return whileScope;
   }
}
