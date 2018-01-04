package org.keeber.desktop;

import java.awt.GridBagLayout;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Example1 {

  public static class Model extends MVC.Model {
    private String name;
    private Address address = new Address();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      propertyChanged("name", this.name, this.name = name);
    }

    public Address getAddress() {
      return address;
    }

    public static class Address extends MVC.Model {
      private String street, town, region, postcode;

      public String getStreet() {
        return street;
      }

      public void setStreet(String street) {
        propertyChanged("street", this.street, this.street = street);
      }

      public String getTown() {
        return town;
      }

      public void setTown(String town) {
        propertyChanged("town", this.town, this.town = town);
      }

      public String getRegion() {
        return region;
      }

      public void setRegion(String region) {
        propertyChanged("region", this.region, this.region = region);
      }

      public String getPostcode() {
        return postcode;
      }

      public void setPostcode(String postcode) {
        propertyChanged("postcode", this.postcode, this.postcode = postcode);
      }



    }
  }

  public static class View extends JFrame {

    public View(Controller ctrl) {
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setIconImage(null);

      add(new JPanel(new GridBagLayout()) {
        {
          setBorder(MVC.View.borders.spacer());
          /*
           * Here we bind a single model property to a text field
           */
          add(new JLabel("Name:"), MVC.View.layout.GBC.create(1).insets(3));
          add(ctrl.binder().bindModelProperty("name", new JTextField(30), "text"), MVC.View.layout.GBC.create(2).insets(3));
          /*
           * Here we bind some dynamic properties to the map. We can technically do this with any
           * properties but the map can be observed as a whole (ie: when any of the properties
           * change).
           */
          for (String propName : new String[] {"Street", "Town", "Region", "Postcode"}) {
            add(new JLabel(propName + ":"), MVC.View.layout.GBC.create(1).insets(3));
            add(ctrl.binder().bindModelProperty("address." + propName.toLowerCase(), new JTextField(30), "text"), MVC.View.layout.GBC.create(2).insets(3));
          }
        }
      });
      pack();
      ctrl.start(true);
    }

  }

  public static class Controller extends MVC.Controller<Example1.Model> {

    public Controller(Model model) {
      super(model);
    }

    /*
     * This watches the name property of the model.
     */
    @WatchListener(properties = {"name"})
    public void onNameChanged(PropertyWatchEvent<String> pwe) {
      getLogger().log(Level.INFO, "[Change] [old:{0}][new:{1}]", new Object[] {pwe.getOldValue().orElse(""), pwe.getNewValue().orElse("")});
    }

    /*
     * This watches all the properties in the address object.
     */
    @WatchListener(properties = {"address.street", "address.town", "address.region", "address.postcode"})
    public void onAddressChanged(PropertyWatchEvent<String> pwe) {
      getLogger().log(Level.INFO, "[Change] [new:\n{0}\n]", new Object[] {getGson().toJson(m.address)});
    }

  }

  public static void main(String[] args) {
    MVC.View.setSystemLookAndFeel();
    new View(new Controller(new Model())).setVisible(true);
  }

}
