package com.buschmais.jqassistant.plugin.yaml2.impl.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.yaml2.api.model.*;
import com.buschmais.jqassistant.plugin.yaml2.impl.scanner.parsing.*;

import org.snakeyaml.engine.v2.events.NodeEvent;

public class GraphGenerator {
    private enum Mode {
        STANDARD(true),
        REFERENCE(false);

        private final boolean inStandardMode;

        Mode(boolean mode) {
            inStandardMode = mode;
        }

        boolean isInReferenceMode() {
            return !inStandardMode;
        }

        boolean isInStandardMode() {
            return inStandardMode;
        }
    }

    private AnchorProcessor anchorProcessor = new AnchorProcessor();
    private Store store;

    public GraphGenerator(Store store) {
        this.store = store;
    }

    public Collection<YMLDocumentDescriptor> generate(StreamNode root) {
        ArrayList<YMLDocumentDescriptor> result = new ArrayList<>();

        root.getDocuments().forEach(documentNode -> {
            Consumer<YMLDescriptor> handler = descriptor -> result.add((YMLDocumentDescriptor) descriptor);

            traverse(documentNode, handler, Mode.STANDARD);
        });

        return result;
    }

    private void traverse(BaseNode<?> node, Consumer<YMLDescriptor> handler, Mode mode) {
        if (node.getClass().isAssignableFrom(DocumentNode.class)) {
            DocumentNode documentNode = (DocumentNode) node;
            YMLDocumentDescriptor documentDescriptor = store.create(YMLDocumentDescriptor.class);
            handler.accept(documentDescriptor);

            documentNode.getSequences().forEach(sequenceNode -> {
                Consumer<YMLDescriptor> newHandler = descriptor -> documentDescriptor.getSequences().add((YMLSequenceDescriptor) descriptor);

                traverse(sequenceNode, newHandler, mode);
            });

            documentNode.getMaps().forEach(mapNode -> {
                Consumer<YMLDescriptor> newHandler = descriptor -> documentDescriptor.getMaps().add((YMLMapDescriptor) descriptor);
                traverse(mapNode, newHandler, mode);
            });

            documentNode.getScalars().forEach(mapNode -> {
                Consumer<YMLDescriptor> newHandler = descriptor -> documentDescriptor.getScalars().add((YMLScalarDescriptor) descriptor);
                traverse(mapNode, newHandler, mode);
            });
        } else if (node.getClass().isAssignableFrom(SequenceNode.class)) {
            SequenceNode sequenceNode = (SequenceNode) node;
            YMLSequenceDescriptor sequenceDescriptor = store.create(YMLSequenceDescriptor.class);
            sequenceNode.getIndex().ifPresent(i -> sequenceDescriptor.setIndex(i));
            handler.accept(sequenceDescriptor);

            Consumer<YMLDescriptor> addItemDescriptor = descriptor -> store.addDescriptorType(descriptor, YMLItemDescriptor.class);

            sequenceNode.getScalars().forEach(scalarNode -> {
                Consumer<YMLDescriptor> newHandler = descriptor -> {
                    sequenceDescriptor.getScalars().add((YMLScalarDescriptor) descriptor);
                    addItemDescriptor.accept(descriptor);
                };

                traverse(scalarNode, newHandler, mode);
            });

            sequenceNode.getMaps().forEach(mapNode -> {
                Consumer<YMLDescriptor> newHandler = descriptor -> {
                    addItemDescriptor.accept(descriptor);
                    sequenceDescriptor.getMaps().add((YMLMapDescriptor) descriptor);
                };
                traverse(mapNode, newHandler, mode);
            });

            sequenceNode.getSequences().forEach(seqNode -> {
                Consumer<YMLDescriptor> newHandler = descriptor -> {
                    addItemDescriptor.accept(descriptor);
                    sequenceDescriptor.getSequences().add((YMLSequenceDescriptor) descriptor);
                };

                traverse(seqNode, newHandler, mode);
            });

            anchorProcessor.process(sequenceNode, sequenceDescriptor, mode);

            sequenceNode.getAliases().forEach(aliasNode -> {
                Consumer<YMLDescriptor> scalarHandler = descriptor -> {
                    YMLScalarDescriptor scalarDescriptor = (YMLScalarDescriptor) descriptor;
                    aliasNode.getIndex().ifPresent(index -> scalarDescriptor.setIndex(index));
                    sequenceDescriptor.getScalars().add(scalarDescriptor);
                    addItemDescriptor.accept(descriptor);
                };
                BaseNode<?> referencedNode = aliasNode.getReferencedNode();
                traverse(referencedNode, scalarHandler, Mode.REFERENCE);
            });

            Comparator<YMLDescriptor> indexComparator = (lhs, rhs) -> {
                Integer lhsIndex = ((YMLIndexable) lhs).getIndex();
                Integer rhsIndex = ((YMLIndexable) rhs).getIndex();
                return Integer.compare(lhsIndex, rhsIndex);
            };
            Optional<? extends YMLDescriptor> first =
                Stream.of(sequenceDescriptor.getScalars(),
                          sequenceDescriptor.getSequences(),
                          sequenceDescriptor.getMaps())
                      .flatMap(Collection::stream)
                      .min(indexComparator);

            Optional<? extends YMLDescriptor> last =
                Stream.of(sequenceDescriptor.getScalars(),
                          sequenceDescriptor.getSequences(),
                          sequenceDescriptor.getMaps())
                      .flatMap(Collection::stream)
                      .max(indexComparator);

            last.ifPresent(descriptor -> store.addDescriptorType(descriptor, YMLLastDescriptor.class));
            first.ifPresent(descriptor -> store.addDescriptorType(descriptor, YMLFirstDescriptor.class));

        } else if (node.getClass().isAssignableFrom(ScalarNode.class)) {
            ScalarNode scalarNode = (ScalarNode) node;
            YMLScalarDescriptor scalarDescriptor = store.create(YMLScalarDescriptor.class);
            scalarDescriptor.setValue(scalarNode.getScalarValue());
            scalarNode.getIndex().ifPresent(i -> scalarDescriptor.setIndex(i));

            anchorProcessor.process(scalarNode, scalarDescriptor, mode);

            handler.accept(scalarDescriptor);
        } else if (node.getClass().isAssignableFrom(MapNode.class)) {
            MapNode mapNode = (MapNode) node;
            YMLMapDescriptor mapDescriptor = store.create(YMLMapDescriptor.class);
            mapNode.getIndex().ifPresent(i -> mapDescriptor.setIndex(i));
            handler.accept(mapDescriptor);

            anchorProcessor.process(mapNode, mapDescriptor, mode);

            mapNode.getSimpleKeys().forEach(keyNode -> {
                YMLSimpleKeyDescriptor keyDescriptor = store.create(YMLSimpleKeyDescriptor.class);
                keyDescriptor.setName(keyNode.getKeyName());
                mapDescriptor.getKeys().add(keyDescriptor);

                Consumer<YMLDescriptor> newHandler = descriptor -> {
                    store.addDescriptorType(descriptor, YMLValueDescriptor.class);
                    keyDescriptor.setValue(descriptor);
                };

                if (keyNode.getValue().getClass().isAssignableFrom(AliasNode.class)) {
                    AliasNode aliasNode = (AliasNode) keyNode.getValue();
                    BaseNode<?> referencedNode = aliasNode.getReferencedNode();
                    traverse(referencedNode, newHandler, Mode.REFERENCE);
                } else {
                    BaseNode<?> valueNode = keyNode.getValue();
                    traverse(valueNode, newHandler, mode);
                }

                store.addDescriptorType(keyDescriptor.getValue(), YMLValueDescriptor.class);
            });

            mapNode.getComplexKeys().forEach(keyNode -> {
                YMLComplexKeyDescriptor keyDescriptor = store.create(YMLComplexKeyDescriptor.class);

                mapDescriptor.getComplexKeys().add(keyDescriptor);
                // todo Check if a alias can here occour
                traverse(keyNode.getKeyNode(), descriptor -> keyDescriptor.setKey(descriptor), mode);
                traverse(keyNode.getValue(), descriptor -> keyDescriptor.setValue(descriptor), mode);

                store.addDescriptorType(keyDescriptor.getKey(), YMLComplexKeyValue.class);
                store.addDescriptorType(keyDescriptor.getValue(), YMLValueDescriptor.class);
            });
        } else {
            // todo
            throw new IllegalStateException();
        }
    }

    class AnchorProcessor {
        public void process(AnchorSupport<? extends NodeEvent> node, YMLDescriptor descriptor, Mode mode) {
            boolean createAnchor = mode.isInStandardMode() && node.getAnchor().isPresent();

            if (createAnchor) {
                String anchor = node.getAnchor().get();
                YMLAnchorDescriptor ymlAnchorDescriptor = store.addDescriptorType(descriptor, YMLAnchorDescriptor.class);
                ymlAnchorDescriptor.setAnchorName(anchor);
            }
        }
    }

}
