package io.github.alwins0n.hibernate.cst;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CompositeSumTypRendererTest {

    @Test void test() {
        var tmpl = CompositeSumTypProcessor.readTemplate();
        var unit = new CompositeSumTypeRenderer(tmpl);
        var filled = unit.render(
                new CompositeSumTypeData(
                        "com.acme.Range",
                        List.of(
                                new CompositeSumTypeData.Alternative(
                                        "com.acme.Range.Closed",
                                        Set.of(
                                                new NameAndType("start", LocalDate.class.getName()),
                                                new NameAndType("end", LocalDate.class.getName())
                                        )
                                ),
                                new CompositeSumTypeData.Alternative(
                                        "com.acme.Range.Open",
                                        Set.of(
                                                new NameAndType("start", LocalDate.class.getName())
                                        )
                                )
                        )
                ),  "RangeCompositeUserType", "com.acme"
        );
        assertFalse(filled.isEmpty());
        System.out.println(filled);
    }

}
