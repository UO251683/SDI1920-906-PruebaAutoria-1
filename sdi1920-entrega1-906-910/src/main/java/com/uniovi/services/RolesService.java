package com.uniovi.services;

import org.springframework.stereotype.Service;

//Prácticamente es un enum.
//Se usa en el controlador (UserController), concretamente en los add y en el signup
@Service
public class RolesService {
	String[] roles = {"ROLE_USER","ROLE_ADMIN"};
	
	public String[] getRoles() {
		return roles;
	}
}
