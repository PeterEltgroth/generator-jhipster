package <%=packageName%>.service;

import <%=packageName%>.domain.Authority;<% if (socialAuth == 'yes') { %>
import <%=packageName%>.domain..ExternalAccount;<% } %>
import <%=packageName%>.domain.PersistentToken;
import <%=packageName%>.domain.User;
import <%=packageName%>.repository.AuthorityRepository;
import <%=packageName%>.repository.PersistentTokenRepository;
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.security.SecurityUtils;
import <%=packageName%>.service.util.RandomUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;<% if (databaseType == 'sql') { %>
import org.springframework.transaction.annotation.Transactional;<% } %>

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;<% if (javaVersion == '8') { %>
import java.util.Optional;<% } %>
import java.util.Set;

/**
 * Service class for managing users.
 */
@Service<% if (databaseType == 'sql') { %>
@Transactional<% } %>
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private PersistentTokenRepository persistentTokenRepository;

    @Inject
    private AuthorityRepository authorityRepository;

    public User activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);<% if (javaVersion == '8') { %>
        return Optional.ofNullable(userRepository.getUserByActivationKey(key))
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);
                userRepository.save(user);
                log.debug("Activated user: {}", user);
                return user;
            })
            .orElse(null);<% } else { %>
        User user = userRepository.getUserByActivationKey(key);

        // activate given user for the registration key.
        if (user != null) {
            user.setActivated(true);
            user.setActivationKey(null);
            userRepository.save(user);
            log.debug("Activated user: {}", user);
        }
        return user;<% } %>
    }

    <% if (socialAuth != 'yes') { %>public <% } %>User createUserInformation(String login, String password, String firstName, String lastName,
                               String email, String langKey<% if (socialAuth == 'yes') { %>, ExternalAccount externalAccount<% } %>) {
        User newUser = new User();
        Authority authority = authorityRepository.findOne("ROLE_USER");
        Set<Authority> authorities = new HashSet<>();
        authorities.add(authority);
        newUser.setAuthorities(authorities);

        if (StringUtils.isNotBlank(password)) {
            String encryptedPassword = passwordEncoder.encode(password);
            newUser.setPassword(encryptedPassword);
        }<% if (socialAuth == 'yes') { %>
        else {
            newUser.getExternalAccounts().add(externalAccount);
            externalAccount.setUser(newUser);
        }<% } %>

        newUser.setLogin(login);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setLangKey(langKey);

        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());

        userRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }<% if (socialAuth == 'yes') { %>

    public User createUserInformation(String login, String password, String firstName, String lastName, String email,
                                      String langKey) {
        return createUserInformation(login, password, firstName, lastName, email, langKey, null);
    }

    public User createUserInformation(String login, String firstName, String lastName, String email,
                                      String langKey, ExternalAccount externalAccount) {
        return createUserInformation(login, null, firstName, lastName, email, langKey, externalAccount);
    }<% } %>

    public void updateUserInformation(String firstName, String lastName, String email) {
        User currentUser = userRepository.findOne(SecurityUtils.getCurrentLogin());
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email);
        userRepository.save(currentUser);
        log.debug("Changed Information for User: {}", currentUser);
    }

    public void changePassword(String password) {
        User currentUser = userRepository.findOne(SecurityUtils.getCurrentLogin());
        String encryptedPassword = passwordEncoder.encode(password);
        currentUser.setPassword(encryptedPassword);
        userRepository.save(currentUser);
        log.debug("Changed password for User: {}", currentUser);
    }
<% if (databaseType == 'sql') { %>
    @Transactional(readOnly = true)<% } %>
    public User getUserWithAuthorities() {
        User currentUser = userRepository.findOne(SecurityUtils.getCurrentLogin());
        currentUser.getAuthorities().size(); // eagerly load the association
        return currentUser;
    }

    /**
     * Persistent Token are used for providing automatic authentication, they should be automatically deleted after
     * 30 days.
     * <p/>
     * <p>
     * This is scheduled to get fired everyday, at midnight.
     * </p>
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        LocalDate now = new LocalDate();
        List<PersistentToken> tokens = persistentTokenRepository.findByTokenDateBefore(now.minusMonths(1));
        for (PersistentToken token : tokens) {
            log.debug("Deleting token {}", token.getSeries());<% if (databaseType == 'sql') { %>
            User user = token.getUser();
            user.getPersistentTokens().remove(token);<% } %>
            persistentTokenRepository.delete(token);
        }
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p/>
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     * </p>
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        DateTime now = new DateTime();
        List<User> users = userRepository.findNotActivatedUsersByCreationDateBefore(now.minusDays(3));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getLogin());
            userRepository.delete(user);
        }
    }
}
