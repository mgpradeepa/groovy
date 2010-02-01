package org.codehaus.groovy.classgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InnerClassVisitor extends ClassCodeVisitorSupport implements Opcodes {

    private final SourceUnit sourceUnit;
    private ClassNode classNode;
    private final static int publicSynthetic = Opcodes.ACC_PUBLIC+Opcodes.ACC_SYNTHETIC;
    private FieldNode thisField = null;
    private MethodNode currentMethod;
    private FieldNode currentField;
    
    public InnerClassVisitor(CompilationUnit cu, SourceUnit su) {
        sourceUnit = su;
    }
    
    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }
    
    @Override
    public void visitClass(ClassNode node) {
        this.classNode = node;
        thisField = null;
        InnerClassNode innerClass=null;
        if (!node.isEnum() && !node.isInterface() &&
             node instanceof InnerClassNode) 
        {
            innerClass = (InnerClassNode) node;
            if (!isStatic(innerClass) && innerClass.getVariableScope()==null) {
                thisField = innerClass.addField("this$0", publicSynthetic, node.getOuterClass(), null);
            }
            
            if (innerClass.getVariableScope()==null && 
                innerClass.getDeclaredConstructors().isEmpty()) 
            {
                // add dummy constructor
                innerClass.addConstructor(publicSynthetic, new Parameter[0], null, null);
            }
        }

        super.visitClass(node);
        
        if (node.isEnum() || node.isInterface()) return;
        addDispatcherMethods();
        if (innerClass==null) return;
        
        if (node.getSuperClass().isInterface()) {
            node.addInterface(node.getUnresolvedSuperClass());
            node.setUnresolvedSuperClass(ClassHelper.OBJECT_TYPE);
        }         
        addDefaultMethods(innerClass);
    }
    
    private boolean isStatic(InnerClassNode node) {
        VariableScope scope = node.getVariableScope(); 
        if (scope!=null) return scope.isInStaticContext(); 
        return (node.getModifiers() & ACC_STATIC)!=0;
    }
    
    private void addDefaultMethods(InnerClassNode node) {
        final boolean isStatic = isStatic(node);
        
        final String classInternalName = BytecodeHelper.getClassInternalName(node);
        final String outerClassInternalName = getInternalName(node.getOuterClass(),isStatic);
        final String outerClassDescriptor = getTypeDescriptor(node.getOuterClass(),isStatic);
        final int objectDistance = getObjectDistance(node.getOuterClass());
        
        // add method dispatcher
        Parameter[] parameters = new Parameter[] {
                new Parameter(ClassHelper.STRING_TYPE, "name"),
                new Parameter(ClassHelper.OBJECT_TYPE, "args")
        };
        MethodNode method = node.addSyntheticMethod(
                "methodMissing", 
                Opcodes.ACC_PUBLIC, 
                ClassHelper.OBJECT_TYPE, 
                parameters, 
                ClassNode.EMPTY_ARRAY, 
                null
        );

        BlockStatement block = new BlockStatement();
        if (isStatic) {
        	setMethodDispatcherCode(block, new ClassExpression(node.getOuterClass()), parameters);
        } else {
	        block.addStatement(
	                new BytecodeSequence(new BytecodeInstruction() {
	                    public void visit(MethodVisitor mv) {
	                        mv.visitVarInsn(ALOAD, 0);
	                        mv.visitFieldInsn(GETFIELD, classInternalName, "this$0", outerClassDescriptor);
	                        mv.visitVarInsn(ALOAD, 1);
	                        mv.visitVarInsn(ALOAD, 2);
	                        mv.visitMethodInsn( INVOKEVIRTUAL, 
	                                            outerClassInternalName, 
	                                            "this$dist$invoke$"+objectDistance, 
	                                            "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
	                        mv.visitInsn(ARETURN);
	                    }
	                })
	        );
        }
        method.setCode(block);
        
        // add property getter dispatcher
        parameters = new Parameter[] {
                new Parameter(ClassHelper.STRING_TYPE, "name"),
                new Parameter(ClassHelper.OBJECT_TYPE, "val")
        };
        method = node.addSyntheticMethod(
                "propertyMissing", 
                Opcodes.ACC_PUBLIC, 
                ClassHelper.VOID_TYPE,
                parameters, 
                ClassNode.EMPTY_ARRAY, 
                null
        );
        
        block = new BlockStatement();
        if (isStatic) {
        	setPropertySetDispatcher(block, new ClassExpression(node.getOuterClass()), parameters);
	    } else {
	        block.addStatement(
	                new BytecodeSequence(new BytecodeInstruction() {
	                    public void visit(MethodVisitor mv) {
	                        mv.visitVarInsn(ALOAD, 0);
	                        mv.visitFieldInsn(GETFIELD, classInternalName, "this$0", outerClassDescriptor);
	                        mv.visitVarInsn(ALOAD, 1);
	                        mv.visitVarInsn(ALOAD, 2);
	                        mv.visitMethodInsn( INVOKEVIRTUAL, 
	                                            outerClassInternalName, 
	                                            "this$dist$set$"+objectDistance,
	                                            "(Ljava/lang/String;Ljava/lang/Object;)V");
	                        mv.visitInsn(RETURN);
	                    }
	                })
	        );
	    }
        method.setCode(block);
        
        // add property setter dispatcher
        parameters = new Parameter[] {
                new Parameter(ClassHelper.STRING_TYPE, "name")
        };
        method = node.addSyntheticMethod(
                "propertyMissing", 
                Opcodes.ACC_PUBLIC, 
                ClassHelper.OBJECT_TYPE, 
                parameters, 
                ClassNode.EMPTY_ARRAY, 
                null
        );
        
        block = new BlockStatement();
	    if (isStatic) {
	    	setPropertyGetterDispatcher(block, new ClassExpression(node.getOuterClass()), parameters);
	    } else {
	        block.addStatement(
	                new BytecodeSequence(new BytecodeInstruction() {
	                    public void visit(MethodVisitor mv) {
	                        mv.visitVarInsn(ALOAD, 0);
	                        mv.visitFieldInsn(GETFIELD, classInternalName, "this$0", outerClassDescriptor);
	                        mv.visitVarInsn(ALOAD, 1);
	                        mv.visitMethodInsn( INVOKEVIRTUAL, 
	                                            outerClassInternalName, 
	                                            "this$dist$get$"+objectDistance, 
	                                            "(Ljava/lang/String;)Ljava/lang/Object;");
	                        mv.visitInsn(ARETURN);
	                    }
	                })
	        );
	    }
        method.setCode(block);
    }

    private String getTypeDescriptor(ClassNode node, boolean isStatic) {
    	return BytecodeHelper.getTypeDescription(getClassNode(node,isStatic));
	}

	private String getInternalName(ClassNode node, boolean isStatic) {
    	return BytecodeHelper.getClassInternalName(getClassNode(node,isStatic));
	}
	
	@Override
	public void visitConstructor(ConstructorNode node) {
	    addThisReference(node);
	    super.visitConstructor(node);
	}
	
	private boolean shouldHandleImplicitThisForInnerClass(ClassNode cn) {
	    if (cn.isEnum() || cn.isInterface()) return false;
	    if ((cn.getModifiers() & Opcodes.ACC_STATIC)!=0) return false;

	    if(!(cn instanceof InnerClassNode)) return false;
	    InnerClassNode innerClass = (InnerClassNode) cn;
	    // scope != null means aic, we don't handle that here
	    if (innerClass.getVariableScope()!=null) return false;
	    // static inner classes don't need this$0
	    if ((innerClass.getModifiers() & ACC_STATIC)!=0) return false;
	    
	    return true;
	}
	
	private void addThisReference(ConstructorNode node) {
		if(!shouldHandleImplicitThisForInnerClass(classNode)) return;
	    Statement code = node.getCode();
	    
	    // add "this$0" field init
	    
	    //add this parameter to node
	    Parameter[] params = node.getParameters();
        Parameter[] newParams = new Parameter[params.length+1];
        System.arraycopy(params, 0, newParams, 1, params.length);
        Parameter thisPara = new Parameter(classNode.getOuterClass(),getUniqueName(params,node));
        newParams[0] = thisPara;
        node.setParameters(newParams);

        Statement firstStatement = node.getFirstStatement();

        BlockStatement block = null;
        if (code==null) {
            block = new BlockStatement();
        } else if (!(code instanceof BlockStatement)) {
            block = new BlockStatement();
            block.addStatement(code);
        } else {
            block = (BlockStatement) code;
        }
        BlockStatement newCode = new BlockStatement();
        addFieldInit(thisPara,thisField,newCode);
        ConstructorCallExpression cce = getFirstIfSpecialConstructorCall(block);
        if (cce == null) {
            block.getStatements().add(0, newCode);
        } else if (cce.isThisCall()) {
            // add thisPara to this(...)
            TupleExpression args = (TupleExpression) cce.getArguments();
            List<Expression> expressions = args.getExpressions();
            VariableExpression ve = new VariableExpression(thisPara.getName());
            ve.setAccessedVariable(thisPara);
            expressions.add(0,ve);
            newCode = block;
        } else {
            // we have a call to super here, so we need to add 
            // our code after that
            block.getStatements().add(1, newCode);
        }
        node.setCode(block);
	}
	
	
	private String getUniqueName(Parameter[] params, ConstructorNode node) {
	    String namePrefix = "$p";
	    outer: 
	    for (int i=0; i<100; i++) {
	        namePrefix=namePrefix+"$";
	        for (Parameter p:params) {
	            if (p.getName().equals(namePrefix)) continue outer;
	        }
	        return namePrefix;
	    }
	    addError("unable to find a unique prefix name for synthetic this reference", node);
	    return namePrefix;
	}
	
	
    private ConstructorCallExpression getFirstIfSpecialConstructorCall(BlockStatement code) {
        if (code == null) return null;

        final List<Statement> statementList = code.getStatements();
        if(statementList.isEmpty()) return null;

        final Statement statement = statementList.get(0);
        if (!(statement instanceof ExpressionStatement)) return null;

        Expression expression = ((ExpressionStatement)statement).getExpression();
        if (!(expression instanceof ConstructorCallExpression)) return null;
        ConstructorCallExpression cce = (ConstructorCallExpression) expression;
        if (cce.isSpecialCall()) return cce;
        return null;
    }

    protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
        this.currentMethod = node;
        super.visitConstructorOrMethod(node, isConstructor);
        this.currentMethod = null;
    }

    public void visitField(FieldNode node) {
    	this.currentField = node;
    	super.visitField(node);
    	this.currentField = null;
    }
    
	@Override
    public void visitConstructorCallExpression(ConstructorCallExpression call) {
        super.visitConstructorCallExpression(call);
        if (!call.isUsingAnnonymousInnerClass()) {
        	passThisReference(call);
        	return;
        }
        
        InnerClassNode innerClass = (InnerClassNode) call.getType();
        if (!innerClass.getDeclaredConstructors().isEmpty()) return;
        if ((innerClass.getModifiers() & ACC_STATIC)!=0) return;
        
        VariableScope scope = innerClass.getVariableScope();
        if (scope==null) return;
        
        
        boolean isStatic = scope.isInStaticContext();
        // expressions = constructor call arguments
        List<Expression> expressions = ((TupleExpression) call.getArguments()).getExpressions();
        // block = init code for the constructor we produce
        BlockStatement block = new BlockStatement();
        // parameters = parameters of the constructor
        List parameters = new ArrayList(expressions.size()+1+scope.getReferencedLocalVariablesCount());
        // superCallArguments = arguments for the super call == the constructor call arguments
        List superCallArguments = new ArrayList(expressions.size());
        
        // first we add a super() call for all expressions given in the 
        // constructor call expression
        int pCount = 0;
        for (Expression expr : expressions) {
            pCount++;
            // add one parameter for each expression in the 
            // constructor call
            Parameter param = new Parameter(ClassHelper.OBJECT_TYPE,"p"+pCount);
            parameters.add(param);
            // add to super call
            superCallArguments.add(new VariableExpression(param));
        }
        
        // add the super call
        ConstructorCallExpression cce = new ConstructorCallExpression(
                ClassNode.SUPER,
                new TupleExpression(superCallArguments)
        );
        block.addStatement(new ExpressionStatement(cce));
        
        // we need to add "this" to access unknown methods/properties
        // this is saved in a field named this$0
        expressions.add(VariableExpression.THIS_EXPRESSION);
        pCount++;
        ClassNode outerClassType = getClassNode(innerClass.getOuterClass(),isStatic);
        Parameter thisParameter = new Parameter(outerClassType,"p"+pCount);
        parameters.add(thisParameter);
        
        thisField = innerClass.addField("this$0", publicSynthetic, outerClassType, null);
        addFieldInit(thisParameter,thisField,block);

        // for each shared variable we add a reference and save it as field
        for (Iterator it=scope.getReferencedLocalVariablesIterator(); it.hasNext();) {
            pCount++;
            org.codehaus.groovy.ast.Variable var = (org.codehaus.groovy.ast.Variable) it.next();
            VariableExpression ve = new VariableExpression(var);
            ve.setClosureSharedVariable(true);
            ve.setUseReferenceDirectly(true);
            expressions.add(ve);

            Parameter p = new Parameter(ClassHelper.REFERENCE_TYPE,"p"+pCount);
            parameters.add(p);
            final VariableExpression initial = new VariableExpression(p);
            initial.setUseReferenceDirectly(true);
            final FieldNode pField = innerClass.addFieldFirst(ve.getName(), publicSynthetic, ClassHelper.REFERENCE_TYPE, initial);
            final int finalPCount = pCount;
            pField.setHolder(true);
        }
        
        innerClass.addConstructor(ACC_PUBLIC, (Parameter[]) parameters.toArray(new Parameter[0]), ClassNode.EMPTY_ARRAY, block);
        
    }
    
    // this is the counterpart of addThisReference(). To non-static inner classes, outer this should be
	// passed as the first argument implicitly.
	private void passThisReference(ConstructorCallExpression call) {
		ClassNode cn = call.getType().redirect();
		if(!shouldHandleImplicitThisForInnerClass(cn)) return;
		boolean isInStaticContext = true;
		if(currentMethod != null)
			isInStaticContext = currentMethod.getVariableScope().isInStaticContext();
		else if(currentField != null)
			isInStaticContext = currentField.isStatic();
		
		// if constructor call is not in static context, return
        if(isInStaticContext) return;

		// if constructor call is not in outer class, don't pass 'this' implicitly. Return.
        if(classNode != ((InnerClassNode) cn).getOuterClass()) return;
        
        //add this parameter to node
        Expression argsExp = call.getArguments();
        if(argsExp instanceof TupleExpression) {
        	TupleExpression argsListExp = (TupleExpression) argsExp;
        	argsListExp.getExpressions().add(0, VariableExpression.THIS_EXPRESSION);
        }
	}
    
    private ClassNode getClassNode(ClassNode node, boolean isStatic) {
    	if (isStatic) node = ClassHelper.CLASS_Type;
    	return node;
	}

	private void addDispatcherMethods() {
        final int objectDistance = getObjectDistance(classNode);
        
        // since we added an anonymous inner class we should also
        // add the dispatcher methods
        
        // add method dispatcher
        Parameter[] parameters = new Parameter[] {
                new Parameter(ClassHelper.STRING_TYPE, "name"),
                new Parameter(ClassHelper.OBJECT_TYPE, "args")
        };
        MethodNode method = classNode.addSyntheticMethod(
                "this$dist$invoke$"+objectDistance, 
                ACC_PUBLIC+ACC_SYNTHETIC, 
                ClassHelper.OBJECT_TYPE, 
                parameters, 
                ClassNode.EMPTY_ARRAY, 
                null
        );

        BlockStatement block = new BlockStatement();
        setMethodDispatcherCode(block, VariableExpression.THIS_EXPRESSION, parameters);
        method.setCode(block);
        
        // add property setter
        parameters = new Parameter[] {
                new Parameter(ClassHelper.STRING_TYPE, "name"),
                new Parameter(ClassHelper.OBJECT_TYPE, "value")
        };
        method = classNode.addSyntheticMethod(
                "this$dist$set$"+objectDistance, 
                ACC_PUBLIC+ACC_SYNTHETIC, 
                ClassHelper.VOID_TYPE, 
                parameters, 
                ClassNode.EMPTY_ARRAY, 
                null
        );

        block = new BlockStatement();
        setPropertySetDispatcher(block,VariableExpression.THIS_EXPRESSION,parameters);
        method.setCode(block);

        // add property getter
        parameters = new Parameter[] {
                new Parameter(ClassHelper.STRING_TYPE, "name")
        };
        method = classNode.addSyntheticMethod(
                "this$dist$get$"+objectDistance, 
                ACC_PUBLIC+ACC_SYNTHETIC, 
                ClassHelper.OBJECT_TYPE, 
                parameters, 
                ClassNode.EMPTY_ARRAY, 
                null
        );

        block = new BlockStatement();
        setPropertyGetterDispatcher(block, VariableExpression.THIS_EXPRESSION, parameters);
        method.setCode(block);
    }

    private void setPropertyGetterDispatcher(BlockStatement block, Expression thiz, Parameter[] parameters) {
    	List gStringStrings = new ArrayList();
        gStringStrings.add(new ConstantExpression(""));
        gStringStrings.add(new ConstantExpression(""));
        List gStringValues = new ArrayList();
        gStringValues.add(new VariableExpression(parameters[0]));
        block.addStatement(
                new ReturnStatement(
                        new AttributeExpression(
                                thiz,
                                new GStringExpression("$name",
                                        gStringStrings,
                                        gStringValues
                                )
                        )
                )
        );		
	}

	private void setPropertySetDispatcher(BlockStatement block, Expression thiz, Parameter[] parameters) {
    	List gStringStrings = new ArrayList();
        gStringStrings.add(new ConstantExpression(""));
        gStringStrings.add(new ConstantExpression(""));
        List gStringValues = new ArrayList();
        gStringValues.add(new VariableExpression(parameters[0]));
        block.addStatement(
                new ExpressionStatement(
                        new BinaryExpression(
                                new AttributeExpression(
                                        thiz,
                                        new GStringExpression("$name",
                                                gStringStrings,
                                                gStringValues
                                        )
                                ),
                                Token.newSymbol(Types.ASSIGN, -1, -1),
                                new VariableExpression(parameters[1])
                        )
                )
        );
	}

	private void setMethodDispatcherCode(BlockStatement block, Expression thiz, Parameter[] parameters) {
        List gStringStrings = new ArrayList();
        gStringStrings.add(new ConstantExpression(""));
        gStringStrings.add(new ConstantExpression(""));
        List gStringValues = new ArrayList();
        gStringValues.add(new VariableExpression(parameters[0]));
        block.addStatement(
                new ReturnStatement(
                        new MethodCallExpression(
                               thiz,
                               new GStringExpression("$name",
                                       gStringStrings,
                                       gStringValues
                               ),
                               new ArgumentListExpression(
                                       new SpreadExpression(new VariableExpression(parameters[1]))
                               )
                        )
                )
        );
	}

	private static void addFieldInit(Parameter p, FieldNode fn, BlockStatement block) {
        VariableExpression ve = new VariableExpression(p);
        FieldExpression fe = new FieldExpression(fn);
        block.addStatement(new ExpressionStatement(
                new BinaryExpression(
                        fe,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        ve
                )
        ));
    }
    
    private int getObjectDistance(ClassNode node) {
        int count = 1;
        while (node!=null && node!=ClassHelper.OBJECT_TYPE) {
            count++;
            node = node.getSuperClass();
        }
        return count;
    }
    
}
