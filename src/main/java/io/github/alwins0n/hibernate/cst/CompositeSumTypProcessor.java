package io.github.alwins0n.hibernate.cst;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes("io.github.alwins0n.hibernate.cst.CompositeSumType")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CompositeSumTypProcessor extends AbstractProcessor {

    private static final String NAME_POSTFIX_OUTPUT_CLASS = "CompositeUserType";
    private static final String CST_JAVA_TEMPLATE = "/templates/CompositeSumType.java";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty() || roundEnv.processingOver()) {
            return false;
        }
        if (annotations.size() != 1) {
            // this should never happen
            throw new AssertionError("Only io.github.alwins0n.hibernate.cst.CompositeSumType is supported");
        }
        var annotation = annotations.iterator().next();

        String templateContent;
        try {
            templateContent = readTemplate();
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error reading template file: " + e.getMessage());
            return false;
        }

        var renderer = new CompositeSumTypeRenderer(templateContent);
        for (var compositeSumTypeElement : roundEnv.getElementsAnnotatedWith(annotation)) {
            String processedFileContent;
            try {
                processedFileContent = processToJavaSource(renderer, (TypeElement) compositeSumTypeElement);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error processing composite sum type: " + e.getMessage());
                return false;
            }
            try {
                var simpleName = Utils.getSimpleName(compositeSumTypeElement.getSimpleName().toString());
                var className = simpleName + NAME_POSTFIX_OUTPUT_CLASS;
                var filename = getPackageName((TypeElement) compositeSumTypeElement) + "." + className;
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Writing " + filename);
                writeFile(filename, processedFileContent);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing output file: " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    private String processToJavaSource(CompositeSumTypeRenderer renderer, TypeElement compositeSumTypeElement) {
        var metadata = CompositeSumTypeDataBuilder.extractMetadata(processingEnv, compositeSumTypeElement);
        var className = Utils.getSimpleName(metadata.typeName()) + NAME_POSTFIX_OUTPUT_CLASS;
        return renderer.render(metadata, className, getPackageName(compositeSumTypeElement));
    }

    private static String getPackageName(TypeElement element) {
        if (element.getEnclosingElement() instanceof PackageElement pkg) {
            return pkg.getQualifiedName().toString();
        } else if (element.getEnclosingElement() instanceof TypeElement parentType) {
            return getPackageName(parentType);
        } else {
            throw new IllegalArgumentException("Unknown enclosing element: " + element.getEnclosingElement());
        }
    }

    static String readTemplate() {
        try (var is = CompositeSumTypProcessor.class.getResourceAsStream(CST_JAVA_TEMPLATE)) {
            if (is == null) {
                throw new IllegalArgumentException("Template not found: " + CST_JAVA_TEMPLATE);
            }
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read template", e);
        }
    }

    private void writeFile(String name, String content) throws IOException {
        var file = processingEnv.getFiler().createSourceFile(name);
        try (var writer = file.openWriter()) {
            writer.write(content);
        }
    }

}
