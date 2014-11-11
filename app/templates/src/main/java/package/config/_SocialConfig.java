package <%=packageName%>.config;

import <%=packageName%>.security.social.SecurityUtilsUserIdSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.mem.InMemoryUsersConnectionRepository;

<% _.map(socialAuthProviders, function(provider) { %>
import org.springframework.social.<%=provider%>.connect.<%=_.titleize(provider)%>ConnectionFactory;<% }) %>

import javax.inject.Inject;

/**
 * Basic Spring Social configuration.  Creates the beans necessary to manage Connections to social services and
 * link accounts from those services to internal Users.
 */
@Configuration
@EnableSocial
public class SocialConfig implements SocialConfigurer {
    private final Logger log = LoggerFactory.getLogger(SocialConfig.class);

    @Inject
    private ConnectionSignUp signup;

    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {
        <% _.map(socialAuthProviders, function(provider) { %>
        // <%=provider%> configuration
        String <%=provider%>ClientId = environment.getProperty("spring.social.<%=provider%>.clientId");
        String <%=provider%>ClientSecret = environment.getProperty("spring.social.<%=provider%>.clientSecret");
        if (<%=provider%>ClientId != null && <%=provider%>ClientSecret != null) {
            log.debug("Configuring <%=_.titleize(provider)%>ConnectionFactory");
            connectionFactoryConfigurer.addConnectionFactory(
                new <%=_.titleize(provider)%>ConnectionFactory(
                    <%=provider%>ClientId,
                    <%=provider%>ClientSecret
                )
            );
        }
        <% }) %>
    }

    @Override
    public UserIdSource getUserIdSource() {
        return new SecurityUtilsUserIdSource();
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        // TODO: should this be converted to JdbcUsersConnectionRepository?  Doing so would allow a user to return to
        // any node in a cluster after the redirect back from an external OAuth2 authentication.  However, it would break support
        // for users that opt for a NoSQL store rather than traditional RDBMS.
        InMemoryUsersConnectionRepository repo = new InMemoryUsersConnectionRepository(connectionFactoryLocator);

        // register our ConnectionSignUp so that UsersConnectionRepository can resolve external account ids
        // to internal Users
        repo.setConnectionSignUp(signup);
        return repo;
    }
}
