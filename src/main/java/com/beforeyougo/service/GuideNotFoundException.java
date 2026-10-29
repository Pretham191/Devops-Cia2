package com.beforeyougo.service;

public class GuideNotFoundException extends RuntimeException {

    public GuideNotFoundException(Long id) {
        super("No guide with id " + id);
    }
}
