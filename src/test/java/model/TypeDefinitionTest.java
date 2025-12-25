package model;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import system.model.TypeDefinition;

import static org.junit.jupiter.api.Assertions.*;

class TypeDefinitionTest {

    @Test
    void typeDefinition_constructorAndGetters() {
        TypeDefinition type = new TypeDefinition(1, "VARCHAR", -1);
        assertEquals(1, type.getOid());
        assertEquals("VARCHAR", type.getName());
        assertEquals(-1, type.getByteLength());
    }

    @Test
    void typeDefinition_fixedLengthType() {
        TypeDefinition fixedType = new TypeDefinition(1, "INT64", 8);
        assertEquals(8, fixedType.getByteLength());
    }

    @Test
    void typeDefinition_variableLengthType() {
        TypeDefinition varType = new TypeDefinition(2, "VARCHAR", -1);
        assertEquals(-1, varType.getByteLength());
    }

    @Test
    void typeDefinition_serializationRoundTrip() {
        TypeDefinition original = new TypeDefinition(789, "DECIMAL", 16);
        byte[] bytes = original.toBytes();
        TypeDefinition restored = original.fromBytes(bytes);
        assertEquals(original.getOid(), restored.getOid());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getByteLength(), restored.getByteLength());
    }

    @Test
    void typeDefinition_serializationWithUnicode() {
        TypeDefinition original = new TypeDefinition(1, "ТИП", 10);
        byte[] bytes = original.toBytes();
        TypeDefinition restored = original.fromBytes(bytes);
        assertEquals("ТИП", restored.getName());
    }

    @Test
    void typeDefinition_serializationWithEmptyName() {
        TypeDefinition original = new TypeDefinition(1, "", 0);
        byte[] bytes = original.toBytes();
        TypeDefinition restored = original.fromBytes(bytes);
        assertEquals("", restored.getName());
        assertEquals(0, restored.getByteLength());
    }

    @Test
    void typeDefinition_serializationExtremeValues() {
        TypeDefinition original = new TypeDefinition(
                Integer.MAX_VALUE,
                "long_name",
                Integer.MAX_VALUE
        );
        byte[] bytes = original.toBytes();
        TypeDefinition restored = original.fromBytes(bytes);
        assertEquals(Integer.MAX_VALUE, restored.getOid());
        assertEquals("long_name", restored.getName());
        assertEquals(Integer.MAX_VALUE, restored.getByteLength());
    }


    @Test
    void typeDefinition_commonDatabaseTypes() {
        TypeDefinition[] commonTypes = {
                new TypeDefinition(1, "INT64", 8),
                new TypeDefinition(2, "INT32", 4),
                new TypeDefinition(3, "VARCHAR", -1),
                new TypeDefinition(4, "BOOLEAN", 1),
                new TypeDefinition(5, "DOUBLE", 8),
                new TypeDefinition(6, "FLOAT", 4),
                new TypeDefinition(7, "DATE", 8),
                new TypeDefinition(8, "TIMESTAMP", 8)
        };

        for (TypeDefinition type : commonTypes) {
            byte[] bytes = type.toBytes();
            TypeDefinition restored = type.fromBytes(bytes);
            assertEquals(type.getName(), restored.getName());
            assertEquals(type.getByteLength(), restored.getByteLength());
        }
    }
}