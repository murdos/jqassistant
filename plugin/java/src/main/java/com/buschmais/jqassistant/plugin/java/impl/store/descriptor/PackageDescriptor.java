package com.buschmais.jqassistant.plugin.java.impl.store.descriptor;

import static com.buschmais.jqassistant.plugin.java.impl.store.descriptor.Java.JavaLanguageElement.Package;
import static com.buschmais.xo.api.annotation.ResultOf.Parameter;

import java.util.Set;

import com.buschmais.jqassistant.core.store.api.descriptor.FileDescriptor;
import com.buschmais.jqassistant.core.store.api.descriptor.FullQualifiedNameDescriptor;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

/**
 * Describes a Java package.
 */
@Java(Package)
@Label(value = "PACKAGE", usingIndexedPropertyOf = FullQualifiedNameDescriptor.class)
public interface PackageDescriptor extends PackageMemberDescriptor {

    /**
     * Return the contained descriptors.
     * 
     * @return The contained descriptors.
     */
    @Relation("CONTAINS")
    Set<FileDescriptor> getContains();

    @ResultOf
    @Cypher("match (p),(f) where id(p)={this} and id(f)={file} create unique (p)-[:CONTAINS]->(f)")
    void addContains(@Parameter("file") FileDescriptor file);
}
