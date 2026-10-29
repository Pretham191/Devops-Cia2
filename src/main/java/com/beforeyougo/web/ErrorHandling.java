package com.beforeyougo.web;

import com.beforeyougo.service.GuideNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ErrorHandling {

    @ExceptionHandler(GuideNotFoundException.class)
    public ModelAndView guideNotFound(GuideNotFoundException e) {
        ModelAndView mav = new ModelAndView("error/404", HttpStatus.NOT_FOUND);
        mav.addObject("detail", "We couldn't find that guide. It may have been removed, or the link is wrong.");
        return mav;
    }
}
