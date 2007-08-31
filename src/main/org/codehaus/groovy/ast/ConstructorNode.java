/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.ast;

import java.util.Map;

import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.*;


/**
 * Represents a constructor declaration
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class ConstructorNode extends MethodNode {
    
    public ConstructorNode(int modifiers, Statement code) {
        this(modifiers, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code);
    }
    
    public ConstructorNode(int modifiers, Parameter[] parameters, ClassNode[] exceptions, Statement code) {
        super("<init>",modifiers,ClassHelper.VOID_TYPE,parameters,exceptions,code);
        
        VariableScope scope = new VariableScope();
        Map declares = scope.getDeclaredVariables(); 
        for (int i = 0; i < parameters.length; i++) {
            declares.put(parameters[i].getName(),parameters[i]);
        }
        this.setVariableScope(scope);
    }
    
    public boolean firstStatementIsSpecialConstructorCall() {
        Statement code = getFirstStatement();
        if (code == null || !(code instanceof ExpressionStatement)) return false;

        Expression expression = ((ExpressionStatement) code).getExpression();
        if (!(expression instanceof ConstructorCallExpression)) return false;
        ConstructorCallExpression cce = (ConstructorCallExpression) expression;
        return cce.isSpecialCall();
    }

}
