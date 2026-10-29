package com.beforeyougo.web.form;

import com.beforeyougo.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InstitutionForm {

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Choose a category")
    private Category category;

    @Size(max = 100)
    private String city;

    @Size(max = 500)
    private String address;

    @Size(max = 300)
    private String contact;

    @Size(max = 2000)
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
