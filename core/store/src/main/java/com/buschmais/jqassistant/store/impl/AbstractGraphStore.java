package com.buschmais.jqassistant.store.impl;

import com.buschmais.jqassistant.store.api.DescriptorDAO;
import com.buschmais.jqassistant.store.api.Store;
import com.buschmais.jqassistant.store.api.model.*;
import com.buschmais.jqassistant.store.impl.dao.DescriptorAdapterRegistry;
import com.buschmais.jqassistant.store.impl.dao.DescriptorDAOImpl;
import com.buschmais.jqassistant.store.impl.dao.mapper.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;

import java.util.Collections;
import java.util.Map;

import static com.buschmais.jqassistant.store.api.DescriptorDAO.NodeProperty.FQN;

/**
 * Abstract base implementation of a {@link Store}.
 * <p>
 * Provides methods for managing the life cycle of a store, transactions,
 * resolving descriptors and executing CYPHER queries.
 * </p>
 */
public abstract class AbstractGraphStore implements Store {

    /**
     * The {@link GraphDatabaseService} to use.
     */
    protected GraphDatabaseService database;

    /**
     * The registry of {@link DescriptorMapper}s. These are used to resolve
     * required metadata.
     */
    private DescriptorAdapterRegistry adapterRegistry;

    /**
     * The {@link DescriptorDAO} instance to use.
     */
    private DescriptorDAO descriptorDAO;

    @Override
    public void start() {
        database = startDatabase();
        for (DescriptorDAO.CoreLabel label : DescriptorDAO.CoreLabel.values()) {
            database.schema().indexFor(label).on(FQN.name());
        }
        adapterRegistry = new DescriptorAdapterRegistry();
        adapterRegistry.register(new PackageDescriptorMapper());
        adapterRegistry.register(new ClassDescriptorMapper());
        adapterRegistry.register(new MethodDescriptorMapper());
        adapterRegistry.register(new FieldDescriptorMapper());
        descriptorDAO = new DescriptorDAOImpl(adapterRegistry, database);
    }

    @Override
    public void stop() {
        adapterRegistry = null;
        stopDatabase(database);
    }

    public GraphDatabaseAPI getDatabaseAPI() {
        if (database == null) {
            throw new IllegalStateException("Store is not started!.");
        }
        return (GraphDatabaseAPI) database;
    }

    @Override
    public PackageDescriptor createPackageDescriptor(final PackageDescriptor parentPackageDescriptor, final String packageName) {
        return persist(new PackageDescriptor(), new Name(parentPackageDescriptor, '.', packageName));
    }

    @Override
    public PackageDescriptor findPackageDescriptor(String fullQualifiedName) {
        return descriptorDAO.find(PackageDescriptor.class, fullQualifiedName);
    }

    @Override
    public ClassDescriptor createClassDescriptor(final PackageDescriptor packageDescriptor, final String className) {
        return persist(new ClassDescriptor(), new Name(packageDescriptor, '.', className));
    }

    @Override
    public ClassDescriptor findClassDescriptor(String fullQualifiedName) {
        return descriptorDAO.find(ClassDescriptor.class, fullQualifiedName);
    }

    @Override
    public MethodDescriptor createMethodDescriptor(final ClassDescriptor classDescriptor, String methodName) {
        return persist(new MethodDescriptor(), new Name(classDescriptor, '#', methodName));
    }

    @Override
    public FieldDescriptor createFieldDescriptor(final ClassDescriptor classDescriptor, String fieldName) {
        return persist(new FieldDescriptor(), new Name(classDescriptor, '#', fieldName));
    }

    @Override
    public QueryResult executeQuery(String query) {
        return descriptorDAO.executeQuery(query, Collections.<String, Object>emptyMap());
    }

    @Override
    public QueryResult executeQuery(String query, Map<String, Object> parameters) {
        return descriptorDAO.executeQuery(query, parameters);
    }

    @Override
    public void flush() {
        descriptorDAO.flush();
    }

    protected DescriptorAdapterRegistry getAdapterRegistry() {
        return adapterRegistry;
    }

    /**
     * Delegates to the sub class to start the database.
     *
     * @return The {@link GraphDatabaseService} instance to use.
     */
    protected abstract GraphDatabaseService startDatabase();

    /**
     * Delegates to the sub class to stop the database.
     *
     * @param database The used {@link GraphDatabaseService} instance.
     */
    protected abstract void stopDatabase(GraphDatabaseService database);

    private <T extends AbstractDescriptor> T persist(T descriptor, Name name) {
        descriptor.setFullQualifiedName(name.getFullQualifiedName());
        descriptorDAO.persist(descriptor);
        return descriptor;
    }

}