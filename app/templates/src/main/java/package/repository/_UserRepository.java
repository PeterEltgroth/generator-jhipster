package <%=packageName%>.repository;

import <%=packageName%>.domain.User;<% if (socialAuth == 'yes') { %>
import <%=packageName%>.domain.ExternalAccountProvider;<% } %>
import org.joda.time.DateTime;<% if (databaseType == 'sql') { %>
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;<% } %><% if (databaseType == 'nosql') { %>
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;<% } %>

import java.util.List;

<% if (databaseType == 'sql') { %>/**
 * Spring Data JPA repository for the User entity.
 */<% } %><% if (databaseType == 'nosql') { %>/**
 * Spring Data MongoDB repository for the User entity.
 */<% } %>
public interface UserRepository extends <% if (databaseType == 'sql') { %>JpaRepository<% } %><% if (databaseType == 'nosql') { %>MongoRepository<% } %><User, String> {
    <% if (databaseType == 'sql') { %>
    @Query("select u from User u where u.activationKey = ?1")<% } %><% if (databaseType == 'nosql') { %>
    @Query("{activationKey: ?0}")<% } %>
    User getUserByActivationKey(String activationKey);
    <% if (databaseType == 'sql') { %>
    @Query("select u from User u where u.activated = false and u.createdDate > ?1")<% } %><% if (databaseType == 'nosql') { %>
    @Query("{activation_key: 'false', createdDate: {$gt: ?0}}")<% } %>
    List<User> findNotActivatedUsersByCreationDateBefore(DateTime dateTime);
    <% if (socialAuth == 'yes') { if (databaseType == 'sql') { %>
    @Query("select u from User u inner join u.externalAccounts ea where ea.externalProvider = ?1 and ea.externalId = ?2")<% } else if (databaseType == 'nosql') { %>
    @Query("{externalAccounts: { $in: [ {externalProvider: ?0, externalId: ?1} ]}}")<% } %>
    User getUserByExternalAccount(ExternalAccountProvider provider, String externalAccountId);<% } %>
}
