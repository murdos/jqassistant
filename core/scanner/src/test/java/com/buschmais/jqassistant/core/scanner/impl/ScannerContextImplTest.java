package com.buschmais.jqassistant.core.scanner.impl;

import com.buschmais.jqassistant.core.store.api.Store;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Verifies the functionality of {@link ScannerContextImpl}.
 */
@ExtendWith(MockitoExtension.class)
public class ScannerContextImplTest {

    @Mock
    private Store store;

    @Test
    public void peekNonExistingValue() {
        ScannerContextImpl scannerContext = new ScannerContextImpl(store);

        Assertions.assertThatThrownBy(() -> scannerContext.peek(String.class))
                  .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void peekExistingValues() {
        ScannerContextImpl scannerContext = new ScannerContextImpl(store);
        scannerContext.push(String.class, "Foo");
        assertThat(scannerContext.peek(String.class), equalTo("Foo"));
        scannerContext.push(String.class, "Bar");
        assertThat(scannerContext.peek(String.class), equalTo("Bar"));
        assertThat(scannerContext.pop(String.class), equalTo("Bar"));
        assertThat(scannerContext.peek(String.class), equalTo("Foo"));
        assertThat(scannerContext.pop(String.class), equalTo("Foo"));
        try {
            scannerContext.peek(String.class);
            fail("Expecting an " + IllegalStateException.class.getName());
        } catch (IllegalStateException e) {
            return;
        }
    }

    @Test
    public void peekDefaultValues() {
        ScannerContextImpl scannerContext = new ScannerContextImpl(store);
        assertThat(scannerContext.peekOrDefault(String.class, "Bar"), equalTo("Bar"));
        scannerContext.push(String.class, "Foo");
        assertThat(scannerContext.peekOrDefault(String.class, "Bar"), equalTo("Foo"));
        assertThat(scannerContext.pop(String.class), equalTo("Foo"));
        assertThat(scannerContext.peekOrDefault(String.class, "Bar"), equalTo("Bar"));
    }
}
