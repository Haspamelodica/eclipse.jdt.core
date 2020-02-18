/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.compiler.apt.tests.processors.elements;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

import org.eclipse.jdt.internal.compiler.apt.dispatch.BaseProcessingEnvImpl;
import org.eclipse.jdt.compiler.apt.tests.processors.base.BaseProcessor;

@SupportedAnnotationTypes("*")
public class Java14ElementProcessor extends BaseProcessor {
	boolean reportSuccessAlready = true;
	RoundEnvironment roundEnv = null;
	Messager _messager = null;
	Filer _filer = null;
	boolean isBinaryMode = false;
	String mode;
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		_elementUtils = processingEnv.getElementUtils();
		_messager = processingEnv.getMessager();
		_filer = processingEnv.getFiler();
	}
	// Always return false from this processor, because it supports "*".
	// The return value does not signify success or failure!
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			return false;
		}
		
		this.roundEnv = roundEnv;
		Map<String, String> options = processingEnv.getOptions();
		if (!options.containsKey(this.getClass().getName())) {
			// Disable this processor unless we are intentionally performing the test.
			return false;
		} else {
			try {
				if (options.containsKey("binary")) {
					this.isBinaryMode = true;
					this.mode = "binary";
				} else {
					this.mode = "source";
				}
				if (!invokeTestMethods(options)) {
					testAll();
				}
				if (this.reportSuccessAlready) {
					super.reportSuccess();
				}
			} catch (AssertionFailedError e) {
				super.reportError(getExceptionStackTrace(e));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean invokeTestMethods(Map<String, String> options) throws Throwable {
		Method testMethod = null;
		Set<String> keys = options.keySet();
		boolean testsFound = false;
		for (String option : keys) {
			if (option.startsWith("test")) {
				try {
					testMethod = this.getClass().getDeclaredMethod(option, new Class[0]);
					if (testMethod != null) {
						testsFound = true;
						testMethod.invoke(this,  new Object[0]);
					}
				} catch (InvocationTargetException e) {
					throw e.getCause();
				} catch (Exception e) {
					super.reportError(getExceptionStackTrace(e));
				}
			}
		}
		return testsFound;
	}

	public void testAll() throws AssertionFailedError, IOException {
		testPreviewFlagTrue();
	}

	public void testPreviewFlagTrue() throws IOException {
		if (this.processingEnv instanceof BaseProcessingEnvImpl) {
			boolean preview = ((BaseProcessingEnvImpl) this.processingEnv).isPreviewEnabled();
			assertTrue("Preview flag not seen as enabled", preview);
		}
	}
	/*
	 * Basic test for record element and kind
	 */
	public void testRecords1() {
		Set<? extends Element> elements = roundEnv.getRootElements();
		TypeElement record = null;
		for (Element element : elements) {
			if ("Point".equals(element.getSimpleName().toString())) {
				record = (TypeElement) element;
			}
		}
		assertNotNull("TypeElement for record should not be null", record);
		assertEquals("Name for record should not be null", "records.Point", record.getQualifiedName().toString());
		assertEquals("Incorrect element kind", ElementKind.RECORD, record.getKind());
	}
	/*
	 * Test for presence of record component in a record element
	 */
	public void testRecords2() {
		Set<? extends Element> elements = roundEnv.getRootElements();
		TypeElement record = null;
		for (Element element : elements) {
			if ("Point".equals(element.getSimpleName().toString())) {
				record = (TypeElement) element;
			}
		}
		assertNotNull("TypeElement for record should not be null", record);
		List<? extends Element> enclosedElements = record.getEnclosedElements();
		assertNotNull("enclosedElements for record should not be null", enclosedElements);
		List<RecordComponentElement> recordComponentsIn = ElementFilter.recordComponentsIn(enclosedElements);
		int size = recordComponentsIn.size();
		assertEquals("incorrect no of record components", 1, size);
		Element element = enclosedElements.get(0);
		assertEquals("Incorrect kind of element", ElementKind.RECORD_COMPONENT, element.getKind());
		RecordComponentElement recordComponent = (RecordComponentElement) element;
		assertEquals("Incorrect name for record component", "comp_", recordComponent.getSimpleName().toString());
		Element enclosingElement = recordComponent.getEnclosingElement();
		assertEquals("Elements should be same", record, enclosingElement);
	}
	/*
	 * Test that the implicit modifiers are set for a record
	 */
	public void testRecords3() {
		Set<? extends Element> elements = roundEnv.getRootElements();
		TypeElement record = null;
		for (Element element : elements) {
			if ("Point".equals(element.getSimpleName().toString())) {
				record = (TypeElement) element;
			}
		}
		assertNotNull("TypeElement for record should not be null", record);
		Set<Modifier> modifiers = record.getModifiers();
		assertTrue("record should be public", modifiers.contains(Modifier.PUBLIC));
		assertTrue("record should be final", modifiers.contains(Modifier.FINAL));
	}
	/*
	 * Test for annotations on record and record components
	 */
	public void testRecords4() {
		Set<? extends Element> elements = roundEnv.getRootElements();
		System.out.println(elements);
		TypeElement record = null;
		for (Element element : elements) {
			if ("Point".equals(element.getSimpleName().toString())) {
				record = (TypeElement) element;
			}
		}
		assertNotNull("TypeElement for record should not be null", record);
		verifyAnnotations(record, new String[]{"@Deprecated()"});

		List<? extends Element> enclosedElements = record.getEnclosedElements();
		assertNotNull("enclosedElements for record should not be null", enclosedElements);
		List<RecordComponentElement> recordComponentsIn = ElementFilter.recordComponentsIn(enclosedElements);
		int size = recordComponentsIn.size();
		assertEquals("incorrect no of record components", 1, size);
		Element element = enclosedElements.get(0);
		assertEquals("Incorrect kind of element", ElementKind.RECORD_COMPONENT, element.getKind());
		RecordComponentElement recordComponent = (RecordComponentElement) element;
		
		verifyAnnotations(recordComponent, new String[]{"@MyAnnot()"});
	}
	/*
	 * Test for getAccessor of a record component
	 */
	public void testRecords5() {
		// This is not yet implemented.
	}

	@Override
	public void reportError(String msg) {
		throw new AssertionFailedError(msg);
	}
	private String getExceptionStackTrace(Throwable t) {
		StringBuffer buf = new StringBuffer(t.getMessage());
		StackTraceElement[] traces = t.getStackTrace();
		for (int i = 0; i < traces.length; i++) {
			StackTraceElement trace = traces[i];
			buf.append("\n\tat " + trace);
			if (i == 12)
				break; // Don't dump all stacks
		}
		return buf.toString();
	}
	protected String getElementsAsString(List<? extends Element> list) {
		StringBuilder builder = new StringBuilder("[");
		for (Element element : list) {
			if (element instanceof PackageElement) {
				builder.append(((PackageElement) element).getQualifiedName());
			} else if (element instanceof ModuleElement) {
				builder.append(((ModuleElement) element).getQualifiedName());
			} else if (element instanceof TypeElement) {
				builder.append(((TypeElement) element).getQualifiedName());
			}  else {
				builder.append(element.getSimpleName());
			}
			builder.append(", ");
		}
		builder.append("]");
		return builder.toString();
	}
	public void assertModifiers(Set<Modifier> modifiers, String[] expected) {
		assertEquals("Incorrect no of modifiers", modifiers.size(), expected.length);
		Set<String> actual = new HashSet<String>(expected.length);
		for (Modifier modifier : modifiers) {
			actual.add(modifier.toString());
		}
		for(int i = 0, length = expected.length; i < length; i++) {
			boolean result = actual.remove(expected[i]);
			if (!result) reportError("Modifier not present :" + expected[i]);
		}
		if (!actual.isEmpty()) {
			reportError("Unexpected modifiers present:" + actual.toString());
		}
	}
	public void assertTrue(String msg, boolean value) {
		if (!value) reportError(msg);
	}
	public void assertFalse(String msg, boolean value) {
		if (value) reportError(msg);
	}
	public void assertSame(String msg, Object obj1, Object obj2) {
		if (obj1 != obj2) {
			reportError(msg + ", should be " + obj1.toString() + " but " + obj2.toString());
		}
	}
	public void assertNotSame(String msg, Object obj1, Object obj2) {
		if (obj1 == obj2) {
			reportError(msg + ", " + obj1.toString() + " should not be same as " + obj2.toString());
		}
	}
	public void assertNotNull(String msg, Object obj) {
		if (obj == null) {
			reportError(msg);
		}
	}
	public void assertNull(String msg, Object obj) {
		if (obj != null) {
			reportError(msg);
		}
	}
    public void assertEquals(String message, Object expected, Object actual) {
        if (equalsRegardingNull(expected, actual)) {
            return;
        } else {
        	reportError(message + ", expected " + expected.toString() + " but was " + actual.toString());
        }
    }

    public void assertEquals(String message, Object expected, Object alternateExpected, Object actual) {
        if (equalsRegardingNull(expected, actual) || equalsRegardingNull(alternateExpected, actual)) {
            return;
        } else {
        	reportError(message + ", expected " + expected.toString() + " but was " + actual.toString());
        }
    }
    
    static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        }
        return expected.equals(actual);
    }
    
	public void assertEquals(String msg, int expected, int actual) {
		if (expected != actual) {
			StringBuffer buf = new StringBuffer();
			buf.append(msg);
			buf.append(", expected " + expected + " but was " + actual);
			reportError(buf.toString());
		}
	}
	public void assertEquals(Object expected, Object actual) {
		if (expected != actual) {
			
		}
	}
	private void verifyAnnotations(AnnotatedConstruct construct, String[] annots) {
		List<? extends AnnotationMirror> annotations = construct.getAnnotationMirrors();
		assertEquals("Incorrect no of annotations", annots.length, annotations.size());
		for(int i = 0, length = annots.length; i < length; i++) {
			AnnotationMirror mirror = annotations.get(i);
			assertEquals("Invalid annotation value", annots[i], getAnnotationString(mirror));
		}
	}
	
	private String getAnnotationString(AnnotationMirror annot) {
		DeclaredType annotType = annot.getAnnotationType();
		TypeElement type = (TypeElement) annotType.asElement();
		StringBuffer buf = new StringBuffer("@" + type.getSimpleName());
		Map<? extends ExecutableElement, ? extends AnnotationValue> values = annot.getElementValues();
		Set<? extends ExecutableElement> keys = values.keySet();
		buf.append('(');
		for (ExecutableElement executableElement : keys) { // @Marker3()
			buf.append(executableElement.getSimpleName());
			buf.append('=');
			AnnotationValue value = values.get(executableElement);
			buf.append(value.getValue());
		}
		buf.append(')');
		return buf.toString();
	}
	private class AssertionFailedError extends Error {
		private static final long serialVersionUID = 1L;

		public AssertionFailedError(String msg) {
			super(msg);
		}
	}
}