package com.buschmais.jqassistant.plugin.java.test.scanner;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.constructor.ImplicitDefaultConstructor;
import com.buschmais.jqassistant.plugin.java.test.set.scanner.constructor.OverloadedConstructor;
import org.junit.Test;

import java.io.IOException;

import static com.buschmais.jqassistant.plugin.java.test.matcher.MethodDescriptorMatcher.constructorDescriptor;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * Contains test which verify correct scanning of constructors.
 */
public class ConstructorIT extends AbstractPluginIT {

    /**
     * Verifies scanning of {@link ImplicitDefaultConstructor}.
     *
     * @throws java.io.IOException   If the test fails.
     * @throws NoSuchMethodException If the test fails.
     */
    @Test
    public void implicitDefaultConstructor() throws IOException, NoSuchMethodException {
        scanClasses(ImplicitDefaultConstructor.class);
        assertThat(query("MATCH (c:METHOD:CONSTRUCTOR) RETURN c").getColumn("c"), hasItem(constructorDescriptor(ImplicitDefaultConstructor.class)));
    }

    /**
     * Verifies scanning of {@link OverloadedConstructor}.
     *
     * @throws java.io.IOException   If the test fails.
     * @throws NoSuchMethodException If the test fails.
     */

    @Test
    public void overloadedConstructors() throws IOException, NoSuchMethodException {
        scanClasses(OverloadedConstructor.class);
        assertThat(query("MATCH (c:METHOD:CONSTRUCTOR) RETURN c").getColumn("c"), allOf(hasItem(constructorDescriptor(OverloadedConstructor.class)), hasItem(constructorDescriptor(OverloadedConstructor.class, String.class))));
    }
}
