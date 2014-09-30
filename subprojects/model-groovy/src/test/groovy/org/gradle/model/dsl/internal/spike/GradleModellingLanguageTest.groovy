/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.dsl.internal.spike

import org.gradle.internal.Factories
import org.gradle.model.dsl.internal.spike.fixture.GradleModellingLanguageCompilingTestClassLoader
import org.gradle.model.internal.core.ModelPath
import spock.lang.Specification

class GradleModellingLanguageTest extends Specification {

    ModelRegistry registry = new ModelRegistry()

    void buildScript(String script) {
        Class<Script> scriptClass = new GradleModellingLanguageCompilingTestClassLoader().parseClass(script)
        Script scriptInstance = scriptClass.newInstance()
        scriptInstance.binding.setVariable("modelRegistryHelper", new ModelRegistryDslHelper(registry))
        scriptInstance.run()
    }

    def getModelValueAt(String path) {
        registry.get(ModelPath.path(path))
    }

    void "simple top level assignment"() {
        when:
        buildScript """
            model {
                foo << { 2 }
            }
        """

        then:
        getModelValueAt("foo") == 2
    }

    void "simple top level assignment using sugared syntax"() {
        when:
        buildScript """
            model {
                foo = 2
                bar = 1 + 2
            }
        """

        then:
        getModelValueAt("foo") == 2
        getModelValueAt("bar") == 3
    }

    void "scoped assignments"() {
        given:
        registry.create(ModelPath.path("person"), Factories.constant(new Person()))

        when:
        buildScript """
            model {
                person {
                    firstName = "foo"
                    lastName = "bar"
                }
            }
        """

        then:
        def person = getModelValueAt("person")
        person.firstName == "foo"
        person.lastName == "bar"
    }

    void "multipart path assignment"() {
        given:
        registry.create(ModelPath.path("book"), Factories.constant(new Book()))
        registry.create(ModelPath.path("book.author"), Factories.constant(new Person()))

        when:
        buildScript """
            model {
                book {
                    author {
                        firstName = "foo"
                    }
                    author.lastName = "bar"
                }
            }
        """

        then:
        def book = getModelValueAt("book")
        book.author.firstName == "foo"
        book.author.lastName == "bar"
    }
}

class Person {
    String firstName
    String lastName
}

class Book {
    String title
    Person author
}