package com.buschmais.jqassistant.plugin.java.impl.scanner.visitor;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.buschmais.jqassistant.plugin.java.api.model.*;
import com.buschmais.jqassistant.plugin.java.api.scanner.SignatureHelper;

public abstract class AbstractAnnotationVisitor<D> extends org.objectweb.asm.AnnotationVisitor {

    private VisitorHelper visitorHelper;

    private ArrayValueDescriptor arrayValueDescriptor;

    private D descriptor;

    /**
     * Constructor.
     * 
     * @param visitorHelper
     *            The {@link VisitorHelper}.
     */
    protected AbstractAnnotationVisitor(D descriptor, VisitorHelper visitorHelper) {
        super(Opcodes.ASM5);
        this.descriptor = descriptor;
        this.visitorHelper = visitorHelper;
    }

    @Override
    public void visit(final String name, final Object value) {
        if (value instanceof Type) {
            String type = SignatureHelper.getType((Type) value);
            ClassValueDescriptor valueDescriptor = createValue(ClassValueDescriptor.class, name);
            valueDescriptor.setValue(visitorHelper.getType(type).getTypeDescriptor());
            addValue(name, valueDescriptor);
        } else {
            PrimitiveValueDescriptor valueDescriptor = createValue(PrimitiveValueDescriptor.class, name);
            TypeDescriptor typeDescriptor = visitorHelper.getType(value.getClass().getName()).getTypeDescriptor();
            valueDescriptor.setType(typeDescriptor);
            valueDescriptor.setValue(value);
            addValue(name, valueDescriptor);
        }
    }

    @Override
    public void visitEnum(final String name, final String desc, final String value) {
        EnumValueDescriptor valueDescriptor = createValue(EnumValueDescriptor.class, name);
        VisitorHelper.CachedType cachedTypeDescriptor = visitorHelper.getType(SignatureHelper.getType(desc));
        FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(cachedTypeDescriptor, SignatureHelper.getFieldSignature(value, desc));
        valueDescriptor.setType(visitorHelper.getType(Enum.class.getName()).getTypeDescriptor());
        valueDescriptor.setValue(fieldDescriptor);
        addValue(name, valueDescriptor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name, final String desc) {
        AnnotationValueDescriptor valueDescriptor = createValue(AnnotationValueDescriptor.class, name);
        valueDescriptor.setType(visitorHelper.getType(SignatureHelper.getType(desc)).getTypeDescriptor());
        addValue(name, valueDescriptor);
        return new AnnotationVisitor(valueDescriptor, visitorHelper);
    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitArray(final String name) {
        this.arrayValueDescriptor = createValue(ArrayValueDescriptor.class, name);
        setValue(descriptor, arrayValueDescriptor);
        return this;
    }

    protected abstract void setValue(D descriptor, ValueDescriptor<?> value);

    /**
     * Create a value descriptor of given type and name and initializes it.
     * 
     * @param type
     *            The class type.
     * @param name
     *            The name
     * @param <T>
     *            The type.
     * @return The initialized descriptor.
     */
    private <T extends ValueDescriptor<?>> T createValue(Class<T> type, String name) {
        if (name != null) {
            this.arrayValueDescriptor = null;
        }
        String valueName;
        if (arrayValueDescriptor != null) {
            valueName = "[" + getArrayValue().size() + "]";
        } else {
            valueName = name;
        }
        T valueDescriptor = visitorHelper.getValueDescriptor(type);
        valueDescriptor.setName(valueName);
        return valueDescriptor;
    }

    /**
     * Add the descriptor as value to the current annotation or array value.
     * 
     * @param name
     *            The name.
     * @param value
     *            The value.
     */
    private void addValue(String name, ValueDescriptor<?> value) {
        if (arrayValueDescriptor != null && name == null) {
            getArrayValue().add(value);
        } else {
            setValue(descriptor, value);
        }
    }

    /**
     * Get the array of referenced values.
     * 
     * @return The array of referenced values.
     */
    private List<ValueDescriptor<?>> getArrayValue() {
        List<ValueDescriptor<?>> values = arrayValueDescriptor.getValue();
        if (values == null) {
            values = new LinkedList<>();
            arrayValueDescriptor.setValue(values);
        }
        return values;
    }
}
