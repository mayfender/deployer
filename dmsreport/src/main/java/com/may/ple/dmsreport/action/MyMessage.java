package com.may.ple.dmsreport.action;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.may.ple.dmsreport.entity.Customer;

@Path("msg")
public class MyMessage {

	@GET
	@Path("/test1")
    @Produces(MediaType.APPLICATION_JSON)
    public Customer getMessage() {
		Customer customer = new Customer();
		customer.setName("mayfender");
		customer.setAge(10);
        return customer;
    }

	@GET
	@Path("/test2")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMessage2() {
		return "testing";
	}

}
