package controllers;

import model.Address;
import model.LineItem;
import model.Order;
import model.User;
import utils.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class OrderController {

    private static DatabaseController dbCon;

    public OrderController() {
        dbCon = new DatabaseController();
    }

    public static Order getOrder(int id) {

        // check for connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        // Build SQL string to query
        String sql = "select \n" +
                "\t*,\n" +
                "\ta.city as billing_city,\n" +
                "    a1.city as shipping_city\n" +
                "from orders o \n" +
                "JOIN address a \n" +
                "ON o.billing_address_id = a.id \n" +
                "JOIN user u\n" +
                "ON o.user_id = u.id\n" +
                "JOIN address a1\n" +
                "ON o.shipping_address_id = a1.id\n" +
                "WHERE o.id = " + id;

        // Do the query in the database and create an empty object for the results
        ResultSet rs = dbCon.query(sql);
        Order order = null;

        try {
            if (rs.next()) {


                ArrayList<LineItem> lineItems = LineItemController.getLineItemsForOrder(rs.getInt("id"));

                User user =
                        new User(
                                rs.getInt("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("password"),
                                rs.getString("email"));


                Address billingAddress =
                        new Address(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("street_address"),
                                rs.getString("city"),
                                rs.getString("zipcode")
                        );

                Address shippingAddress =
                        new Address(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("street_address"),
                                rs.getString("city"),
                                rs.getString("zipcode")
                        );


                // Create an object instance of order from the database dataa
                order =
                        new Order(
                                rs.getInt("id"),
                                user,
                                lineItems,
                                billingAddress,
                                shippingAddress,
                                rs.getFloat("order_total"),
                                rs.getLong("created_at"),
                                rs.getLong("updated_at"));

                // Returns the build order
                return order;
            } else {
                System.out.println("No order found");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        // Returns null
        return order;
    }

    /**
     * Get all orders in database
     *
     * @return
     */
    public static ArrayList<Order> getOrders() {

        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        String sql = "select \n" +
                "\t*,\n" +
                "\ta.city as billing_city,\n" +
                "    a1.city as shipping_city\n" +
                "from orders o \n" +
                "JOIN address a \n" +
                "ON o.billing_address_id = a.id \n" +
                "JOIN user u\n" +
                "ON o.user_id = u.id\n" +
                "JOIN address a1\n" +
                "ON o.shipping_address_id = a1.id";

        ResultSet rs = dbCon.query(sql);
        ArrayList<Order> orders = new ArrayList<Order>();

        try {
            while (rs.next()) {

                ArrayList<LineItem> lineItems = LineItemController.getLineItemsForOrder(rs.getInt("id"));

                User user =
                        new User(
                                rs.getInt("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("password"),
                                rs.getString("email"));


                Address billingAddress =
                        new Address(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("street_address"),
                                rs.getString("city"),
                                rs.getString("zipcode")
                        );

                Address shippingAddress =
                        new Address(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("street_address"),
                                rs.getString("city"),
                                rs.getString("zipcode")
                        );

                // Create an order from the database data
                Order order =
                        new Order(
                                rs.getInt("id"),
                                user,
                                lineItems,
                                billingAddress,
                                shippingAddress,
                                rs.getFloat("order_total"),
                                rs.getLong("created_at"),
                                rs.getLong("updated_at"));

                // Add order to our list
                orders.add(order);

            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        // return the orders
        return orders;
    }

    public static Order createOrder(Order order) {

        // Check for DB Connection
        if (dbCon == null) {
            dbCon = new DatabaseController();
        }

        // Write in log that we've reach this step
        Log.writeLog(OrderController.class.getName(), order, "Actually creating a order in DB", 0);

        // Set creation and updated time for order.
        order.setCreatedAt(System.currentTimeMillis() / 1000L);
        order.setUpdatedAt(System.currentTimeMillis() / 1000L);

        //Laver en forbindelse til databasen
        Connection comnection = dbCon.getConnection();

        try {

            //Sætter AutoCommit til false så vi selv kan bestemme hvornår vi commiter
            comnection.setAutoCommit(false);

            // Save addresses to database and save them back to initial order instance
            order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
            order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

            // Save the user to the database and save them back to initial order instance
            order.setCustomer(UserController.createUser(order.getCustomer()));

            // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts. - FIXED

            // Insert the product in the DB
            int orderID = dbCon.insert(
                    "INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, created_at, updated_at) VALUES("
                            + order.getCustomer().getId()
                            + ", "
                            + order.getBillingAddress().getId()
                            + ", "
                            + order.getShippingAddress().getId()
                            + ", "
                            + order.calculateOrderTotal()
                            + ", "
                            + order.getCreatedAt()
                            + ", "
                            + order.getUpdatedAt()
                            + ")");

            if (orderID != 0) {
                //Update the productid of the product before returning
                order.setId(orderID);
            }

            // Create an empty list in order to go trough items and then save them back with ID
            ArrayList<LineItem> items = new ArrayList<LineItem>();

            // Save line items to database
            for (LineItem item : order.getLineItems()) {
                item = LineItemController.createLineItem(item, order.getId());
                items.add(item);
            }
            order.setLineItems(items);


            comnection.commit();


        } catch (SQLException e) {

            //udskriver fejlen
            System.err.println(e.getMessage());
            if (comnection != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    comnection.rollback();
                } catch (SQLException excep) {
                    System.err.println(e.getMessage());
                }
            }
        } finally {
            try {
                comnection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Return order
        return order;
    }
}