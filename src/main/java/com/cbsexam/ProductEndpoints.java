package com.cbsexam;

import cache.ProductCache;
import com.google.gson.Gson;
import controllers.ProductController;
import model.Product;
import utils.Encryption;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("product")
public class ProductEndpoints {

    //Jeg opretter her et objekt af klassen ProductCache, så klassen kan kaldes. Så getProducts nu bliver brugt.
    static ProductCache productCache = new ProductCache();

    /**
     * @param idProduct
     * @return Responses
     */
    @GET
    @Path("/{idProduct}")
    public Response getProduct(@PathParam("idProduct") int idProduct) {

        // Call our controller-layer in order to get the order from the DB
        Product product = ProductController.getProduct(idProduct);

        // TODO: Add Encryption to JSON - FIXED
        // We convert the java object to json with GSON library imported in Maven
        String json = new Gson().toJson(product);
        json = Encryption.encryptDecryptXOR(json);

        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.TEXT_PLAIN_TYPE).entity(json).build();
    }

    /**
     * @return Responses
     */
    @GET
    @Path("/")
    public Response getProducts() {

        // Bruger Chaching metoden fra ProductCache klassen og sætter den til false så den kun cacher når det er nødvendigt
        ArrayList<Product> products = productCache.getProducts(false);

        // TODO: Add Encryption to JSON - FIXED
        // We convert the java object to json with GSON library imported in Maven
        String json = new Gson().toJson(products);
        json = Encryption.encryptDecryptXOR(json);

        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.TEXT_PLAIN_TYPE).entity(json).build();
    }

    @POST
    @Path("/createProduct")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProduct(String body) {

        // Read the json from body and transfer it to a product class
        Product newProduct = new Gson().fromJson(body, Product.class);

        // Use the controller to add the user
        Product createdProduct = ProductController.createProduct(newProduct);

        //added Encryption
        // Get the user back with the added ID and return it to the user
        String json = new Gson().toJson(createdProduct);
        json = Encryption.encryptDecryptXOR(json);

        // Return the data to the user
        if (createdProduct != null) {

            //Opdatere Cache
            productCache.getProducts(true);

            // Return a response with status 200 and JSON as type
            return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
        } else {
            return Response.status(400).entity("Could not create product").build();
        }
    }

}
