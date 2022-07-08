package com.yugabyte.app.messenger.data.entity;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@IdClass(GeoId.class)
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_id_seq")
    @SequenceGenerator(name = "profile_id_seq", sequenceName = "profile_id_seq", allocationSize = 1)
    private Integer id;

    @Id
    @Column(name = "country_code")
    private String countryCode;

    @NotEmpty
    @Column(name = "full_name")
    private String fullName;

    @Email
    @NotEmpty
    private String email;

    @NotEmpty
    private String phone;

    @JsonIgnore
    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(name = "user_picture_url")
    private String userPictureUrl;

    @ManyToMany
    @JoinTable(name = "WorkspaceProfile", joinColumns = {
            @JoinColumn(name = "profile_id", referencedColumnName = "id"),
            @JoinColumn(name = "workspace_country", referencedColumnName = "country_code") })
    @Column(updatable = false, insertable = false)
    @LazyCollection(LazyCollectionOption.TRUE)
    private Set<Workspace> workspaces;

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

    @Override
    public String toString() {
        return "User [countryCode=" + countryCode + ", email=" + email +
                ", fullName=" + fullName + ", id=" + id + ", phone=" + phone + "]";
    }
}
