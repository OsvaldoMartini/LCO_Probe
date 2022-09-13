package com.lcoprobe.acbs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Osvaldo Martini
 */
// tag::code[]
@Controller
public class HomeController {

	/**
	 * Retrieve the main and page route
	 *
	 */
	@GetMapping(path = "/")
	public String index() {
		return "index";
	}

}
// end::code[]