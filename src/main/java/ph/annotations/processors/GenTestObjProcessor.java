package ph.annotations.processors;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import ph.annotations.metadata.GenTestObj;


@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GenTestObjProcessor extends AbstractProcessor {

	private static final String NEWLINE = System.getProperty("line.separator");

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotationTypes = new HashSet<String>();
		annotationTypes.add(GenTestObj.class.getName());
		return annotationTypes;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		for (String annotationType : getSupportedAnnotationTypes()) {
			TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(annotationType);
			Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(typeElement);
			for (Element annotatedElement : annotatedElements) {

				if (annotatedElement instanceof TypeElement) {
					genSource((TypeElement) annotatedElement);
				}
			}
		}
		return false;
	}
	
	public void genSource(TypeElement typeelement) {
    	try {
    		PrintWriter printwriter = createSourceFile(getGenPath(typeelement));
    		
    		writePackage(printwriter, getPackage(typeelement));
    		writeClassDeclaration(printwriter, getGenClassname(typeelement));
    		
    		printwriter.println("}");
    		printwriter.flush();
    		printwriter.close();
    	} catch (IOException ioe) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Unable to generate" + 
                    " target class for elmenet [reason: " + ioe.toString() + "]",
                    typeelement);
    	}
    	
    }
        
	public PrintWriter createSourceFile(String srcFileName) throws IOException {
		JavaFileObject javafileobject = processingEnv.getFiler().createSourceFile(srcFileName, new Element[0]);
        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, "Creating " + javafileobject.toUri());
        return new PrintWriter(javafileobject.openWriter());
    }
	
	public String getGenPath(TypeElement typeelement) {
       return (new StringBuilder()).append(getPackage(typeelement)).append(".").append(getGenClassname(typeelement)).toString();
    }

	public String getPackage(TypeElement typeelement) {
        String elementPackage = typeelement.getEnclosingElement().toString();
        if (elementPackage.startsWith("package ")) {
        	elementPackage = elementPackage.substring(8);
        }
        return elementPackage;
    }

	public String getGenClassname(TypeElement typeelement) {
        return (new StringBuilder()).append("_").append(typeelement.getSimpleName().toString()).toString();
    }

	public void writePackage(PrintWriter printwriter, String packageName) {
        printwriter.format((new StringBuilder()).append("package %s;").append(NEWLINE).toString(), new Object[] {packageName});
        printwriter.println();
    }        

	public void writeClassDeclaration(PrintWriter printwriter, String clsName) {
    	printwriter.println("@SuppressWarnings({\"cast\", \"deprecation\", \"unchecked\"})");
    	printwriter.format((new StringBuilder()).append("public abstract class %s {").append(NEWLINE).toString(), new Object[] {clsName});
    }
 
}