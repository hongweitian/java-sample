package ph.annotations.processors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import ph.annotations.metadata.GenTestObj;


@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class CodeCheckProcessor extends AbstractProcessor {

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotationTypes = new HashSet<String>();
		annotationTypes.add(GenTestObj.class.getName());
		return annotationTypes;
	}

	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {

		for (String annotationType : getSupportedAnnotationTypes()) {
			TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(annotationType);
			Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(typeElement);
			for (Element annotatedElement : annotatedElements) {

				if (annotatedElement instanceof TypeElement) {
					validateParent((TypeElement) annotatedElement);
				}
			}
		}
		return true;
	}

	private void validateParent(TypeElement typeelement) {

		TypeMirror typemirror = null;
		List list = typeelement.getInterfaces();
		if (ElementKind.INTERFACE.equals(typeelement.getKind())) {
			typemirror = list.size() != 1 ? null : (TypeMirror) list.get(0);
		} else {
			typemirror = typeelement.getSuperclass();
		}
		
		if(typemirror == null || !((DeclaredType)typemirror).asElement().getSimpleName().toString().equals(getGenClassname(typeelement)))
			processingEnv.getMessager().printMessage(
					javax.tools.Diagnostic.Kind.ERROR, (new StringBuilder()).append("must *ONLY* extend ")
						.append(getGenClassname(typeelement)).toString(), typeelement);

	}
	
	private static String getGenClassname(TypeElement typeelement) {
		return (new StringBuilder()).append("_").append(typeelement.getSimpleName().toString()).toString();
    }

}
