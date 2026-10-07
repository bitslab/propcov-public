package edu.uic.bitslab.propcov.core.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class JavaPropertiesTest {
    @Test
    void build() {
        JavaProperties javaProperties = new JavaProperties();
        javaProperties.props = null;
        javaProperties.build();

        javaProperties.props = new ArrayList<>();
        javaProperties.build();

        JavaProperties.Prop prop1 = new JavaProperties.Prop();
        prop1.name = "name1";
        prop1.description = "description1";
        prop1.defaultValue = "defaultValue1";
        javaProperties.props.add(prop1);

        JavaProperties.Prop prop2 = new JavaProperties.Prop();
        prop2.name = "name.2";
        prop2.description = "description2";
        prop2.defaultValue = "defaultValue2";
        prop2.propertyKey = "key2";
        javaProperties.props.add(prop2);

        JavaProperties.Prop prop3 = new JavaProperties.Prop();
        prop3.name = "name.3";
        prop3.description = "description3";
        prop3.defaultValue = "defaultValue3";
        prop3.propertyKey = null;
        javaProperties.props.add(prop3);

        JavaProperties.Prop prop4 = new JavaProperties.Prop();
        prop4.name = "name.4";
        prop4.description = "description4";
        prop4.defaultValue = "defaultValue4";
        prop4.propertyKey = "";
        javaProperties.props.add(prop4);

        javaProperties.build();
        javaProperties.build();
    }

    @Test
    void resolveString() {
        JavaProperties javaProperties = new JavaProperties();
        assertThrows(RuntimeException.class, () -> javaProperties.resolveString("blah", null));
    }

    @Test
    void testEquals() {
        JavaProperties javaProperties1 = new JavaProperties();
        javaProperties1.props = new ArrayList<>();
        javaProperties1.build();
        //noinspection ConstantValue,SimplifiableAssertion
        assertFalse(javaProperties1.equals(null));

        JavaProperties javaProperties2 = new JavaProperties();
        javaProperties2.props = new ArrayList<>();
        javaProperties2.build();
        assertEquals(javaProperties1, javaProperties2);

        JavaProperties.Prop prop1 = new JavaProperties.Prop();
        prop1.name = "name1";
        prop1.description = "description1";
        prop1.defaultValue = "defaultValue1";
        javaProperties1.props.add(prop1);
        javaProperties1.build();
        assertNotEquals(javaProperties1, javaProperties2);

        javaProperties2.props.add(prop1);
        javaProperties2.build();
        assertEquals(javaProperties1, javaProperties2);

    }

    @Test
    void testToString() {
        JavaProperties javaProperties = new JavaProperties();
        javaProperties.props = new ArrayList<>();
        JavaProperties.Prop prop1 = new JavaProperties.Prop();
        prop1.name = "name1";
        prop1.description = "description1";
        prop1.defaultValue = "defaultValue1";
        javaProperties.props.add(prop1);
        javaProperties.packageName = "package name";
        javaProperties.applyOrder = 1;
        javaProperties.description = "some description";

        assertEquals("package name: some description\n" +
                "name1 : defaultValue1 : description1", javaProperties.toString());
    }

    @Test
    void testToStringNullProp() {
        JavaProperties javaProperties = new JavaProperties();
        javaProperties.props = null;
        javaProperties.packageName = "package name";
        javaProperties.applyOrder = 1;
        javaProperties.description = "some description";

        assertEquals("package name: some description\n", javaProperties.toString());

    }
}