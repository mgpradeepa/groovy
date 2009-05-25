/*
 * Copyright 2008 the original author or authors.
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

package org.codehaus.groovy.transform.powerassert

import org.codehaus.groovy.transform.powerassert.PowerAssertionError

import junit.framework.Assert

/**
 * Utility methods for testing power assertions.
 *
 * @author Peter Niederwieser
 */

abstract class AssertionTestUtil {
    static fails(Closure assertion) {
        try {
            assertion.call();
            Assert.fail("assertion should have failed but didn't")
        } catch (PowerAssertionError expected) {}
    }

    static isNotTransformed(Closure failingAssertion) {
        try {
            failingAssertion()
            throw new RuntimeException("assertion should have failed but didn't")
        } catch (AssertionError expected) {
            if (expected instanceof org.codehaus.groovy.transform.powerassert.PowerAssertionError)
                throw new RuntimeException("assertion shouldn't have been but was transformed")
        }
    }

    static isRendered(String expectedRendering, Closure failingAssertion) {
        try {
            failingAssertion.call();
            Assert.fail("assertion should have failed but didn't")
        } catch (PowerAssertionError e) {
            Assert.assertEquals(expectedRendering.trim(), e.message.trim())
        }
    }
}
