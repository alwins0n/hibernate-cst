package io.github.alwins0n.hibernate.cst;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

record CompositeSumTypeData(
        String typeName,
        List<Alternative> alternatives
) {
    record Alternative(
            String typeName,
            Set<NameAndType> fields
    ) {
        int arity() { return fields.size(); }
    }

    List<NameAndType> allFieldsOrdered() {
        return alternatives
                .stream()
                .flatMap(a -> a.fields().stream())
                .distinct()
                .sorted(Comparator.comparing(NameAndType::name))
                .toList();
    }

    Set<NameAndType> commonFields() {
        return alternatives
                .stream()
                .map(Alternative::fields)
                .reduce(Utils::getIntersection)
                .orElseThrow();
    }

}
