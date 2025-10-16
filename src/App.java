import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.time.LocalDate;
import java.util.*;

// Base class Prod
class Prod {
    String id;
    String name;
    double price;
    int qty;
    String type;

    public Prod(String id, String name, int qty, double price) {
        this.id = id;
        this.name = name;
        this.qty = qty;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }

    public synchronized void setQty(int qty) { this.qty = qty; }

    public String getType() {
        return "Unknown";
    }
}

// Derived class Good
class Good extends Prod {
    public Good(String id, String name, int qty, double price) {
        super(id, name, qty, price);
    }

    public String getType() { return "good"; }
}

// Derived class Cargo
class Cargo extends Prod {
    public Cargo(String id, String name, int qty, double price) {
        super(id, name, qty, price);
    }

    public String getType() { return "cargo"; }
}

// Inventory class
class Inv {
    Map<String, Prod> prods = new HashMap<>();
    List<String> lowStockNotifications = new ArrayList<>();
    int totalProducts = 0;

    void addProd(Prod p) {
        prods.put(p.getId(), p);
        totalProducts += p.getQty();
    }

    synchronized String procureProd(String id, int qty, String mode) {
        Prod p = prods.get(id);
        
        if (id == null || id.isEmpty()) {
            return "Product not found: ID is missing.";
        }

        if (p == null || (p.getName() == null || p.getName().isEmpty())) {
            return "Product not found: " + id;
        }
        
        if (p.getQty() >= qty) {
            p.setQty(p.getQty() - qty);
            totalProducts -= qty;
            checkLowStock(p);
            
            LocalDate deliveryDate = LocalDate.now().plusDays(7);
            return "Procured " + qty + " of " + p.getName() + "\nExpected Delivery Date: " + deliveryDate;
        } else {
            return p.getQty() == 0 
                ? "Product out of stock: " + p.getName() 
                : "Insufficient stock for " + p.getName() + ". Requested: " + qty + ", Available: " + p.getQty();
        }
    }

    void checkLowStock(Prod p) {
        if (p.getQty() < 10 && !lowStockNotifications.contains("Low stock notification for " + p.getName())) {
            lowStockNotifications.add("Low stock notification for " + p.getName());
        }
    }

    Prod[] getProds() {
        return prods.values().toArray(new Prod[0]);
    }

    List<String> getLowStockNotifications() {
        return lowStockNotifications;
    }

    int getTotalProducts() {
        return totalProducts;
    }

    int getProductsCountByType(String type) {
        return (int) prods.values().stream()
                .filter(p -> (p instanceof Good && "good".equals(type)) || (p instanceof Cargo && "cargo".equals(type)))
                .count();
    }

    String getStatistics() {
        int totalGoods = getProductsCountByType("good");
        int totalCargo = getProductsCountByType("cargo");
        return "Total Products: " + getTotalProducts() + 
               "\nGoods: " + totalGoods + 
               "\nCargo: " + totalCargo;
    }
}

// Manager class
class Mgr {
    Inv inv;
    int idCounter = 1;

    public Mgr(Inv inv) {
        this.inv = inv;
    }

    boolean addProd(String name, int qty, double price, String type, TextArea itemAddedNotif) {
        for (Prod prod : inv.prods.values()) {
            if (prod.getName().equalsIgnoreCase(name)) {
                itemAddedNotif.appendText("Item already exists: " + name + "\n");
                return false;
            }
        }
      
        String id = String.valueOf(idCounter++);
        Prod p = "good".equals(type) ? new Good(id, name, qty, price) : new Cargo(id, name, qty, price);
        inv.addProd(p);
        return true;
    }

    void addExistingProd(String id, int qty) {
        Prod existingProd = inv.prods.get(id);
        if (existingProd != null) {
            existingProd.setQty(existingProd.getQty() + qty);
        }
    }
}


public class App extends Application {
    Inv inv = new Inv();
    Mgr mgr = new Mgr(inv);

    public void start(Stage stage) {
        GridPane layout = new GridPane();
        layout.setAlignment(Pos.CENTER); 
        layout.setVgap(8);
        layout.setHgap(10);  
        layout.setPadding(new Insets(20));
        TextArea prodList = new TextArea();
        TextField nameInput = new TextField();
        TextField qtyInput = new TextField();
        TextField priceInput = new TextField();
        TextField modeInput = new TextField();
        TextField existingProdIdInput = new TextField();

        Button procureBtn = new Button("Procure Product");
        Button addProdBtn = new Button("Add New Product");
        Button addExistingProdBtn = new Button("Update Existing Product");
        Button viewStatsBtn = new Button("View Statistics");

        TextArea orderConf = new TextArea();
        TextArea lowStockNotif = new TextArea();
        TextArea itemAddedNotif = new TextArea();

        // prodList.setMaxWidth(500);     // Increase width for Product List
        // prodList.setPrefHeight(1000);   // Increase height slightly if needed

        // orderConf.setMaxWidth(500);    // Increase width for Order Confirmation
        // orderConf.setPrefHeight(1000);

        // Initial products
        mgr.addProd("Laptop", 20, 1500.00, "good", itemAddedNotif);
        mgr.addProd("Container", 5, 5000.00, "cargo" , itemAddedNotif);
        mgr.addProd("Tablet", 15, 300.00, "good", itemAddedNotif);
        mgr.addProd("Smartphone", 25, 800.00, "good", itemAddedNotif);

        prodList.setEditable(false);
        orderConf.setEditable(false);
        lowStockNotif.setEditable(false);
        itemAddedNotif.setEditable(false);
        
        updateProdList(prodList);

        TextArea statsDisplay = new TextArea();
        statsDisplay.setEditable(false); // Make it read-only


        viewStatsBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                statsDisplay.setText(inv.getStatistics());
            }
        });


        // Event handlers using anonymous inner classes
        procureBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleProcurement(existingProdIdInput, qtyInput, modeInput, orderConf, lowStockNotif, prodList);

                existingProdIdInput.clear();
                qtyInput.clear();
                modeInput.clear();
            }
        });

        addProdBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String name = nameInput.getText();
                int qty;
                double price;
                String mode = modeInput.getText().trim(); // Get the shipment mode
        
                try {
                    qty = Integer.parseInt(qtyInput.getText());
                    price = Double.parseDouble(priceInput.getText());
        
                    String type;
                    if (mode.equals("land")) {
                        type = "good";
                    } else if (mode.equals("sea")) {
                        type = "cargo";
                    } else {
                        itemAddedNotif.appendText("Enter a valid shipment mode (land/sea)\n");
                        return; 
                    }

                    boolean added = mgr.addProd(name, qty, price, type, itemAddedNotif); 
                    if(added)
                    {
                        itemAddedNotif.appendText("Added product " + name + ", Quantity: " + qty + ", Price: " + price + " to inventory\n");
                    }
                    updateProdList(prodList);
                } catch (NumberFormatException ex) {
                    itemAddedNotif.appendText("Invalid quantity or price.\n");
                }

                nameInput.clear();
                qtyInput.clear();
                priceInput.clear();
                modeInput.clear();
            }
            
        });

        addExistingProdBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String id = existingProdIdInput.getText().trim(); // Get the input ID
                String newName = nameInput.getText().trim(); // Get new name input
                String mode = modeInput.getText().trim(); // Get shipment mode input
                double newPrice = -1; // Initialize newPrice
                String newType = ""; // Initialize newType
                int qty = -1;
        
                try {
        
                    Prod existingProd = inv.prods.get(id); // Attempt to get the existing product by ID
        
                    // Check if the ID exists and is valid
                    if (existingProd == null) {
                        itemAddedNotif.appendText("Error: Product ID " + id + " not found. Please enter a valid ID.\n");
                    } else {
                        // Update quantity
                        if (!qtyInput.getText().trim().isEmpty()) {
                            try {
                                qty = Integer.parseInt(qtyInput.getText());
                                existingProd.setQty(qty); // Update the quantity
                            } catch (NumberFormatException e) {
                                itemAddedNotif.appendText("Error: Invalid quantity entered. Please enter a numeric value.\n");
                            }
                        }
                        // Update name if a new name is provided
                        if (!newName.isEmpty()) {
                            existingProd.name = newName; // Update the name
                        }
        
                        // Update price if a new price is provided
                        if (!priceInput.getText().trim().isEmpty()) {
                            try {
                                newPrice = Double.parseDouble(priceInput.getText());
                                existingProd.price = newPrice; // Update the price
                            } catch (NumberFormatException e) {
                                itemAddedNotif.appendText("Error: Invalid price entered. Please enter a numeric value.\n");
                            }
                        }
        
                        // Determine the new type based on the shipment mode
                        if (!mode.isEmpty()) {
                            if (mode.equalsIgnoreCase("sea")) {
                                newType = "cargo"; // Set type as "Cargo" for sea mode
                            } else if (mode.equalsIgnoreCase("land")) {
                                newType = "good"; // Set type as "Good" for land mode
                            } else {
                                itemAddedNotif.appendText("Warning: Unrecognized shipment mode. Type will not be set.\n");
                            }
                        }
        
                        // Update type if a new type is provided
                        if (!newType.isEmpty()) {
                            existingProd.type = newType; // Update the type
                        }
        
                        // Notify user about the updates
                        itemAddedNotif.appendText("Updated Product ID: " + id + ", Quantity: " + qty +
                                                   (newName.isEmpty() ? "" : ", New Name: " + newName) +
                                                   (newPrice != -1 ? ", New Price: " + newPrice : "") +
                                                   (newType.isEmpty() ? "" : ", New Type: " + newType) +
                                                   ".\n");
                        updateProdList(prodList);
                    }
                } catch (NumberFormatException ex) {
                    itemAddedNotif.appendText("Invalid quantity. Please enter a numeric value.\n");
                } catch (Exception e) {
                    itemAddedNotif.appendText("Error: Please enter a valid product ID (numeric value).\n");
                }

                existingProdIdInput.clear();
                nameInput.clear();
                qtyInput.clear();
                priceInput.clear();
                modeInput.clear();
            }
        });
        
        layout.add(new Label("Product Name:"), 0, 0);
        layout.add(nameInput, 1, 0);
        layout.add(new Label("Quantity:"), 0, 1);
        layout.add(qtyInput, 1, 1);
        layout.add(new Label("Price:"), 0, 2);
        layout.add(priceInput, 1, 2);
        
        layout.add(new Label("Existing Product ID:"), 0, 3);
        layout.add(existingProdIdInput, 1, 3);

        layout.add(new Label("Shipment Mode (land/sea):"), 0, 4);
        layout.add(modeInput, 1, 4);

        layout.add(addProdBtn, 1, 8);
        layout.add(addExistingProdBtn, 0, 8);
        
        layout.add(viewStatsBtn, 2, 18);
        layout.add(procureBtn, 2, 8);
        
        // Product List
        layout.add(new Label("Product List:"), 0, 6);
        layout.add(prodList, 0, 7, 2, 1);
        
        // Order Confirmation
        layout.add(new Label("Order Confirmation:"), 0, 10);
        layout.add(orderConf, 0, 11, 2, 1);
        
        // Low Stock Notifications
        layout.add(new Label("Low Stock Notifications:"), 0, 12);
        layout.add(lowStockNotif, 0, 13, 2, 1);
        
        // Item Added Notifications
        layout.add(new Label("Item Added Notifications:"), 0, 14);
        layout.add(itemAddedNotif, 0, 15, 2, 1);

        layout.add(new Label("Statistics:"), 0, 16); // Adjust the row number as needed
        layout.add(statsDisplay, 0, 17, 2, 1); 

        Scene scene = new Scene(layout, 700, 800);
        stage.setTitle("Inventory Management System");
        stage.setScene(scene);
        stage.show();
    }

    void handleProcurement(TextField existingProdIdInput, TextField qtyInput, TextField modeInput, TextArea orderConf, TextArea lowStockNotif, TextArea prodList) {
        String id = existingProdIdInput.getText();
        int qty;
        String mode = modeInput.getText().toLowerCase();

        try {
            qty = Integer.parseInt(qtyInput.getText());
            String result = inv.procureProd(id, qty, mode);

            orderConf.appendText(result + "\n");
            lowStockNotif.clear();
            lowStockNotif.setText(String.join("\n", inv.getLowStockNotifications()));
            updateProdList(prodList);
        } catch (NumberFormatException ex) {
            orderConf.appendText("Please enter a valid quantity.\n");
        }
    }


    void updateProdList(TextArea prodList) {
        StringBuilder sb = new StringBuilder();
        for (Prod p : inv.getProds()) {
            sb.append("ID: ").append(p.getId())
              .append(", Name: ").append(p.getName())
              .append(", Quantity: ").append(p.getQty())
              .append(", Price: ").append(p.getPrice())
              .append(", Type: ").append(p instanceof Good ? "Good" : (p instanceof Cargo ? "Cargo" : "Unknown")).append("\n");
        }
        prodList.setText(sb.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
} 



// TO Compile: javac --module-path lib --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics -d bin src/App.java
// TO Run: java --module-path "lib;bin" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics -cp bin App
