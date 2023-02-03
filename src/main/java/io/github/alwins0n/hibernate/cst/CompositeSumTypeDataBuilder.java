package io.github.alwins0n.hibernate.cst;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

class CompositeSumTypeDataBuilder {

    static CompositeSumTypeData extractMetadata(ProcessingEnvironment processingEnv, TypeElement compositeSumTypeElement) {
        if (!compositeSumTypeElement.getKind().isInterface()) {
            throw new IllegalArgumentException("Type annotated with CompositeSumType must be interface");
        }

        var subclasses = compositeSumTypeElement.getPermittedSubclasses();
        if (subclasses.isEmpty()) {
            throw new IllegalArgumentException("Type annotated with CompositeSumType must have subclasses");
        }

        var allRecord = subclasses.stream()
                .map(processingEnv.getTypeUtils()::asElement)
                .map(Element::getKind)
                .allMatch(k -> k == ElementKind.RECORD);

        if (!allRecord) {
            throw new IllegalArgumentException("All subclasses of a sealed type must be records");
        }

        var alternativesWithFields = subclasses
                .stream()
                .collect(Collectors.toMap(
                                TypeMirror::toString,
                                t -> getFieldElements((TypeElement) processingEnv.getTypeUtils().asElement(t))
                                        .stream()
                                        .map(CompositeSumTypeDataBuilder::toFieldDefinition)
                                        .collect(Collectors.toSet())
                        )
                )
                .entrySet()
                .stream()
                .map(e -> new CompositeSumTypeData.Alternative(e.getKey(), e.getValue()))
                .toList();

        return new CompositeSumTypeData(
                compositeSumTypeElement.getQualifiedName().toString(),
                alternativesWithFields
        );
    }

    private static NameAndType toFieldDefinition(VariableElement ve) {
        return new NameAndType(ve.getSimpleName().toString(), ve.asType().toString());
    }

    private static List<VariableElement> getFieldElements(TypeElement t) {
        return t.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .toList();
    }

}
