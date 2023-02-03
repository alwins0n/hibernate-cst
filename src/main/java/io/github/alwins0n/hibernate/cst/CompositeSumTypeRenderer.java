package io.github.alwins0n.hibernate.cst;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class CompositeSumTypeRenderer {

    private static final String INTENT = "    ";

    private final String templateContent;

    public CompositeSumTypeRenderer(String templateContent) { this.templateContent = templateContent; }

    public String render(CompositeSumTypeData typeMetadata, String className, String packageName) {
        var originName = typeMetadata.typeName();
        var embeddableName = originName + "Embeddable";
        return templateContent
                .replace("%NAME%", className)
                .replace("%PACKAGE%", packageName)
                .replace("%SUM_TYPE%", originName)
                .replace("%EMBEDDABLE_TYPE%", Utils.getSimpleName(embeddableName))
                .replace("%EMBEDDABLE_TYPE_CONTENT%", buildEmbeddableTypeContent(typeMetadata.allFieldsOrdered(), typeMetadata.commonFields()))
                .replace("%GET_PROPERTY_BODY%", buildGetPropertyValueBody(typeMetadata.allFieldsOrdered(), typeMetadata.alternatives()))
                .replace("%INSTANTIATE_BODY%", buildInstantiateBody(typeMetadata.allFieldsOrdered(), typeMetadata.alternatives()));
    }

    private String buildEmbeddableTypeContent(List<NameAndType> fieldDefinitionsOrderedByName, Set<NameAndType> commonFields) {
        return fieldDefinitionsOrderedByName.stream()
                .map(def -> buildFieldDeclaration(commonFields, def))
                .collect(Collectors.joining("\n" + INTENT + INTENT));
    }

    private String buildFieldDeclaration(Set<NameAndType> commonFields, NameAndType def) {
        // var inferredNonnull = commonFields.contains(def) ? "@Column(nullable = false) " : ""; TODO
        return def.fullyQualifiedType() + " " + def.name() + ";";
    }

    private String buildGetPropertyValueBody(List<NameAndType> fieldDefinitionsOrderedByName, List<CompositeSumTypeData.Alternative> alternatives) {
        var fieldNames = fieldDefinitionsOrderedByName.stream().map(NameAndType::name).toList();
        var alternativesByArity = alternatives.stream()
                .sorted(Comparator.comparingInt(CompositeSumTypeData.Alternative::arity).reversed())
                .toList();

        var first = true;
        var sb = new StringBuilder();
        for (CompositeSumTypeData.Alternative alternative : alternativesByArity) {
            var localScopeName = Utils.getSimpleName(alternative.typeName()).toLowerCase();
            if (!first) {
                sb.append(" else ");
            }
            sb.append("if (component instanceof ").append(alternative.typeName()).append(" ").append(localScopeName).append(") {\n");
            sb.append(INTENT).append("return switch (index) {\n");
            for (NameAndType field : alternative.fields()) {
                sb.append(INTENT).append(INTENT).append("case ")
                        .append(fieldNames.indexOf(field.name())).append(" -> ")
                        .append(localScopeName).append(".").append(field.name()).append("();\n");
            }
            sb.append(INTENT).append(INTENT).append("default -> null;\n");
            sb.append(INTENT).append("};\n");
            sb.append("}\n");
            first = false;
        }
        sb.append("else {\n");
        sb.append(INTENT).append("throw new AssertionError(\"Unknown alternative: \" + component.getClass().getName());\n");
        sb.append("}\n");

        return sb.toString().replaceAll("\\n", "\n" + INTENT + INTENT);
    }

    private String buildInstantiateBody(List<NameAndType> fieldDefinitionsOrderedByName, List<CompositeSumTypeData.Alternative> alternatives) {
        var sortedFieldNames = fieldDefinitionsOrderedByName.stream().map(NameAndType::name).toList();
        var alternativesByArity = alternatives.stream()
                .sorted(Comparator.comparingInt(CompositeSumTypeData.Alternative::arity).reversed())
                .toList();

        var sb = new StringBuilder();
        var first = true;
        for (CompositeSumTypeData.Alternative alternative : alternativesByArity) {
            if (!first) {
                sb.append(" else ");
            }
            sb.append("if (");
            sb.append(alternative.fields().stream()
                    .map(f -> buildValueExtract(sortedFieldNames, f) + " != null")
                    .collect(Collectors.joining(" && ")));
            sb.append(") {\n");
            sb.append(INTENT).append("return new ").append(alternative.typeName())
                    .append("(\n" + INTENT + INTENT + INTENT + INTENT)
                    .append(alternative.fields().stream()
                            .map(f -> buildValueExtract(sortedFieldNames, f))
                            .collect(Collectors.joining(", " + "\n" + INTENT + INTENT + INTENT + INTENT)))
                    .append("\n");
            sb.append(INTENT).append(");\n");
            sb.append("}\n");
            first = false;
        }
        sb.append("else {\n");
        sb.append(INTENT).append("throw new AssertionError(\"Unknown alternative\");\n");
        sb.append("}\n");
        return sb.toString().replaceAll("\\n", "\n" + INTENT + INTENT);
    }

    private static String buildValueExtract(List<String> sortedFieldNames, NameAndType fieldNameAndType) {
        return "values.getValue("
                + sortedFieldNames.indexOf(fieldNameAndType.name())
                + ", " + fieldNameAndType.fullyQualifiedType()
                + ".class)";
    }
}
