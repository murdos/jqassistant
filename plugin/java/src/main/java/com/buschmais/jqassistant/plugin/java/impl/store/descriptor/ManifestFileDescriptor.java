package com.buschmais.jqassistant.plugin.java.impl.store.descriptor;

import static com.buschmais.xo.neo4j.api.annotation.Relation.Outgoing;

import java.util.List;

import com.buschmais.jqassistant.core.store.api.descriptor.FileDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Manifest")
public interface ManifestFileDescriptor extends FileDescriptor {

    @Relation("DECLARES")
    @Outgoing
    ManifestSectionDescriptor getMainSection();

    void setMainSection(ManifestSectionDescriptor mainSection);

    @Relation("DECLARES")
    @Outgoing
    List<ManifestSectionDescriptor> getManifestSections();
}
