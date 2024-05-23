package com.weirddev.testme.intellij.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.weirddev.testme.intellij.action.CreateTestMeAction;
import com.weirddev.testme.intellij.action.TestMeActionHandler;
import com.weirddev.testme.intellij.builder.MethodFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * create test for selected method
 *
 * @author huangliang
 */
@NonNls
final class CreateTestForMethodIntention extends PsiElementBaseIntentionAction implements IntentionAction {

    /**
     * Checks whether this intention is available at the caret offset in file - the caret must sit a testable method
     * character in a ternary statement. If this condition is met, this intention's entry is shown in the available
     * intentions list.
     *
     * <p>Note: this method must do its checks quickly and return.</p>
     *
     * @param project a reference to the Project object being edited.
     * @param editor  a reference to the object editing the project source
     * @param element a reference to the PSI element currently under the caret
     * @return {@code true} if the caret is in a literal string element, so this functionality should be added to the
     * intention menu or {@code false} for all other types of caret positions
     */
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        // Quick sanity check
        return null != element && element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod;
    }

    /**
     * pop up the testMe menu list
     * @param project a reference to the Project object being edited.
     * @param editor  a reference to the object editing the project source
     * @param element a reference to the PSI element currently under the caret
     * @throws IncorrectOperationException Thrown by underlying (PSI model) write action context
     *                                     when manipulation of the PSI tree fails.
     */
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {
        PsiMethod method = (PsiMethod)element.getParent();
        final PsiClass srcClass = CreateTestMeAction.getContainingClass(element);
        // if is not testable show a error dialog
        if (!MethodFactory.isTestable(method, srcClass)) {
            String message = "Method \"" + method.getName() + "\" is not testable!";
            String title = "TestMe: Method Not Testable";
            Messages.showErrorDialog(message, title);
        } else {
            new TestMeActionHandler(true).invoke(project, editor, element.getContainingFile());
        }
    }

    @Override
    @NotNull
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    /**
     * If this action is applicable, returns the text to be shown in the list of intention actions available.
     */
    @NotNull public String getText() {
        return getFamilyName();
    }

    /**
     * Returns text for name of this family of intentions.
     * It is used to externalize "auto-show" state of intentions.
     * It is also the directory name for the descriptions.
     *
     * @return the intention family name.
     */
    @NotNull public String getFamilyName() {
        return "TestMe: Generate test for method";
    }

}
