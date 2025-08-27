package com.jompastech.backend.security.service;

import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // search for user by email (username)
        User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Returns the User as UserDetails
        return new UserDetailsImpl(user);
    }
}