package net.eyde.personalblog.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.eyde.personalblog.service.PersonalBlogService;
import net.eyde.personalblog.service.ServiceException;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.checkerframework.checker.tainting.qual.Untainted;

/**
 * Description of the Class
 *
 * @author NEyde
 * @created September 17, 2002
 */
public final class ReadAction extends BlogGeneralAction {
    /**
     * Process the specified HTTP request, and create the corresponding HTTP response (or forward to
     * another web component that will create it). Return an ActionForward instance describing where
     * and how control should be forwarded, or null if the response has already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @param form Description of the Parameter
     * @return Description of the Return Value
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    @Override
    public ActionForward executeSub(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        ActionErrors errors = new ActionErrors();
        String forward = "readposts";

        // Get request parameters
        String reqCategory = cleanNull(request.getParameter("cat"));

        // Get instance of PersonalBlog Service
        PersonalBlogService pblog = PersonalBlogService.getInstance();

        // Set Request Parameters
        // Depending on the parameters, call the appropriate method
        try {
            if (!reqCategory.equals("")) {
                request.setAttribute("posts", pblog.getPostsByCategory(reqCategory));
            } else {
                request.setAttribute("posts", pblog.getPosts());
            }

        } catch (ServiceException e) {
            ActionMessages messages = new ActionMessages();
            ActionMessage message = new ActionMessage("exception.postdoesnotexist");
            messages.add(ActionMessages.GLOBAL_MESSAGE, message);

            errors.add(messages);
            e.printStackTrace();
        }

        if (!errors.isEmpty()) {
            saveErrors(request, errors);
        }

        return (mapping.findForward(forward));
    }

    /**
     * Validates userInput: verifies that it cannot be used for an attack.
     *
     * <p>A string is valid if it contains only letters, digits, and whitespace.
     *
     * @param userInput user input to be validated
     * @return the input if it is valid
     * @throws IllegalArgumentException if userInput is not valid
     */
    @Untainted String validate(String userInput) {
        for (int i = 0; i < userInput.length(); ++i) {
            char ch = userInput.charAt(i);
            if (!Character.isLetter(ch) && !Character.isDigit(ch) && !Character.isWhitespace(ch))
                throw new IllegalArgumentException("Illegal user input");
        }
        @SuppressWarnings("tainting")
        @Untainted String result = userInput;
        return result;
    }
}

/* To fix the bug, replace line 48 by:
        String reqCategory = validate(cleanNull(request.getParameter("cat")));
*/
