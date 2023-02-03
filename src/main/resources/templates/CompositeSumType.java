package %PACKAGE%;


import jakarta.persistence.Column;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class %NAME% implements CompositeUserType<%SUM_TYPE%> {

    @Override
    public Object getPropertyValue(%SUM_TYPE% component, int index) throws HibernateException {
        %GET_PROPERTY_BODY%
    }

    @Override
    public %SUM_TYPE% instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
        %INSTANTIATE_BODY%
    }

    @Override
    public Class<?> embeddable() {
        return %EMBEDDABLE_TYPE%.class;
    }

    @Override
    public Class<%SUM_TYPE%> returnedClass() {
        return %SUM_TYPE%.class;
    }

    @Override
    public boolean equals(%SUM_TYPE% x, %SUM_TYPE% y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(%SUM_TYPE% x) {
        return Objects.hashCode(x);
    }

    @Override
    public %SUM_TYPE% deepCopy(%SUM_TYPE% value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(%SUM_TYPE% value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public %SUM_TYPE% assemble(Serializable cached, Object owner) {
        return (%SUM_TYPE%) cached;
    }

    @Override
    public %SUM_TYPE% replace(%SUM_TYPE% detached, %SUM_TYPE% managed, Object owner) {
        return detached;
    }

    public static class %EMBEDDABLE_TYPE% {
        %EMBEDDABLE_TYPE_CONTENT%
    }
}
