package com.yugabyte.app.messenger.data.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@IdClass(GeoId.class)
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_id_generator_pooled_lo")
    @GenericGenerator(name = "profile_id_generator_pooled_lo", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "profile_id_seq"),
            @Parameter(name = "initial_value", value = "1"),
            @Parameter(name = "increment_size", value = "5"),
            @Parameter(name = "optimizer", value = "pooled-lo") })
    private Integer id;

    @Id
    private String countryCode;

    @NotEmpty
    private String fullName;

    @Email
    @NotEmpty
    private String email;

    @NotEmpty
    private String phone;

    @JsonIgnore
    private String hashedPassword;

    private String userPictureUrl;

    @ManyToMany(cascade = {}, fetch = FetchType.EAGER)
    @JoinTable(name = "WorkspaceProfile", joinColumns = { @JoinColumn(name = "profile_id", referencedColumnName = "id"),
            @JoinColumn(name = "profile_country", referencedColumnName = "countryCode") }, inverseJoinColumns = {
                    @JoinColumn(name = "workspace_country", referencedColumnName = "countryCode"),
                    @JoinColumn(name = "workspace_id", referencedColumnName = "id") })
    private List<Workspace> workspaces = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getUserPictureUrl() {
        return userPictureUrl;
    }

    public void setUserPictureUrl(String userPictureUrl) {
        this.userPictureUrl = userPictureUrl;
    }

    public List<Workspace> getWorkspaces() {
        return workspaces;
    }

    @Override
    public String toString() {
        return "User [countryCode=" + countryCode + ", email=" + email +
                ", fullName=" + fullName + ", id=" + id + ", phone=" + phone + "]";
    }
}
