package com.cbsexam;

import cache.OrderCache;
import com.google.gson.Gson;
import controllers.OrderController;
import model.Order;
import utils.Encryption;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("order")
public class OrderEndpoints {

    //Jeg opretter her et objekt af klassen OrderCache, så klassen kan kaldes. Så getUsers nu bliver brugt.
    static OrderCache orderCache = new OrderCache();

    /**
     * @param idOrder
     * @return Responses
     */
    @GET
    @Path("/{idOrder}")
    public Response getOrder(@PathParam("idOrder") int idOrder) {

        // Call our controller-layer in order to get the order from the DB
        Order order = OrderController.getOrder(idOrder);

        // TODO: Add Encryption to JSON - FIXED
        // We convert the java object to json with GSON library imported in Maven
        String json = new Gson().toJson(order);
        json = Encryption.encryptDecryptXOR(json);

        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(json).build();
    }

    /**
     * @return Responses
     */
    @GET
    @Path("/")
    public Response getOrders() {

        // Bruger Chaching metoden fra UserCache klassen og sætter den til false så den kun cacher når det er nødvendigt
        ArrayList<Order> orders = orderCache.getOrders(false);

        // TODO: Add Encryption to JSON - FIXED
        // We convert the java object to json with GSON library imported in Maven
        String json = new Gson().toJson(orders);
        json = Encryption.encryptDecryptXOR(json);

        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.TEXT_PLAIN_TYPE).entity(json).build();
    }

    @POST
    @Path("/createOrder")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrder(String body) {

        // Read the json from body and transfer it to a order class
        Order newOrder = new Gson().fromJson(body, Order.class);

        // Use the controller to add the user
        Order createdOrder = OrderController.createOrder(newOrder);

        // TODO: (Måske Encryption)
        // Get the user back with the added ID and return it to the user
        String json = new Gson().toJson(createdOrder);
        //json = Encryption.encryptDecryptXOR(json);

        // Return the data to the user
        if (createdOrder != null) {
            // Return a response with status 200 and JSON as type
            return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
        } else {

            // Return a response with status 400 and a message in text
            return Response.status(400).entity("Could not create user").build();
        }
    }
}