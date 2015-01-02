package <%=packageName%>.security;

import <%=packageName%>.domain.Authority;
import <%=packageName%>.domain.User;
import <%=packageName%>.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;<% if (socialAuth == 'yes') { %>
import org.springframework.dao.DataAccessException;<% } %>
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;<% if (socialAuth == 'yes') { %>
import org.springframework.social.security.SocialUser;
import org.springframework.social.security.SocialUserDetails;
import org.springframework.social.security.SocialUserDetailsService;<% } %>
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;<%if (javaVersion == '8') {%>
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.List;<%}%>

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService <% if (socialAuth == 'yes') { %>, SocialUserDetailsService<% } %>{

    private final Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    @Inject
    private UserRepository userRepository;

    private User getUser(final String login) {
        String lowercaseLogin = login.toLowerCase();<%if (javaVersion == '8') {%>
        User userFromDatabase =  userRepository.findOneByLogin(lowercaseLogin)
            .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));
        if (!userFromDatabase.getActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        return userFromDatabase;
        <%} else {%>
        User userFromDatabase = userRepository.findOneByLogin(lowercaseLogin);
        if (userFromDatabase == null) {
            throw new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database");
        } else if (!userFromDatabase.getActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (Authority authority : userFromDatabase.getAuthorities()) {
            GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority.getName());
            grantedAuthorities.add(grantedAuthority);
        }
        return new org.springframework.security.core.userdetails.User(lowercaseLogin,
            userFromDatabase.getPassword(), grantedAuthorities);<% } %>
    }

    private Collection<GrantedAuthority> getGrantedAuthorities(User user) {
        <% if (javaVersion == '8') { %>
            return user.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                .collect(Collectors.toList());
        <% } else { %>
            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            for (Authority authority : user.getAuthorities()) {
                GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority.getName());
                grantedAuthorities.add(grantedAuthority);
            }
            return grantedAuthorities;
        <% } %>
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login){
        log.debug("Authenticating {}", login);
        User user = getUser(login);
        Collection<GrantedAuthority> grantedAuthorities = getGrantedAuthorities(user);
        log.debug("Login successful");
        return new org.springframework.security.core.userdetails.User(user.getLogin(), user.getPassword(), grantedAuthorities);
    }<% if (socialAuth == 'yes') { %>

    @Override
    @Transactional(readOnly = true)
    public SocialUserDetails loadUserByUserId(final String userId) throws UsernameNotFoundException, DataAccessException{
        log.debug("Authenticating {} from social login", userId);
        User user = getUser(userId);
        Collection<GrantedAuthority> grantedAuthorities = getGrantedAuthorities(user);
        log.debug("Login successful");
        return new SocialUser(user.getLogin(), "n/a", grantedAuthorities);
    }<% } %>
}
