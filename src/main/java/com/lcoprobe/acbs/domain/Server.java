package com.lcoprobe.acbs.domain;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Osvaldo Martini
 */
// tag::code[]
@Entity
public class Server {

    private @Id
    @GeneratedValue
    Long id;
    private String serverName;
    private String host;
    private String userName;
    private String password;
    private Long timeout;
    private String status;
    private String environment;
    private String description;

    private @Version
    @JsonIgnore
    Long version;

    private Server() {
    }

    public Server(String serverName, String host, String userName, String password, Long timeout, String environment, String description) {
        this.serverName = serverName;
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.timeout = timeout;
        this.description = description;
        this.environment = environment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(id, server.id) &&
                Objects.equals(serverName, server.serverName) &&
                Objects.equals(host, server.host) &&
                Objects.equals(userName, server.userName) &&
                Objects.equals(password, server.password) &&
                Objects.equals(status, server.status) &&
                Objects.equals(timeout, server.timeout) &&
                Objects.equals(environment, server.environment) &&
                Objects.equals(description, server.description) &&
                Objects.equals(version, server.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverName, host, userName, password, status, timeout, environment, description, version);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Server{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", timeout=" + timeout +
                ", status='" + status + '\'' +
                '}';
    }
}
// end::code[]