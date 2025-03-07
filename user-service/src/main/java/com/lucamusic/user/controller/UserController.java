package com.lucamusic.user.controller;

import com.lucamusic.user.utils.JwtUtil;
import javax.validation.Valid;

import com.lucamusic.user.controller.error.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lucamusic.user.entity.User;
import com.lucamusic.user.model.AuthenticationRequest;
import com.lucamusic.user.model.AuthenticationResponse;
import com.lucamusic.user.service.CustomUserDetailsService;
import com.lucamusic.user.utils.Utils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

/**
 * Nombre de la clase: UserControl
 * Esta clase se encarga de poner en uso los eventos de UserRepository
 * @author Emanuel
 * @version 14/09/2021/v1
 */

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
        
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtTokenUtil;
        
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> createAuthenticationToken(@Valid @RequestBody AuthenticationRequest authenticationRequest, BindingResult result) throws Exception {
        if(result.hasErrors()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Utils.formatBindingResult(result));
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword()));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getEmail());
        String token = jwtTokenUtil.generateToken(userDetails);
        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    /**
     * Como usuario sin registrar puedo darme de alta en el sistema
     * @param user Información del usuario
     * @param result
     * @return Usuario + status 200, 404 si el formato es inválido
     */

    @PostMapping("/register")
    public ResponseEntity<User> saveUser(@Valid @RequestBody User user, BindingResult result) {
        log.info("Creating User: {}", user);
        if(result.hasErrors()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Utils.formatBindingResult(result));
        }
        User userDB = userDetailsService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDB);
    }

	/**
	 * Como admin método para obtener la información de un usuario
	 * @param id Id del usuario
	 * @return Usuario + status 200, 404 si no existe
	 */

	@Secured({"ROLE_ADMIN"})
	@GetMapping("/{id}")
	public ResponseEntity<User> getUserById(@PathVariable("id") Long id){
		log.info("Fetching User with id {}", id);
		User user = userDetailsService.findByID(id);
		if(user == null){
			log.error("User with id {} not found", id);
			throw new UserNotFoundException();
		}
		return ResponseEntity.ok(user);
	}
}