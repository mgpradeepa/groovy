/*
 * Copyright 2003-2009 the original author or authors.
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
package org.codehaus.groovy.classgen.asm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class MopWriter {
    
    private static class MopKey {
        int hash = 0;
        String name;
        Parameter[] params;

        MopKey(String name, Parameter[] params) {
            this.name = name;
            this.params = params;
            hash = name.hashCode() << 2 + params.length;
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            MopKey other = (MopKey) obj;
            return other.name.equals(name) && equalParameterTypes(other.params,params);
        }
    }
    
    private WriterController controller;
    
    public MopWriter(WriterController wc) {
        controller = wc;
    }
    
    public void createMopMethods() {
        ClassNode classNode = controller.getClassNode();
        if (classNode.declaresInterface(ClassHelper.GENERATED_CLOSURE_Type)) {
            return;
        }
        visitMopMethodList(classNode.getMethods(), true);
        visitMopMethodList(classNode.getSuperClass().getAllDeclaredMethods(), false);
    }
    
    /**
     * filters a list of method for MOP methods. For all methods that are no
     * MOP methods a MOP method is created if the method is not public and the
     * call would be a call on "this" (isThis == true). If the call is not on
     * "this", then the call is a call on "super" and all methods are used,
     * unless they are already a MOP method
     *
     * @param methods unfiltered list of methods for MOP
     * @param isThis  if true, then we are creating a MOP method on "this", "super" else
     * @see #generateMopCalls(LinkedList, boolean)
     */
    private void visitMopMethodList(List methods, boolean isThis) {
        HashMap<MopKey, MethodNode> mops = new HashMap<MopKey, MethodNode>();
        LinkedList<MethodNode> mopCalls = new LinkedList<MethodNode>();
        for (Object method : methods) {
            MethodNode mn = (MethodNode) method;
            if ((mn.getModifiers() & ACC_ABSTRACT) != 0) continue;
            if (mn.isStatic()) continue;
            // no this$ methods for protected/public isThis=true
            // super$ method for protected/public isThis=false
            // --> results in XOR
            if (isThis ^ (mn.getModifiers() & (ACC_PUBLIC | ACC_PROTECTED)) == 0) continue;
            String methodName = mn.getName();
            if (isMopMethod(methodName)) {
                mops.put(new MopKey(methodName, mn.getParameters()), mn);
                continue;
            }
            if (methodName.startsWith("<")) continue;
            String name = getMopMethodName(mn, isThis);
            MopKey key = new MopKey(name, mn.getParameters());
            if (mops.containsKey(key)) continue;
            mops.put(key, mn);
            mopCalls.add(mn);
        }
        generateMopCalls(mopCalls, isThis);
        mopCalls.clear();
        mops.clear();
    }

    /**
     * creates a MOP method name from a method
     *
     * @param method  the method to be called by the mop method
     * @param useThis if true, then it is a call on "this", "super" else
     * @return the mop method name
     */
    public static String getMopMethodName(MethodNode method, boolean useThis) {
        ClassNode declaringNode = method.getDeclaringClass();
        int distance = 0;
        for (; declaringNode != null; declaringNode = declaringNode.getSuperClass()) {
            distance++;
        }
        return (useThis ? "this" : "super") + "$" + distance + "$" + method.getName();
    }

    /**
     * method to determine if a method is a MOP method. This is done by the
     * method name. If the name starts with "this$" or "super$" but does not 
     * contain "$dist$", then it is an MOP method
     *
     * @param methodName name of the method to test
     * @return true if the method is a MOP method
     */
    public static boolean isMopMethod(String methodName) {
        return (methodName.startsWith("this$") ||
                methodName.startsWith("super$")) && !methodName.contains("$dist$");
    }

    /**
     * generates a Meta Object Protocol method, that is used to call a non public
     * method, or to make a call to super.
     *
     * @param mopCalls list of methods a mop call method should be generated for
     * @param useThis  true if "this" should be used for the naming
     */
    private void generateMopCalls(LinkedList<MethodNode> mopCalls, boolean useThis) {
        for (MethodNode method : mopCalls) {
            String name = getMopMethodName(method, useThis);
            Parameter[] parameters = method.getParameters();
            String methodDescriptor = BytecodeHelper.getMethodDescriptor(method.getReturnType(), method.getParameters());
            MethodVisitor mv = controller.getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, name, methodDescriptor, null, null);
            controller.setMethodVisitor(mv);
            mv.visitVarInsn(ALOAD, 0);
            int newRegister = 1;
            OperandStack operandStack = controller.getOperandStack();
            for (Parameter parameter : parameters) {
                ClassNode type = parameter.getType();
                operandStack.load(parameter.getType(), newRegister);
                // increment to next register, double/long are using two places
                newRegister++;
                if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE) newRegister++;
            }
            operandStack.remove(parameters.length);
            mv.visitMethodInsn(INVOKESPECIAL, BytecodeHelper.getClassInternalName(method.getDeclaringClass()), method.getName(), methodDescriptor);
            BytecodeHelper.doReturn(mv, method.getReturnType());
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            controller.getClassNode().addMethod(name, ACC_PUBLIC | ACC_SYNTHETIC, method.getReturnType(), parameters, null, null);
        }
    }

    private static boolean equalParameterTypes(Parameter[] p1, Parameter[] p2) {
        if (p1.length!=p2.length) return false;
        for (int i=0; i<p1.length; i++) {
            if (!p1[i].getType().equals(p2[i].getType())) return false;
        }
        return true;
    }

}
