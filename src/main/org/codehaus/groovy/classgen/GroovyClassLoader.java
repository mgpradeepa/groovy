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

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.lexer.CharStream;
import org.codehaus.groovy.syntax.lexer.InputStreamCharStream;
import org.codehaus.groovy.syntax.lexer.Lexer;
import org.codehaus.groovy.syntax.lexer.LexerTokenStream;
import org.codehaus.groovy.syntax.parser.ASTBuilder;
import org.codehaus.groovy.syntax.parser.CSTNode;
import org.codehaus.groovy.syntax.parser.Parser;
import org.objectweb.asm.ClassWriter;

/**
 * A ClassLoader which can load Groovy classes
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class GroovyClassLoader extends ClassLoader {

    private ClassWriter classWriter;
    private Verifier verifier = new Verifier();

    public GroovyClassLoader() {
        this.classWriter = new ClassWriter(true);
    }

    /**
     * Loads the given class node returning the implementation Class
     * 
     * @param classNode
     * @return
     */
    public Class defineClass(ClassNode classNode) {
        return defineClass(new GeneratorContext(), classNode);
    }

    /**
     * Parses the given character stream into a Java class capable of being run
     * 
     * @param charStream
     * @return the main class defined in the given script
     */
    public Class parseClass(InputStream in) throws SyntaxException, IOException {
        Class answer = null;
        try {
            answer = parseClass(new InputStreamCharStream(in));
        }
        catch (SyntaxException e) {
            try {
                in.close();
            }
            catch (Exception hide) {
                // ignore
            }
            throw e;
        }
        catch (IOException e) {
            try {
                in.close();
            }
            catch (Exception hide) {
                // ignore
            }
            throw e;
        }
        in.close();
        return answer;
    }
    
    protected Class parseClass(CharStream charStream) throws SyntaxException, IOException {
        Lexer lexer = new Lexer(charStream);
        Parser parser = new Parser(new LexerTokenStream(lexer));
        CSTNode compilationUnit = parser.compilationUnit();

        ASTBuilder astBuilder = new ASTBuilder(this);
        ClassNode[] classNodes = astBuilder.build(compilationUnit);

        GeneratorContext context = new GeneratorContext();
        Class answer = null;
        for (int i = 0; i < classNodes.length; ++i) {
            Class aClass = defineClass(context, classNodes[i]);
            if (i == 0) {
                answer = aClass;
            }
        }
        return answer;
    }

    protected Class defineClass(GeneratorContext context, ClassNode classNode) {
        verifier.visitClass(classNode);
        ClassGenerator visitor = new ClassGenerator(context, classWriter, this, null);
        visitor.visitClass(classNode);

        byte[] code = classWriter.toByteArray();

        return defineClass(classNode.getName(), code, 0, code.length);
    }

}
