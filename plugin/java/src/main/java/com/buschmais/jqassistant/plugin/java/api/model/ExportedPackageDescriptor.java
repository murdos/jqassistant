package com.buschmais.jqassistant.plugin.java.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("ExportedPackage")
public interface ExportedPackageDescriptor extends JavaDescriptor, PackageToModuleDescriptor {

    @Override
    @Relation("EXPORTED_PACKAGE")
    PackageDescriptor getPackage();

}
