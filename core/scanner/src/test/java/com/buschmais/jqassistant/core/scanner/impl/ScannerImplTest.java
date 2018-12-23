package com.buschmais.jqassistant.core.scanner.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerConfiguration;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class ScannerImplTest {

    @Mock
    private ScannerContext context;

    @Mock
    private Store store;

    @Mock
    private ScannerPlugin<String, ?> scannerPlugin;

    @Mock
    private Scope scope;

    private Map<String, ScannerPlugin<?, ?>> plugins = new HashMap<>();

    private boolean transaction = false;

    private ScannerConfiguration configuration = new ScannerConfiguration();

    @BeforeEach
    public void setup() throws IOException {
        // Plugin
        doReturn(String.class).when(scannerPlugin).getType();
        when(scannerPlugin.accepts(anyString(), anyString(), eq(scope))).thenReturn(true);
        doAnswer(invocation -> {
            assertThat(transaction, equalTo(true));
            return mock(Descriptor.class);
        }).when(scannerPlugin).scan(anyString(), anyString(), any(Scope.class), any(Scanner.class));
        plugins.put("testPlugin", scannerPlugin);
        // Store
        doReturn(store).when(context).getStore();
        doAnswer(invocation -> transaction).when(store).hasActiveTransaction();
        doAnswer(invocation -> {
            transaction = true;
            return null;
        }).when(store).beginTransaction();
        doAnswer(invocation -> {
            transaction = false;
            return null;
        }).when(store).commitTransaction();
        doAnswer(invocation -> {
            transaction = false;
            return null;
        }).when(store).rollbackTransaction();
    }

    @Test
    public void acceptReturnTrueIfPluginAcceptsResource() throws IOException {
        Properties resource = mock(Properties.class);
        String path = "/a/b/c.properties";
        ScannerPlugin<Properties, ?> selectedPlugin = mock(ScannerPlugin.class);
        doReturn(Boolean.TRUE).when(selectedPlugin).accepts(Mockito.<Properties> anyObject(), Mockito.eq(path), Mockito.eq(scope));
        ScannerImpl scanner = new ScannerImpl(configuration, context, plugins, emptyMap());

        boolean result = scanner.accepts(selectedPlugin, resource, path, scope);

        assertThat(result, is(true));
    }

    @Test
    public void acceptReturnFalseIfPluginRefusesResource() throws IOException {
        Properties resource = mock(Properties.class);
        String path = "/a/b/c.properties";
        ScannerPlugin<Properties, ?> selectedPlugin = mock(ScannerPlugin.class);
        doReturn(Boolean.FALSE).when(selectedPlugin).accepts(Mockito.anyObject(), Mockito.eq(path), Mockito.eq(scope));
        ScannerImpl scanner = new ScannerImpl(configuration, context, plugins, emptyMap());

        boolean result = scanner.accepts(selectedPlugin, resource, path, scope);

        assertThat(result, is(false));
    }

    @Test
    public void failOnError() throws IOException {
        Scanner scanner = new ScannerImpl(configuration, context, plugins, emptyMap());
        stubExceptionDuringScan(scanner);
        try {
            scanner.scan("test", "test", scope);
            fail("Expecting an " + UnrecoverableScannerException.class.getName());
        } catch (UnrecoverableScannerException e) {
            String message = e.getMessage();
            assertThat(message, containsString("test"));
        }

        verify(store).beginTransaction();
        verify(store).rollbackTransaction();
        verify(store, never()).commitTransaction();
        assertThat(transaction, equalTo(false));
    }

    @Test
    public void continueOnError() throws IOException {
        Scanner scanner = new ScannerImpl(configuration, context, plugins, emptyMap());
        stubExceptionDuringScan(scanner);
        configuration.setContinueOnError(true);

        scanner.scan("test", "test", scope);
        scanner.scan("test", "test", scope);

        verify(store, times(2)).beginTransaction();
        verify(store, times(2)).rollbackTransaction();
        verify(store, never()).commitTransaction();
    }

    private void stubExceptionDuringScan(Scanner scanner) throws IOException {
        doAnswer(invocation -> {
            assertThat(transaction, equalTo(true));
            throw new IllegalStateException("Exception in plugin");
        }).when(scannerPlugin).scan("test", "test", scope, scanner);
    }

    @Test
    public void continueOnErrorDuringCommit() {
        doThrow(new IllegalStateException("Exception during commit")).when(store).commitTransaction();
        configuration.setContinueOnError(true);
        Scanner scanner = new ScannerImpl(configuration, context, plugins, emptyMap());

        scanner.scan("test1", "test1", scope);
        scanner.scan("test2", "test2", scope);

        verify(store, times(2)).beginTransaction();
        verify(store, times(2)).commitTransaction();
        verify(store, times(2)).rollbackTransaction();
    }
}
