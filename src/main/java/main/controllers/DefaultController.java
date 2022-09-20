package main.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Default controller for displaying the prepared html file.
 */
@Controller
public class DefaultController {

    @RequestMapping("/admin")
    public String index(Model model) {
        return "index";
    }
}
