/*
 $Id$

 Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "groovy" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Codehaus.  For written permission,
    please contact info@codehaus.org.

 4. Products derived from this Software may not be called "groovy"
    nor may "groovy" appear in their names without prior written
    permission of The Codehaus. "groovy" is a registered
    trademark of The Codehaus.

 5. Due credit should be given to The Codehaus -
    http://groovy.codehaus.org/

 THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package org.codehaus.groovy.classgen;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.syntax.SyntaxException;
import org.objectweb.asm.ClassWriter;

/**
 * A ClassLoader which can load Groovy classes
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class GroovyClassLoader extends ClassLoader {

    private Verifier verifier = new Verifier();
    private Class generatedClass = null;
    private CompilerFacade compiler = new CompilerFacade(this) {
        protected void onClass(ClassWriter classWriter, ClassNode classNode) {
            onClassNode(classWriter, classNode);
        }
    };

    public GroovyClassLoader() {
    }

    public GroovyClassLoader(ClassLoader loader) {
        super(loader);
    }

    /**
     * Loads the given class node returning the implementation Class
     * 
     * @param classNode
     * @return
     */
    public Class defineClass(ClassNode classNode, String file) {
        generatedClass = null;
        compiler.generateClass(new GeneratorContext(), classNode, file);
        return generatedClass;
    }

    /**
     * Parses the given file name into a Java class capable of being run
     * 
     * @param charStream
     * @return the main class defined in the given script
     */
    public Class parseClass(String file) throws SyntaxException, IOException {
        return parseClass(new FileInputStream(file), file);
    }

    /**
     * Parses the given character stream into a Java class capable of being run
     * 
     * @param charStream
     * @return the main class defined in the given script
     */
    public Class parseClass(InputStream in, String file) throws SyntaxException, IOException {
        generatedClass = null;
        compiler.parseClass(in, file);
        return generatedClass;
    }
   
    protected void onClassNode(ClassWriter classWriter, ClassNode classNode) {
         byte[] code = classWriter.toByteArray();

         //System.out.println("About to load class: " + classNode.getName());

         Class theClass = defineClass(classNode.getName(), code, 0, code.length);

         if (generatedClass == null) {
             generatedClass = theClass;
         }
     }
}
