# Composite Sum Types for Hibernate

Java 17 allows for sealed classes and thus composite sum types. 
This project is an annotation processor that generates Hibernate user types for composite sum types.

## What is a composite sum type?

Example for a sum type with two variants `Active` and `Inactive`:

```java
public sealed interface State extends Serializable {
    LocalDate start();
    record Active(LocalDate start) implements State {}
    record Inactive(LocalDate start, LocalDate ending) implements State {}
}
```

## How to use it?

We would like to be able to do this

```java
import jakarta.persistence.Embeddable;

@Entity
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String pnr;

    @Embedded
    private State state;
    
    ...
    
    @Embeddable
    public sealed interface State extends Serializable {
        LocalDate start();
        record Active(LocalDate start) implements State { }
        record Inactive(LocalDate start, LocalDate ending) implements State { }
    }
}
```

but this is not possible because Hibernate does not know how to persist/hydrate `State`.
`@CompositeType` to the rescue:

```java
package com.example;


import jakarta.persistence.Column;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class StateCompositeUserType implements CompositeUserType<com.example.Employee.State> {

    @Override
    public Object getPropertyValue(com.example.Employee.State component, int index) throws HibernateException {
        if (component instanceof com.example.Employee.State.Inactive inactive) {
            return switch (index) {
                case 1 -> inactive.start();
                case 0 -> inactive.ending();
                default -> null;
            };
        }
         else if (component instanceof com.example.Employee.State.Active active) {
            return switch (index) {
                case 1 -> active.start();
                default -> null;
            };
        }
        else {
            throw new AssertionError("Unknown alternative: " + component.getClass().getName());
        }
        
    }

    @Override
    public com.example.Employee.State instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
        if (values.getValue(1, java.time.LocalDate.class) != null && values.getValue(0, java.time.LocalDate.class) != null) {
            return new com.example.Employee.State.Inactive(
                        values.getValue(1, java.time.LocalDate.class), 
                        values.getValue(0, java.time.LocalDate.class)
            );
        }
         else if (values.getValue(1, java.time.LocalDate.class) != null) {
            return new com.example.Employee.State.Active(
                        values.getValue(1, java.time.LocalDate.class)
            );
        }
        else {
            throw new AssertionError("Unknown alternative");
        }
        
    }

    @Override
    public Class<?> embeddable() {
        return StateEmbeddable.class;
    }

    @Override
    public Class<com.example.Employee.State> returnedClass() {
        return com.example.Employee.State.class;
    }

    @Override
    public boolean equals(com.example.Employee.State x, com.example.Employee.State y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(com.example.Employee.State x) {
        return Objects.hashCode(x);
    }

    @Override
    public com.example.Employee.State deepCopy(com.example.Employee.State value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(com.example.Employee.State value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public com.example.Employee.State assemble(Serializable cached, Object owner) {
        return (com.example.Employee.State) cached;
    }

    @Override
    public com.example.Employee.State replace(com.example.Employee.State detached, com.example.Employee.State managed, Object owner) {
        return detached;
    }

    public static class StateEmbeddable {
        java.time.LocalDate ending;
        java.time.LocalDate start;
    }
}
```

Ough, thats a lot of boilerplate. How about this:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        ...
        <annotationProcessors>
            <annotationProcessor>io.github.alwins0n.hibernate.cst.CompositeSumTypProcessor</annotationProcessor>
        </annotationProcessors>
    </configuration>
</plugin>
```

```java
import jakarta.persistence.Embeddable;

@Entity
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String pnr;

    @Embedded
    @CompositeType(StateCompositeUserType.class) // reference the generated class
    private State state;
    
    ...
    
    @CompositeSumType // generate the embeddable user type
    public sealed interface State extends Serializable {
        LocalDate start();
        record Active(LocalDate start) implements State { }
        record Inactive(LocalDate start, LocalDate ending) implements State { }
    }
}
```

## How does it work and limitations

The basis assumption is that one uses composite sum types to avoid null. 
This is how the generated java code knows how to persist and hydrate the composite sum type:
It searches for the maximum nonnull fields it can find and uses this to determine the variant.

Currently, the following limitations apply (TODOs):
- No support for nested hierarchies
- No support for `@AttributeOverride`
- No support for not null constraints for shared fields
- Sealed type must be interface, implementations must be records
- No nullability in record components
- No support for `@Column` on record components
- No support to query for a variant type. Maybe reuse `@DiscriminatorColumn`?


