package io.github.alwins0n.hibernate.cst;

import java.util.HashSet;
import java.util.Set;

class Utils {
    private Utils() { }

    static <T> Set<T> getIntersection(Set<T> a, Set<T> b) {
        var localCopy = new HashSet<>(a);
        localCopy.retainAll(b);
        return localCopy;
    }

    static String getSimpleName(String fullTypeName) {
        return fullTypeName.substring(fullTypeName.lastIndexOf(".") + 1);
    }
}
