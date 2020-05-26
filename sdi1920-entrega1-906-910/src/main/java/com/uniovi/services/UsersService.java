package com.uniovi.services;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.uniovi.entities.User;
import com.uniovi.repositories.UsersRepository;

//Saca los datos del userRepository en este caso y genera métodos sencillos.
//Se usa en el controlador (UserController)
@Service
public class UsersService {

	@Autowired
	private UsersRepository usersRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	//Lista todos los usuarios de la aplicación
	public Page<User> getUsers(Pageable pageable) {
		Page<User> users = usersRepository.findAll(pageable);
			return users;
	}
	
	//Lista todos los usuarios de la aplicación salvo el propio usuario actual
	public Page<User> getUsersLessUser(Pageable pageable,User user) {
		Page<User> users = usersRepository.findAllLessUser(pageable, user);
			return users;
	}
	
	//Devuelve un usuario por el id
	public User getUser(Long id) {
		return usersRepository.findById(id).get();
	}
	
	//Añade un usuario al sistema
	public void addUser(User user) {
		user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
		usersRepository.save(user);
	}
	
	//Devuelve un usuario por el email (o username, en este caso son iguales)
	public User getUserByEmail(String email) {
		return usersRepository.findByEmail(email);
		}
	
	//Borra un usuario del sistema.
	public void deleteUser(Long id) {
		usersRepository. deleteById(id);
	}
	
	//Busca por el email, nombre y/o apellido un usuario/s que coincida.
	public Page<User> searchByEmailNameAndLastname (Pageable pageable,String searchText){
		Page<User> users = new PageImpl<User>(new LinkedList<User>());	
		searchText = "%"+searchText+"%";
		users = usersRepository.searchByEmailNameAndLastname(pageable,searchText);
		return users;
		}
	
	//Añade una nueva petición de amistad entre dos usuarios.
	public void addNewPetition(Long id, User user) {
		//Buscamos el usuario al que le queremos mandar la petición, indicado por el id que recibimos
		User toSend=usersRepository.findById(id).get();
		//Por cada usuario con el id que corresponde.
		user.getUsers().forEach(u -> {
			if(u.getId()==id)
				//Ahora tiene una petición.
				u.setResend(true);
		});
		//Añadimos en el recibidor la referencia de la petición con respecto al usuario que la mandó.
		toSend.getPetitions().add(user);
		//Guarda las entidades usuario que han intervenido en el proceso.
		usersRepository.save(toSend);
		usersRepository.save(user);

	}
	
	//Elimina una petición de amistad entre dos usuarios, tras haber sido esta aceptada.
	//Es decir, dentro un usuario concreto, elimina una petición después de que este la haya aceptado.
	public void deletePetition(Long id, User user) {
		//Buscamos el usuario al que le queremos eliminar la petición, indicado por el id que recibimos.
		User toSend=usersRepository.findById(id).get();
		//Por cada usuario con el id que corresponde.
		user.getUsers().forEach(u -> {
			if(u.getId()==id)
				//Ahora no tiene una petición.
				u.setResend(false);				
		});
		//Aceptamos la petición.
		toSend.setAceptado(true);
		//Añadimos en el recibidor la referencia de la petición con respecto al usuario que la mandó.
		toSend.getPetitions().add(user);
		//Guarda las entidades usuario que han intervenido en el proceso.
		usersRepository.save(toSend);
		usersRepository.save(user);

	}
	
	//Añade una amistad entre dos usuarios concretos.
	public void addNewFriend(Long id, User user) {
		//Buscamos el usuario objetivo, indicado por el id que recibimos.
		User toSend=usersRepository.findById(id).get();	
		//Añadimos en el usuario objetivo al User como nuevo amigo.
		toSend.getFriends().add(user);
		//Añadimos en el usuario User al usuario objetivo como nuevo amigo.
		user.getFriends().add(toSend);
		//Guarda las entidades usuario que han intervenido en el proceso.
		usersRepository.save(toSend);
		usersRepository.save(user);

	}
	
	//Añade un favorito entre los amigos.
		public void addNewFav(Long id, User user) {
			//Buscamos el usuario objetivo, indicado por el id que recibimos.
			User toSend=usersRepository.findById(id).get();	
			//Añadimos en el usuario objetivo al User como nuevo fav.
			toSend.getFavs().add(user);
			//Añadimos en el usuario User al usuario objetivo como nuevo fav.
			user.getFavs().add(toSend);
			//Guarda las entidades usuario que han intervenido en el proceso.
			usersRepository.save(toSend);
			usersRepository.save(user);

		}
		
		//Añade un favorito entre los amigos.
		public void deleteFav(Long id, User user) {
			//Buscamos el usuario objetivo, indicado por el id que recibimos.
			User toSend=usersRepository.findById(id).get();	
			//Añadimos en el usuario objetivo al User como nuevo fav.
			toSend.getFavs().remove(user);
			//Añadimos en el usuario User al usuario objetivo como nuevo fav.
			user.getFavs().remove(toSend);
			//Guarda las entidades usuario que han intervenido en el proceso.
			usersRepository.save(toSend);
			usersRepository.save(user);
		}
	
}
