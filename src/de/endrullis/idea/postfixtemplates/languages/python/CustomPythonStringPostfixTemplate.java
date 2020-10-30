package de.endrullis.idea.postfixtemplates.languages.python;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.codeInsight.postfix.PyPostfixUtils;
import com.jetbrains.python.psi.PyTypedElement;
import com.jetbrains.python.psi.types.PyClassLikeType;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.TypeEvalContext;

import de.endrullis.idea.postfixtemplates.templates.SimpleStringBasedPostfixTemplate;
import de.endrullis.idea.postfixtemplates.templates.SpecialType;

/**
 * Custom postfix template for Python.
 */
@SuppressWarnings("WeakerAccess")
public class CustomPythonStringPostfixTemplate extends SimpleStringBasedPostfixTemplate {
    /**
     * Contains predefined type-to-psiCondition mappings as well as cached mappings for individual types.
     */
    private static final Map<String, Condition<PsiElement>> type2psiCondition =
        new HashMap<String, Condition<PsiElement>>() {{
            put(SpecialType.ANY.name(), e -> true);

            PythonPostfixTemplatesUtils.COMPLEX_TYPES.forEach((key, value) -> {
				put(key, e -> {
					String typeName = getTypeName(e);
					return value.contains(typeName);
				});
			});
            for (String pyType : PythonPostfixTemplatesUtils.PYTHON_TYPES) {
                put(pyType, e -> {
                    if (e instanceof PyTypedElement) {
						String typeName = getTypeName(e);
						return pyType.equals(typeName);
                    } else {
                        return false;
                    }
                });
            }
        }};

    @Nullable
    private static PyType getType(PsiElement e) {
        return TypeEvalContext.codeAnalysis(e.getProject(), e.getContainingFile()).getType((PyTypedElement) e);
    }

    @Nullable
    private static String getTypeName(PsiElement e) {
        PyType type = getType(e);
        final String name;
        if (type != null) {
            if (type instanceof PyClassLikeType) {
                name = ((PyClassLikeType) type).getClassQName();
            } else {
                name = type.getName();
            }
        } else {
            name = null;
        }
        return name;
    }

    public CustomPythonStringPostfixTemplate(String matchingClass, String conditionClass, String name, String example,
        String template, PostfixTemplateProvider provider, PsiElement psiElement) {
        super(name, example, template, provider, psiElement,
            PyPostfixUtils.selectorAllExpressionsWithCurrentOffset(getCondition(matchingClass, conditionClass)));
    }

    @NotNull
    public static Condition<PsiElement> getCondition(final @NotNull String matchingClass,
        final @Nullable String conditionClass) {
        Condition<PsiElement> psiElementCondition = type2psiCondition.get(matchingClass);

        // PyElementTypes.INTEGER_LITERAL_EXPRESSION
        //TypeEvalContext.codeAnalysis(e.getProject(), e.getContainingFile()).getType((PyTypedElement) e)

        if (psiElementCondition == null) {
            //psiElementCondition = PythonPostfixTemplatesUtils.isCustomClass(matchingClass);
        }

        return psiElementCondition;
    }

}
