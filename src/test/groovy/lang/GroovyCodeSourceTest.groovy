/*
 * $Id$version
 * Nov 23, 2003 9:02:55 PM $user Exp $
 *
 * Copyright 2003 (C) Sam Pullara. All Rights Reserved.
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain
 * copyright statements and notices. Redistributions must also contain a copy
 * of this document. 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the distribution. 3.
 * The name "groovy" must not be used to endorse or promote products derived
 * from this Software without prior written permission of The Codehaus. For
 * written permission, please contact info@codehaus.org. 4. Products derived
 * from this Software may not be called "groovy" nor may "groovy" appear in
 * their names without prior written permission of The Codehaus. "groovy" is a
 * registered trademark of The Codehaus. 5. Due credit should be given to The
 * Codehaus - http://groovy.codehaus.org/
 *
 * THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 */
package groovy.lang

/**
 * @author Merlyn Albery-Speyer
 */
class GroovyCodeSourceTest extends GroovyTestCase {
    void testValidEncoding() {
    	new GroovyCodeSource(createTemporaryGroovyClassFile(), "UTF-8")
	}

    void testInvalidEncoding() {
		try {
		    new GroovyCodeSource(createTemporaryGroovyClassFile(), "non-existant encoding")
		    fail("expected exception")
		} catch (UnsupportedEncodingException e) {
	        assert "non-existant encoding" == e.getMessage()
	    }
    }

	void testInvalidFile() {
		try {
	        new GroovyCodeSource(new File("SomeFileThatDoesNotExist"+System.currentTimeMillis()), "UTF-8")
	        fail("expected IOException")
	    } catch (IOException) {
	    	assert true
	    }
	}

	void testRuntimeException() {
		try {
			new GroovyCodeSource(null, "UTF-8")
			fail("expected NullPointerException")
		} catch (NullPointerException) {
		    assert true
		}
	}

    File createTemporaryGroovyClassFile() {
        String testName = "GroovyCodeSourceTest"+System.currentTimeMillis()
		File groovyCode = new File(System.getProperty("java.io.tmpdir"), testName)
		groovyCode.write("class SomeClass { }")
		groovyCode.deleteOnExit()
		return groovyCode
	}
}
