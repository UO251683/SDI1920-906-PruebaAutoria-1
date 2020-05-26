package com.uniovi.controllers;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.uniovi.entities.*;
import com.uniovi.services.RolesService;
import com.uniovi.services.SecurityService;
import com.uniovi.services.UsersService;
import com.uniovi.validators.SignUpFormValidator;

//Rutas de la app. Las declara y gestiona qué hará y mostrará en todos los casos.
@Controller
public class UserController {

	@Autowired
	private UsersService usersService;
	
	@Autowired
	private SecurityService securityService;
	
	@Autowired
	private SignUpFormValidator signUpFormValidator;
	
	@Autowired
	private RolesService rolesService;
	
	//Lista todos los usuarios de la aplicación.
	//Model = modelo de datos.
	@RequestMapping("/user/list")
	public String getListado(Model model, Pageable pageable, 
			@RequestParam(value="",required=false) String searchText)
	{
		//Inicializamos una lista paginada de usuarios.
		Page<User> users=new PageImpl<User>(new LinkedList<User>());
		
		//Cogemos la sesión actual.
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//Sacamos el email del usuarios de la sesión actual.
		 String email = auth.getName();
		 //Sacamos el usuario actual en base al email anterior.
		 User activeUser = usersService.getUserByEmail(email);
		 
		 //Si el campo de búsqueda tiene algo escrito.
		if(searchText!=null && !searchText.isEmpty()) 
			//Buscamos los usuarios en base a ese criterio.
			users=usersService.searchByEmailNameAndLastname(pageable,searchText);
		else //Si no tiene nada escrito ese campo.
			//Sacamos directamente todos los usuarios menos el usuario actual. 
			users=usersService.getUsersLessUser(pageable, activeUser);
		
		//Añadimos al atributo usersList el contenido de la lista anterior.
		model.addAttribute("usersList",users.getContent());
		//Añade al atributo page la lista de usuarios anterior, paginada.
		model.addAttribute("page", users);
		//Redirecciona a la lista de usuarios de la aplicación.
		return "user/list";
	}
	
	//Seleccionamos la lista de roles y la enviamos a la vista para permitirnos seleccionar un rol.
	//Model = modelo de datos.
	@RequestMapping(value="/user/add")
	public String getUser(Model model){
		model.addAttribute("rolesList", rolesService.getRoles());
		return "user/add";
	}
	
	//Añade un usuario al sistema.
	@RequestMapping(value="/user/add", method=RequestMethod.POST )
	public String setUser(@ModelAttribute User user){
		usersService.addUser(user);
		return "redirect:/user/list";
	}
	
	//Devuelve los detalles de un usuario concreto, por el id.
	//Model = modelo de datos.
	@RequestMapping("/user/details/{id}" )
	public String getDetail(Model model, @PathVariable Long id){
		model.addAttribute("user", usersService.getUser(id));
		return "user/details";
	}
	
	//Lleva a la vista de administrador.
	@RequestMapping("/user/adminView" )
	public String getAdminView(){
		return "user/adminView";
	}
	
	//Borra un usuario concreto, por el id.
	@RequestMapping("/user/delete/{id}" )
	public String delete(@PathVariable Long id){
		usersService.deleteUser(id);
		return "redirect:/user/list";
	}

	//Lleva al registro de la app.
	//Model = modelo de datos.
	@RequestMapping(value = "/signup", method = RequestMethod.GET)
	public String signup(Model model) {
		model.addAttribute("user", new User());
		return "signup";
	}
	
	//Valida el registro.
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	public String signup(@Validated User user, BindingResult result) {
		this.signUpFormValidator.validate(user, result);
		if(result.hasErrors())
			return "signup";
		
		user.setRole(rolesService.getRoles()[0]);
		usersService.addUser(user);
		securityService.autoLogin(user.getEmail(), user.getPasswordConfirm());
		return "redirect:home";
	}
	
	//Lleva a la vista home de la app.
	//Model = modelo de datos.
	@RequestMapping(value = { "/home" }, method = RequestMethod.GET)
	public String home(Model model) {
		//Coge la sesión actual.
		 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//Coge el email de la sesión actual. Es decir, del usuario identificado.
		 String email = auth.getName();
		//Busca y saca dicho usuario buscando por el email anteriormente sacado.
		 User activeUser = usersService.getUserByEmail(email);
		 //Añade al atributo userList el usuario activo.
		 model.addAttribute("userList", activeUser);
		 return "home";
	}
	
	//Valida el login de la app.
	//Model = modelo de datos.
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(Model model, String error, String logout) {
		if (error != null)
			model.addAttribute("error", "Your username and password is invalid.");
		if (logout != null)
			model.addAttribute("message", "You have been logged out successfully.");

		return "login";
	}

	//Actualiza la lista de usuarios de la app.
	//Model = modelo de datos.
	//Pageable = paginación recibida como parámetro.
	@RequestMapping("/user/list/update")
	public String updateList(Model model, Pageable pageable){
		//Inicializa una lista paginada de usuarios.
		Page<User> users=new PageImpl<User>(new LinkedList<User>());
		//Rellena la lista paginada con la paginación recibida como parámetro.
		users= usersService.getUsers(pageable);
		//Añade al atributo userList la lista de usuarios.
		model.addAttribute("userList",users.getContent());	
		//Redirecciona a la tabla de usuarios de la aplicación.
		return "user/list :: tableUsers";
	}
	
	//Pone a true una petición entre el usuario actual y un usuario objetivo (dado por el id).
	//Model = modelo de datos.
	//Principal = objeto que contiene información de la sesión actual.
	@RequestMapping(value="/user/{id}/resend", method=RequestMethod.GET)
	public String setResendTrue(Model model, @PathVariable Long id,Principal principal){
		//Coge el email del usuario actual, gracias al objeto principal.
		 String email = principal.getName();
		//Busca y saca dicho usuario buscando por el email anteriormente sacado.
		 User activeUser = usersService.getUserByEmail(email);
		 //Crea una nueva petición entre el usuario objetivo (dado por el id) y el usuario actual.
		usersService.addNewPetition(id, activeUser);
		
		//Redirecciona a la lista de usuarios.
		return "redirect:/user/list";
	}
	
	//Añade un amigo a un usuario concreto, por el id. Elimina en el proceso la petición de amistad, ya que la acepta.
	//Model = modelo de datos.
	@RequestMapping(value="/user/{id}/agregar", method=RequestMethod.GET)
	public String setAgregar(Model model, @PathVariable Long id){
		//Coge la sesión actual.
		 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//Coge el email de la sesión actual. Es decir, del usuario identificado.
		 String email = auth.getName();
		//Busca y saca dicho usuario buscando por el email anteriormente sacado.
		 User activeUser = usersService.getUserByEmail(email);	
		 //Borra la petición creada anteriormente entre el usuario objetivo y el usuario creador.
		usersService.deletePetition(id, activeUser);
		//Finalmente crea la amistad entre ambos usuarios.
		usersService.addNewFriend(id,activeUser);
		
		return "redirect:/user/petitions";
	}
	
	//Lista las peticiones de amistad de un usuario concreto.
	//Model = modelo de datos.
	@RequestMapping("/user/petitions")
	public String getPetitions(Model model,Pageable pageable)
	{	
		//Coge la sesión actual.
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//Coge el email de la sesión actual. Es decir, del usuario identificado.
		 String email = auth.getName();
		 //Busca y saca dicho usuario buscando por el email anteriormente sacado.
		 User activeUser = usersService.getUserByEmail(email);		
		 
		 //Crea una lista de usuarios en base a las peticiones actuales del mismo.
		List<User> lista = activeUser.getPetitions().stream().collect(Collectors.toList());
		
		//Crea una lista de paginación de usuarios en base a la lista de usuarios anterior.
			Page<User> users=new PageImpl<User>(lista);
			 
			//Añade al atributo petitionList la lista de usuarios anterior.
			model.addAttribute("petitionsList",users.getContent());
			//Añade al atributo page la lista de usuarios anterior, paginada.
			model.addAttribute("page", users);

			return "user/petitions";

	}
	
	//Lista los amigos de un usuario concreto.
	//Model = modelo de datos.
	@RequestMapping("/friend/list")
	public String getFriendsList(Model model)
	{	
		//Coge la sesión actual.
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//Coge el email de la sesión actual. Es decir, del usuario identificado.
		 String email = auth.getName();
		 //Busca y saca dicho usuario buscando por el email anteriormente sacado.
		 User activeUser = usersService.getUserByEmail(email);		
		 
		 //Crea una lista de usuarios con todos los amigos actuales del mismo.
		List<User> lista = activeUser.getFriends().stream().collect(Collectors.toList());
		
			//Crea una lista de paginación de usuarios en base a la lista de usuarios anterior.
			Page<User> users=new PageImpl<User>(lista);
			 
			//Añade al atributo friendList la lista de usuarios anterior.
			model.addAttribute("friendsList",users.getContent());
			//Añade al atributo page la lista de usuarios anterior, paginada.
			model.addAttribute("page", users);

			return "friend/list";

	}
	
	//Lista los favoritos de un usuario concreto.
		//Model = modelo de datos.
		@RequestMapping("/user/favorits")
		public String getFavList(Model model)
		{	
			//Coge la sesión actual.
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			//Coge el email de la sesión actual. Es decir, del usuario identificado.
			 String email = auth.getName();
			 //Busca y saca dicho usuario buscando por el email anteriormente sacado.
			 User activeUser = usersService.getUserByEmail(email);		
			 
			 //Crea una lista de usuarios con todos los amigos actuales del mismo.
			List<User> lista = activeUser.getFavs().stream().collect(Collectors.toList());
			
				//Crea una lista de paginación de usuarios en base a la lista de usuarios anterior.
				Page<User> users=new PageImpl<User>(lista);
				 
				//Añade al atributo favsList la lista de usuarios anterior.
				model.addAttribute("favsList",users.getContent());
				//Añade al atributo page la lista de usuarios anterior, paginada.
				model.addAttribute("page", users);

				return "user/favorits";

		}
		
		//Añade un amigo a la lista de favs
		//Model = modelo de datos.
		@RequestMapping(value="/user/{id}/agregarFavorito", method=RequestMethod.GET)
		public String setFavs(Model model, @PathVariable Long id){
			//Coge la sesión actual.
			 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			//Coge el email de la sesión actual. Es decir, del usuario identificado.
			 String email = auth.getName();
			//Busca y saca dicho usuario buscando por el email anteriormente sacado.
			 User activeUser = usersService.getUserByEmail(email);	
			 //Añade dicho usuario a la lista de favs del usuario activo.
			 usersService.addNewFav(id, activeUser);
			return "redirect:/user/favorits";
		}
		
		//Borrar un amigo a la lista de favs
		//Model = modelo de datos.
		@RequestMapping(value="/user/{id}/eliminarFavorito", method=RequestMethod.GET)
		public String setFavsFalse(Model model, @PathVariable Long id){
			//Coge la sesión actual.
			 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			//Coge el email de la sesión actual. Es decir, del usuario identificado.
			 String email = auth.getName();
			//Busca y saca dicho usuario buscando por el email anteriormente sacado.
			 User activeUser = usersService.getUserByEmail(email);	
			 //Añade dicho usuario a la lista de favs del usuario activo.
			 usersService.deleteFav(id, activeUser);
			return "redirect:/user/favorits";
		}
}
