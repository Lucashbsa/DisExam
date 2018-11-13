package controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import model.User;
import utils.Hashing;
import utils.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class UserController {

    private static DatabaseController dbCon;

    public UserController() {
        dbCon = new DatabaseController();
    }

    public static User getUser(int id) {

        // Check for connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        // Build the query for DB
        String sql = "SELECT * FROM user where id=" + id;

        // Actually do the query
        ResultSet rs = dbCon.query(sql);
        User user = null;

        try {
            // Get first object, since we only have one
            if (rs.next()) {
                user =
                        new User(
                                rs.getInt("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("password"),
                                rs.getString("email"));

                // return the create object
                return user;
            } else {
                System.out.println("No user found");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        // Return null
        return user;
    }

    /**
     * Get all users in database
     *
     * @return
     */
    public static ArrayList<User> getUsers() {

        // Check for DB connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        // Build SQL
        String sql = "SELECT * FROM user";

        // Do the query and initialyze an empty list for use if we don't get results
        ResultSet rs = dbCon.query(sql);
        ArrayList<User> users = new ArrayList<User>();

        try {
            // Loop through DB Data
            while (rs.next()) {
                User user =
                        new User(
                                rs.getInt("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("password"),
                                rs.getString("email"));

                // Add element to list
                users.add(user);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        // Return the list of users
        return users;
    }

    public static User createUser(User user) {

        Hashing hashing = new Hashing(); //..

        // Write in log that we've reach this step
        Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);

        // Set creation time for user.
        user.setCreatedTime(System.currentTimeMillis() / 1000L);

        // Check for DB Connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        // Insert the user in the DB
        // TODO: Hash the user password before saving it. - FIXED
        int userID = dbCon.insert(
                "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES('"
                        + user.getFirstname()
                        + "', '"
                        + user.getLastname()
                        + "', '"
                        + hashing.hashWithSalt(user.getPassword()) //kalder hashWithSalt og hasher passworded med salt før den gemmer det
                        + "', '"
                        + user.getEmail()
                        + "', "
                        + user.getCreatedTime()
                        + ")");

        if (userID != 0) {
            //Update the userid of the user before returning
            user.setId(userID);
        } else {
            // Return null if user has not been inserted into database
            return null;
        }

        // Return user
        return user;
    }

    public static String loginUser(User user) {

        // Check for DB Connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        String sql = "SELECT * FROM user where email='" + user.getEmail() + "'AND password='" + user.getPassword() + "'";

        dbCon.loginUser(sql);

        // Actually do the query
        ResultSet resultSet = dbCon.query(sql);
        User userlogin;
        String token = null;

        try {
            // Get first object, since we only have one
            if (resultSet.next()) {
                userlogin =
                        new User(
                                resultSet.getInt("id"),
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                resultSet.getString("password"),
                                resultSet.getString("email"));

                if (userlogin != null) {
                    try {
                        Algorithm algorithm = Algorithm.HMAC256("secret");
                        token = JWT.create()
                                .withClaim("userid", userlogin.getId())
                                .withIssuer("auth0")
                                .sign(algorithm);
                    } catch (JWTCreationException exception) {
                        //Invalid Signing configuration / Couldn't convert Claims.
                        System.out.println(exception.getMessage());
                    } finally {
                        return token;
                    }
                }
            } else {
                System.out.println("No user found");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        // Return null
        return "";
    }

    public static boolean deleteUser(String token) {

        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        DecodedJWT jwt = null;

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build();  //reusable verifier indstance
            jwt = verifier.verify(token);
        } catch (JWTVerificationException exception) {
            //Invalid signature/claims
        }

        String sql = "DELETE FROM user WHERE id = " + jwt.getClaim("userid").asInt();

        return dbCon.deleteUser(sql);
    }

    public static boolean updateUser(User user, String token) {

        Hashing hashing = new Hashing();

        // Check for DB Connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        DecodedJWT jwt = null;
        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build(); //Reusable verifier instance
            jwt = verifier.verify(token);
        } catch (JWTVerificationException exception) {
            //Invalid signature/claims
        }

        String sql =
                "UPDATE user SET first_name = '" + user.getFirstname() + "', last_name ='" + user.getLastname() + "', password = '" + user.getPassword()
                        + "', email ='" + user.getEmail() + "' WHERE id = " + jwt.getClaim("userid").asInt();

        return dbCon.updateUser(sql);
    }
}
