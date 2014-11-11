package <%=packageName%>.security.social;

import <%=packageName%>.security.SecurityUtils;
import org.springframework.social.UserIdSource;


/**
 * A UserIdSource that delegates to {@link <%=packageName%>.security.SecurityUtils#getCurrentLogin()}.
 * UserIdSource is used by Spring Security Social to link the
 * {@link org.springframework.social.connect.Connection Connections} stored in the
 * {@link org.springframework.social.connect.UsersConnectionRepository UsersConnectionRepository} to the
 * {@link <%=packageName%>.domain.User#getLogin() internal logins}.
 */
public class SecurityUtilsUserIdSource implements UserIdSource {
    @Override
    public String getUserId() {
        return SecurityUtils.getCurrentLogin();
    }
}
