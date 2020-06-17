package de.endrullis.idea.postfixtemplates.languages.scala;

import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.OrderedSet;
import de.endrullis.idea.postfixtemplates.templates.MyVariable;
import de.endrullis.idea.postfixtemplates.templates.NavigatablePostfixTemplate;
import de.endrullis.idea.postfixtemplates.templates.SpecialType;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.ScalaStringBasedPostfixTemplate;
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector;
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.endrullis.idea.postfixtemplates.languages.java.CustomJavaStringPostfixTemplate.withProjectClassCondition;
import static de.endrullis.idea.postfixtemplates.languages.scala.ScalaPostfixTemplatesUtils.*;
import static de.endrullis.idea.postfixtemplates.templates.CustomPostfixTemplateUtils.parseVariables;
import static de.endrullis.idea.postfixtemplates.templates.CustomPostfixTemplateUtils.removeVariableValues;
import static de.endrullis.idea.postfixtemplates.templates.SimpleStringBasedPostfixTemplate.addVariablesToTemplate;
import static de.endrullis.idea.postfixtemplates.settings.CustomPostfixTemplates.PREDEFINED_VARIABLES;

/**
 * Custom postfix template for Scala.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
@SuppressWarnings("WeakerAccess")
public class CustomScalaStringPostfixTemplate extends ScalaStringBasedPostfixTemplate implements NavigatablePostfixTemplate {

	static final Pattern IMPORT_PATTERN = Pattern.compile("\\[IMPORT ([^\\]]+)\\]");

	private static final Map<String, Condition<PsiElement>> type2psiCondition = new HashMap<String, Condition<PsiElement>>() {{
		put(SpecialType.ANY.name(), e -> true);
		put(SpecialType.VOID.name(), VOID);
		put(SpecialType.NON_VOID.name(), NON_VOID);
		//put(SpecialType.ARRAY.name(), IS_ARRAY);
		put(SpecialType.BOOLEAN.name(), BOOLEAN);
		//put(SpecialType.ITERABLE_OR_ARRAY.name(), IS_ITERABLE_OR_ARRAY);
		put(SpecialType.NUMBER.name(), DECIMAL_NUMBER);
		put(SpecialType.BYTE.name(), BYTE);
		put(SpecialType.SHORT.name(), SHORT);
		put(SpecialType.CHAR.name(), CHAR);
		put(SpecialType.INT.name(), INT);
		put(SpecialType.LONG.name(), LONG);
		put(SpecialType.FLOAT.name(), FLOAT);
		put(SpecialType.DOUBLE.name(), DOUBLE);
		/*
		put(SpecialType.BYTE_LITERAL.name(), isCertainNumberLiteral(PsiType.BYTE));
		put(SpecialType.SHORT_LITERAL.name(), isCertainNumberLiteral(PsiType.SHORT));
		put(SpecialType.CHAR_LITERAL.name(), isCertainNumberLiteral(PsiType.CHAR));
		put(SpecialType.INT_LITERAL.name(), isCertainNumberLiteral(PsiType.INT));
		put(SpecialType.LONG_LITERAL.name(), isCertainNumberLiteral(PsiType.LONG));
		put(SpecialType.FLOAT_LITERAL.name(), isCertainNumberLiteral(PsiType.FLOAT));
		put(SpecialType.DOUBLE_LITERAL.name(), isCertainNumberLiteral(PsiType.DOUBLE));
		put(SpecialType.NUMBER_LITERAL.name(), IS_DECIMAL_NUMBER_LITERAL);
		put(SpecialType.STRING_LITERAL.name(), STRING_LITERAL);
		put(SpecialType.CLASS.name(), IS_CLASS);
		/*
		put(SpecialType.FIELD.name(), IS_FIELD);
		put(SpecialType.LOCAL_VARIABLE.name(), IS_LOCAL_VARIABLE);
		put(SpecialType.VARIABLE.name(), IS_VARIABLE);
		put(SpecialType.ASSIGNMENT.name(), IS_ASSIGNMENT);
		*/
	}};

	private final String          template;
	private final Set<MyVariable> variables = new OrderedSet<>();
	private final PsiElement      psiElement;
	private final Set<String>     imports;

	public CustomScalaStringPostfixTemplate(String matchingClass, String conditionClass, String templateName, String example, String template, PsiElement psiElement) {
		super(templateName.substring(1), example, new AncestorSelector.SelectAllAncestors(getCondition(matchingClass, conditionClass)));
		this.psiElement = psiElement;

		imports = extractImport(template);
		template = removeImports(template);
		template = template.replaceAll("\\[USE_STATIC_IMPORTS\\]", "");

		List<MyVariable> allVariables = parseVariables(template).stream().filter(v -> {
			return !PREDEFINED_VARIABLES.contains(v.getName());
		}).collect(Collectors.toList());

		this.template = removeVariableValues(template, allVariables);

		// filter out variable duplicates
		Set<String> foundVarNames = new HashSet<>();
		for (MyVariable variable : allVariables) {
			if (!foundVarNames.contains(variable.getName())) {
				variables.add(variable);
				foundVarNames.add(variable.getName());
			}
		}
	}

	@Override
	public void setVariables(@NotNull Template template, @NotNull PsiElement psiElement) {
		super.setVariables(template, psiElement);

		addVariablesToTemplate(template, variables, psiElement.getProject(), this);
	}

	/**
	 * Returns a function that returns true if
	 * <ul>
	 *   <li>the PSI element satisfies the type condition regarding {@code matchingClass} and</li>
	 *   <li>{@code conditionClass} is either {@code null} or available in the current module.</li>
	 * </ul>
	 *
	 * @param matchingClass  required type of the psi element to satisfy this condition
	 * @param conditionClass required class in the current module to satisfy this condition, or {@code null}
	 * @return PSI element condition
	 */
	@NotNull
	private static Condition<PsiElement> getCondition(final @NotNull String matchingClass, final @Nullable String conditionClass) {
		Condition<PsiElement> psiElementCondition = type2psiCondition.get(matchingClass);

		if (psiElementCondition == null) {
			psiElementCondition = ScalaPostfixTemplatesUtils.isDescendant(matchingClass);
		}

		return withProjectClassCondition(conditionClass, psiElementCondition);
	}


	@Nullable
	@Override
	public String getTemplateString(@NotNull PsiElement psiElement) {
		return template;
	}

	@Override
	public PsiElement getNavigationElement() {
		return psiElement;
	}

	@Override
	protected void prepareAndExpandForChooseExpression(@NotNull PsiElement expression, @NotNull Editor editor) {
		super.prepareAndExpandForChooseExpression(expression, editor);
	}

	@Override
	public void expandForChooseExpression(@NotNull PsiElement expr, @NotNull Editor editor) {
		for (String anImport : imports) {
			addImport(expr, anImport);
		}

		super.expandForChooseExpression(expr, editor);
	}

	private void addImport(@NotNull PsiElement expr, String qualifiedName) {
		ScImportsHolder importHolder = ScImportsHolder.apply(expr, expr.getProject());

		boolean imported = importHolder.getAllImportUsed().exists(i -> i.qualName().exists(n -> n.equals(qualifiedName)));

		if (!imported) {
			importHolder.addImportForPath(qualifiedName, expr);
		}
	}


	static Set<String> extractImport(String template) {
		val matcher = IMPORT_PATTERN.matcher(template);
		val imports = new HashSet<String>();

		while(matcher.find()) {
			imports.add(matcher.group(1));
		}

		return imports;
	}

	static String removeImports(String template) {
		return template.replaceAll(IMPORT_PATTERN.toString(), "");
	}

}
