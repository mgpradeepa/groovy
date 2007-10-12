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

package groovy.swing.factory

import java.awt.Component
import java.awt.Window
import javax.swing.JTabbedPane

class ComponentFactory extends BeanFactory {

    public ComponentFactory(Class beanClass) {
        super(beanClass)
    }

    public ComponentFactory(Class beanClass, boolean leaf) {
        super(beanClass, leaf)
    }

    public void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (!(child instanceof Component) || (child instanceof Window)) {
            return;
        }
        try {
            def constraints = builder.constraints
            if (constraints != null) {
                parent.add(child, constraints)
            } else {
                parent.add(child)
            }
        } catch (MissingPropertyException mpe) {
            parent.add(child)
        }
    }
}