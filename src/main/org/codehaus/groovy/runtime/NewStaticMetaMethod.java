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
package org.codehaus.groovy.runtime;

import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.ParameterTypes;
import org.codehaus.groovy.runtime.metaclass.StdMetaMethod;

import java.util.HashMap;

/**
 * A MetaMethod implementation where the underlying method is really a static
 * helper method on some class.
 *
 * This implementation is used to add new static methods to the JDK writing them as normal
 * static methods with the first parameter being the class on which the method is added.
 *
 * @author Guillaume Laforge
 * @version $Revision$
 */
public class NewStaticMetaMethod extends NewMetaMethod {

    private NewStaticMetaMethod(CachedMethod method) {
        super(method);
    }

    public boolean isStatic() {
        return true;
    }

    public int getModifiers() {
        return super.getModifiers();
    }

    public Object invoke(Object object, Object[] arguments) {
        int size = arguments.length;
        Object[] newArguments = new Object[size + 1];
        System.arraycopy(arguments, 0, newArguments, 1, size);
        newArguments[0] = null;
        return super.invoke(null, newArguments);
    }

    private static HashMap cache = new HashMap();
    public synchronized static NewStaticMetaMethod createNewStaticMetaMethod(CachedMethod element) {
        NewStaticMetaMethod method = (NewStaticMetaMethod) cache.get(element);
        if (method == null) {
            method = new NewStaticMetaMethod(element);
            cache.put(element, method);
        }
        return method;
    }
}
