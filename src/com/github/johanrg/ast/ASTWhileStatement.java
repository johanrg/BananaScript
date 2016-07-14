package com.github.johanrg.ast;

import com.github.johanrg.frontend.Location;

/**
 * @author Johan Gustafsson
 * @since 7/11/2016.
 */
public class ASTWhileStatement extends ASTNode {
   private final ASTNode expression;
   private final ASTScope whileScope;

   public ASTWhileStatement(ASTNode expression, ASTScope whileScope, Location location) {
      super(location);
      this.expression = expression;
      this.whileScope = whileScope;
   }

   public ASTNode getExpression() {
      return expression;
   }

   public ASTScope getWhileScope() {
      return whileScope;
   }
}
