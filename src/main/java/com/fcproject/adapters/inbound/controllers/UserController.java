//package com.fcproject.adapters.inbound.controllers;
//
//import com.fcproject.adapters.inbound.mapper.ObjectMapper;
//import com.fcproject.adapters.outbound.entities.;

//import com.fcproject.application.services.UserServices;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/user")
//public class UserController {
//
//    @Autowired
//    private UserServices userServices;
//
//
//    // POST to create users
//    @PostMapping(
//        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
//        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
//        value = "/create"
//    )
//    public ResponseEntity<String> createUser(@RequestBody UserPostDTO user) {
//        userServices.createUser(ObjectMapper.parseObject(user, UserEntity.class));
//
//        return new ResponseEntity<>(HttpStatus.CREATED);
//    }
//
//    // GET to search users by email
//    @GetMapping(
//        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
//    )
//    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
//        return ResponseEntity.ok(userServices.getUserByEmail(email));
//    }
//
//   // GET to search users by id
//    @GetMapping(
//            value = "{user_id}/",
//            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
//    )
//    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer user_id) {
//
//        UserDTO user = userServices.getUserById(user_id);
//
//        if (user != null) {
//            return ResponseEntity.ok(user);
//        }
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//    }
//
//    //User delete
//    @DeleteMapping("{user_id}/")
//    public ResponseEntity<String> deleteUser(@PathVariable("user_id") Integer user_id) {
//        UserDTO user = userServices.getUserById(user_id);
//
//        if (user != null) {
//            userServices.deleteUser(user_id);
//
//            return ResponseEntity.noContent().build();
//        }
//
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//    }
//
//}
