package org.keeber.desktop;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;
import org.jdesktop.observablecollections.ObservableListListener;
import org.jdesktop.observablecollections.ObservableMap;
import org.jdesktop.observablecollections.ObservableMapListener;
import org.jdesktop.swingbinding.SwingBindings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MVC {

  /**
   * This class is not extended like the Model and Controller, it is simply used to hold useful
   * classes in building a UI.
   * 
   * @author Jason Keeber <jason@keeber.org>
   *
   */
  public static class View {

    /**
     * Sets the look and feel without the huge catch block - because you probably know if it's going
     * to work or not.
     */
    public static void setSystemLookAndFeel() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {

      }
    }

    public static class borders {

      public static Border spacer() {
        return BorderFactory.createEmptyBorder(13, 13, 13, 13);
      }

    }

    public static class layout {

      /**
       * Handy builder pattern for GridBagConstraints.
       * 
       * @author Jason Keeber <jason@keeber.org>
       *
       */
      public static class GBC extends GridBagConstraints {

        public static GBC create() {
          return new GBC().setX(GBC.RELATIVE).setY(GBC.RELATIVE).setAnchor(GBC.WEST);
        }

        public static GBC create(int x) {
          return create().setX(x);
        }

        public static GBC create(int x, int y) {
          return create().setX(x).setY(y);
        }

        public GBC setAnchor(int anchor) {
          this.anchor = anchor;
          return this;
        }

        public GBC setX(int x) {
          this.gridx = x;
          return this;
        }

        public GBC setY(int y) {
          this.gridy = y;
          return this;
        }

        public GBC setGridWidth(int width) {
          this.gridwidth = width;
          return this;
        }

        public GBC setGridHeight(int height) {
          this.gridheight = height;
          return this;
        }

        public GBC insets(int top, int left, int bottom, int right) {
          this.insets = new Insets(top, left, bottom, right);
          return this;
        }

        public GBC insets(int horiz, int vert) {
          this.insets = new Insets(vert, horiz, vert, horiz);
          return this;
        }

        public GBC insets(int all) {
          this.insets = new Insets(all, all, all, all);
          return this;
        }

      }

    }

  }

  public static class Model {
    protected transient PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * Called when a property changes. Generally used in a setter like this:
     * 
     * <pre>
     * {@code
     *   public void setName(String name){
     *       propertyChanged("name",this.name,this.name = name);
     *   }
     * }
     * </pre>
     * 
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    protected void propertyChanged(String propertyName, Object oldValue, Object newValue) {
      support.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Not generally called directly but needed by the binding.
     * 
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
      support.addPropertyChangeListener(listener);
    }

    /**
     * Not generally called directly but needed by the binding.
     * 
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
      support.removePropertyChangeListener(listener);
    }

    /*
     * Maps and Lists
     */
    /**
     * A generic Map that tracks changes - it can be used to bind values: ${mapname.property}. Be
     * careful to have default values in the list if you depend on them (otherwise it will fail).
     * 
     * @author Jason Keeber <jason@keeber.org>
     *
     * @param <K>
     * @param <V>
     */
    public static final class ObservableMap<K, V> extends HashMap<K, V> implements org.jdesktop.observablecollections.ObservableMap<K, V> {
      private transient List<ObservableMapListener> listeners;
      private V defValue;

      public ObservableMap() {
        super();
      }

      public ObservableMap(V defValue) {
        super();
        this.defValue = defValue;
      }

      private void listeners(Consumer<? super ObservableMapListener> action) {
        listeners().forEach(action);
      }

      private List<ObservableMapListener> listeners() {
        return (listeners == null ? listeners = new CopyOnWriteArrayList<ObservableMapListener>() : listeners);
      }

      @Override
      public V get(Object key) {
        return defValue == null ? super.get(key) : super.getOrDefault(key, defValue);
      }

      @Override
      public V put(K key, V value) {
        V ovalue = super.put(key, value);
        listeners(l -> {
          if (ovalue == null) {
            l.mapKeyAdded(ObservableMap.this, key);
          } else {
            l.mapKeyValueChanged(ObservableMap.this, key, value);
          }
        });
        return ovalue;
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
        m.entrySet().forEach(e -> {
          put(e.getKey(), e.getValue());
        });
      }

      @Override
      public V remove(Object key) {
        V ovalue = super.remove(key);
        listeners(l -> {
          l.mapKeyRemoved(ObservableMap.this, key, ovalue);
        });
        return ovalue;
      }

      @Override
      public void addObservableMapListener(ObservableMapListener listener) {
        listeners().add(listener);
      }

      @Override
      public void removeObservableMapListener(ObservableMapListener listener) {
        listeners().remove(listener);
      }

    }

    /**
     * A generic trackable list.
     * 
     * @author Jason Keeber <jason@keeber.org>
     *
     * @param <E>
     */
    public static final class ObservableList<E> extends ArrayList<E> implements org.jdesktop.observablecollections.ObservableList<E> {
      private transient List<ObservableListListener> listeners;

      public ObservableList() {
        super();
      }

      public ObservableList(List<E> list) {
        super(list);
      }

      private List<ObservableListListener> listeners() {
        return (listeners == null ? listeners = new CopyOnWriteArrayList<ObservableListListener>() : listeners);
      }

      private void listeners(Consumer<? super ObservableListListener> action) {
        listeners().forEach(action);
      }

      public E set(int index, E element) {
        E oldValue = set(index, element);
        listeners(l -> {
          l.listElementReplaced(ObservableList.this, index, oldValue);
        });
        return oldValue;
      }

      public void add(int index, E element) {
        super.add(index, element);
        modCount++;
        listeners(l -> {
          l.listElementsAdded(ObservableList.this, index, 1);
        });
      }

      public boolean add(E element) {
        boolean result = super.add(element);
        modCount++;
        listeners(l -> {
          l.listElementsAdded(ObservableList.this, ObservableList.super.indexOf(element), 1);
        });
        return result;
      }

      public E remove(int index) {
        E oldValue = remove(index);
        modCount++;
        listeners(l -> {
          l.listElementsRemoved(ObservableList.this, index, java.util.Collections.singletonList(oldValue));
        });
        return oldValue;
      }

      public boolean addAll(int index, Collection<? extends E> c) {
        if (super.addAll(index, c)) {
          modCount++;
          listeners(l -> {
            l.listElementsAdded(ObservableList.this, index, c.size());
          });
        }
        return true;
      }

      public void clear() {
        List<E> dup = new ArrayList<E>(this);
        super.clear();
        modCount++;
        listeners(l -> {
          l.listElementsRemoved(ObservableList.this, 0, dup);
        });
      }

      public void fileElementChanged(E element) {
        listeners(l -> {
          l.listElementPropertyChanged(ObservableList.this, indexOf(element));
        });
      }

      public void addObservableListListener(ObservableListListener listener) {
        listeners().add(listener);
      }

      public void removeObservableListListener(ObservableListListener listener) {
        listeners().remove(listener);
      }

      public boolean supportsElementPropertyChanged() {
        return true;
      }

    }

  }

  public abstract static class Controller<M> {
    /**
     * This is the model, it is protected so it can be referred to directly which reduces the amount
     * of code.
     */
    protected M m;

    public Controller(M model) {
      this.m = model;
    }

    private transient final List<Binder> binders = Collections.synchronizedList(new ArrayList<>());

    public class Binder {
      private transient BindingGroup binding = new BindingGroup();
      private String name;

      public Binder(String name) {
        this.name = name;
      }

      /**
       * Two way binding between the source property (of the model) and the target property of the
       * target object (usually a UI control).
       * 
       * <p>
       * Source and target properties can be provided as the property name "property" or an
       * expression "${property > 0}"
       * 
       * @param srcProperty source property name or expression eg: "${prop}"
       * @param trg target Object (usually a UI control)
       * @param trgProperty target property name or expression eg: "${prop}"
       * @return target Object
       */
      public <T> T bindModelProperty(String srcProperty, T trg, String trgProperty) {
        binding.addBinding(Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, m, createProperty(srcProperty), trg, createProperty(trgProperty)));
        return trg;
      }

      /**
       * One way binding between the source property (of the model) and the target property of the
       * target object (usually a UI control).
       * 
       * <p>
       * Typically used to write a model property (changed by the controller) to the UI.
       * 
       * <p>
       * Source and target properties can be provided as the property name "property" or an
       * expression "${property > 0}"
       * 
       * @param srcProperty source property name or expression eg: "${prop}"
       * @param trg target Object (usually a UI control)
       * @param trgProperty target property name or expression eg: "${prop}"
       * @return target Object
       */
      public <T> T readModelProperty(String srcProperty, T trg, String trgProperty) {
        binding.addBinding(Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ, m, createProperty(srcProperty), trg, createProperty(trgProperty)));
        return trg;
      }

      /**
       * One way binding between the source property of the source object (usually a UI control) and
       * the target property of the model.
       * 
       * <p>
       * Typically used to write a model property from some event in the UI.
       * 
       * <p>
       * Source and target properties can be provided as the property name "property" or an
       * expression "${property > 0}"
       * 
       * @param srcProperty source property name or expression eg: "${prop}"
       * @param src source Object (usually a UI control)
       * @param trgProperty target property name or expression eg: "${prop}"
       * @return source Object
       */
      public <T> T writeModelProperty(String srcProperty, T src, String trgProperty) {
        binding.addBinding(Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ, src, createProperty(trgProperty), m, createProperty(srcProperty)));
        return src;
      }

      /*
       * SWING BINDINGS
       */

      /**
       * Binds the list property of the model to the target JList.
       * 
       * @param listName list source property name or expression eg: "${mylists.listone}"
       * @param target the JList
       * @return the JList (allows for chaining)
       */
      public <T> JList<T> bindModelList(String listName, JList<T> target) {
        try {
          @SuppressWarnings("unchecked")
          List<T> list = (List<T>) createProperty(listName).getValue(m);
          binding.addBinding(SwingBindings.createJListBinding(AutoBinding.UpdateStrategy.READ_WRITE, list, target));
        } catch (IllegalArgumentException | SecurityException e) {
          getLogger().log(Level.SEVERE, null, e);
        }
        return target;
      }

      /**
       * Binds the list property of the model to the target JComboBox.
       * 
       * @param listProperty list source property name or expression eg: "${mylists.listone}"
       * @param target JCombobox
       * @return the JCombobox (allows for chaining)
       */
      public <T> JComboBox<T> bindModelList(String listProperty, JComboBox<T> target) {
        try {
          @SuppressWarnings("unchecked")
          List<T> list = (List<T>) createProperty(listProperty).getValue(m);
          binding.addBinding(SwingBindings.createJComboBoxBinding(AutoBinding.UpdateStrategy.READ_WRITE, list, target));
        } catch (IllegalArgumentException | SecurityException e) {
          getLogger().log(Level.SEVERE, null, e);
        }
        return target;
      }

      /**
       * Binds the list property of the model to the target JComboBox. Sets the property defined by
       * selectedProperty to the selected value of the ComboBox.
       * 
       * @param listProperty list property name or expression eg: "${customers}"
       * @param selectedProperty selected property name or expression eg:"${selectedCustomer}"
       * @param target JComboBox to bind
       * @return the JCombobox (allows for chaining)
       */
      public <T> JComboBox<T> bindModelList(String listProperty, String selectedProperty, JComboBox<T> target) {
        JComboBox<T> box = bindModelList(listProperty, target);
        bindModelProperty(selectedProperty, target, "${selectedItem}");
        return box;
      }

      public void bind() {
        binding.bind();
      }

      /**
       * Unbind this binder
       */
      public void unbind() {
        binding.unbind();
      }

      /**
       * Unbind this and remove it from the binders
       */
      public void dispose() {
        unbind();
        binders.remove(this);
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null) {
          return false;
        }
        if (getClass() != obj.getClass()) {
          return false;
        }
        @SuppressWarnings("unchecked")
        Binder other = (Binder) obj;
        if (name == null) {
          if (other.name != null) {
            return false;
          }
        } else if (!name.equals(other.name)) {
          return false;
        }
        return true;
      }

    }

    public Binder binder() {
      return binder("main");
    }

    /**
     * Provides a Binder object for binding UI components to the model properties fro
     * 
     * @param name
     * @return
     */
    public Binder binder(String name) {
      return binders.stream().filter(b -> b.name.equals(name)).findFirst().orElseGet(() -> {
        Binder b = new Binder(name);
        binders.add(b);
        return b;
      });
    }

    protected static <S, V> Property<S, V> createProperty(String property) {
      return property.contains("${") ? ELProperty.create(property) : BeanProperty.create(property);
    }

    /**
     * Binds each of the binders - typically called from the start method.
     * 
     */
    public void update() {
      this.binders.forEach((b) -> {
        b.bind();
      });
    }

    /**
     * Start this controller (ie: have it call it's own abstract on start method) while optionally
     * performing the binding on all of the configured bindings.
     * 
     * @param bind
     */
    public void start(boolean bind) {
      for (Method method : this.getClass().getDeclaredMethods()) {

        WatchListener listener = method.getAnnotation(WatchListener.class);
        if (listener != null) {
          for (String property : listener.properties()) {
            watchModelProperty(property, method);
          }
        }
      }
      SwingUtilities.invokeLater(() -> {
        if (bind) {
          update();
        }
        onStart();
      });
    }

    public void onStart() {};

    /*
     * LOGGER
     */

    private transient Logger logger;

    public Logger getLogger() {
      return logger == null ? logger = Logger.getLogger(this.getClass().getSimpleName() + "-" + System.currentTimeMillis()) : logger;
    }

    /*
     * GSON TOOLS
     */

    private transient Gson gson;

    /**
     * A basic pretty serializer for Json Objects.
     * 
     * @return
     */
    public Gson getGson() {
      return gson == null ? gson = new GsonBuilder().setPrettyPrinting().create() : gson;
    }

    /**
     * Dump and object as Json to the logger - handy for debugging.
     * 
     * @param o
     */
    public void logObject(Object o) {
      getLogger().info(getGson().toJson(o));
    }

    /*
     * WATCH LISTENERS
     */
    /**
     * Defines a method as watching for changes of the specified properties. The method is called
     * with a single PropertyWatchEvent<T> with the expected property type.
     * 
     * @author Jason Keeber <jason@keeber.org>
     *
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface WatchListener {
      String[] properties();
    }

    /**
     * Internal method that processes the property watch annotations.
     * 
     * @param srcProperty
     * @param method
     */
    protected void watchModelProperty(String srcProperty, Method method) {
      Property<Object, Object> property = createProperty(srcProperty);
      if (property.getValue(m) instanceof Model.ObservableMap<?, ?>) {
        ((Model.ObservableMap<?, ?>) property.getValue(m)).addObservableMapListener(new ObservableMapListener() {

          @Override
          public void mapKeyAdded(@SuppressWarnings("rawtypes") ObservableMap arg0, Object arg1) {
            try {
              method.invoke(Controller.this, new PropertyWatchEvent<>(arg0, arg0, null));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              getLogger().log(Level.SEVERE, "Error invoking method", e);
            }
          }

          @Override
          public void mapKeyRemoved(@SuppressWarnings("rawtypes") ObservableMap arg0, Object arg1, Object arg2) {
            try {
              method.invoke(Controller.this, new PropertyWatchEvent<>(arg0, arg0, null));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              getLogger().log(Level.SEVERE, "Error invoking method", e);
            }
          }

          @Override
          public void mapKeyValueChanged(@SuppressWarnings("rawtypes") ObservableMap arg0, Object arg1, Object arg2) {
            try {
              method.invoke(Controller.this, new PropertyWatchEvent<>(arg0, arg0, null));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              getLogger().log(Level.SEVERE, "Error invoking method", e);
            }
          }
        });
      } else {
        property.addPropertyStateListener(m, new PropertyStateListener() {

          @Override
          public void propertyStateChanged(PropertyStateEvent pse) {
            try {
              method.invoke(Controller.this, new PropertyWatchEvent<>(pse.getOldValue(), pse.getNewValue(), pse));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              getLogger().log(Level.SEVERE, "Error invoking method", e);
            }
          }
        });
      }
    }

    /**
     * Event passed from a Property Watch Event containing the old and new values.
     * 
     * @author Jason Keeber <jason@keeber.org>
     *
     * @param <T>
     */
    public static final class PropertyWatchEvent<T> {
      private T oldValue;
      private T newValue;
      private PropertyStateEvent event;

      PropertyWatchEvent(T oldValue, T newValue, PropertyStateEvent event) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.event = event;
      }

      public Optional<T> getOldValue() {
        return Optional.ofNullable(oldValue);
      }

      public Optional<T> getNewValue() {
        return Optional.ofNullable(newValue);
      }

      public PropertyStateEvent getEvent() {
        return event;
      }

    }

    /*
     * ACTIONS
     */
    /**
     * Returns an action (named <b>name</b>) that will call the controller method <b>methodname</b>
     * with an ActionEvent.
     * 
     * @param name of the action
     * @param methodname to call on invoke
     * @return
     */
    public Action addAction(String name, String methodname) {
      return addAction(name, null, methodname);
    }

    /**
     * Returns an action (named <b>name</b>, with icon <b>icon</b>) that will call the controller
     * method <b>methodname</b> with an ActionEvent.
     * 
     * @param name of the action
     * @param icon of the action
     * @param methodname to call on invoke
     * @return
     */
    public Action addAction(String name, Icon icon, String methodname) {
      try {
        return new AbstractAction(name, icon) {

          @Override
          public void actionPerformed(ActionEvent ae) {
            try {
              Method method = Controller.this.getClass().getDeclaredMethod(methodname, new Class[] {ActionEvent.class});
              method.invoke(Controller.this, ae);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              getLogger().log(Level.SEVERE, "Error getting method [" + methodname + "]", e);
            }
          }
        };
      } catch (SecurityException e) {
        getLogger().log(Level.SEVERE, "Error getting method", e);
      }
      return null;
    }
    /*
     * MOUSE EVENTS
     */
    /*
     * MOUSE EVENTS
     */

    public <T extends JComponent> T addMouseListener(T comp, String methodname) {
      comp.addMouseListener(addMouseListener(methodname));
      return comp;
    }

    public MouseListener addMouseListener(String methodname) {
      return new MouseAdapter() {
        private Method method;

        private void callMethod(java.awt.event.MouseEvent event, MouseEvent.Type type) {
          try {
            method = method == null ? method = Controller.this.getClass().getDeclaredMethod(methodname, new Class[] {MouseEvent.class}) : method;
            method.invoke(Controller.this, new MouseEvent<Object>(event.getSource(), event, type));
          } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            getLogger().log(Level.SEVERE, "Error getting mouse listener method [" + methodname + "]", e);
          }
        }

        @Override
        public void mouseClicked(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.CLICKED);
        }

        @Override
        public void mouseDragged(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.DRAGGED);
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.ENTERED);
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.EXITED);
        }

        @Override
        public void mouseMoved(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.MOVED);
        }

        @Override
        public void mousePressed(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.PRESSED);
        }

        @Override
        public void mouseReleased(java.awt.event.MouseEvent event) {
          callMethod(event, MouseEvent.Type.RELEASED);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
          callMethod(event, MouseEvent.Type.WHEELMOVED);
        }

      };
    }

    public static final class MouseEvent<T> {
      private java.awt.event.MouseEvent nativeEvent;
      private Type type;
      private T source;

      protected MouseEvent(T src, java.awt.event.MouseEvent mouseEvent, Type type) {
        this.source = src;
        this.nativeEvent = mouseEvent;
        this.type = type;
      }

      public enum Type {
        CLICKED, DRAGGED, ENTERED, EXITED, MOVED, PRESSED, RELEASED, WHEELMOVED;
      }

      public java.awt.event.MouseEvent getNativeEvent() {
        return nativeEvent;
      }

      public Point getLocationOnScreen() {
        return nativeEvent.getLocationOnScreen();
      }

      public Point getPoint() {
        return nativeEvent.getPoint();
      }

      public Type getType() {
        return type;
      }

      public T getSource() {
        return source;
      }

    }

    public <T extends JComponent> T addDroptargetListener(T comp, String methodname) {
      comp.setDropTarget(new DropTarget(comp, DnDConstants.ACTION_COPY, addDroptargetListener(methodname)));
      return comp;
    }

    public DropTargetListener addDroptargetListener(String methodname) {
      return new DropTargetListener() {
        private Method method;

        private void callMethod(Object src, DropEvent.Type type, Transferable t) {
          try {
            method = method == null ? method = Controller.this.getClass().getDeclaredMethod(methodname, new Class[] {DropEvent.class}) : method;
            method.invoke(Controller.this, new DropEvent<Object>(src, type, t));
          } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            getLogger().log(Level.SEVERE, "Error getting drop listener method [" + methodname + "]", e);
          }
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
          dtde.acceptDrag(dtde.getDropAction());
          callMethod(dtde.getSource(), DropEvent.Type.DRAGENTER, null);
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
          dtde.acceptDrag(dtde.getDropAction());
          callMethod(dtde.getSource(), DropEvent.Type.DRAGOVER, dtde.getTransferable());

        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
          dtde.acceptDrag(dtde.getDropAction());
          callMethod(dtde.getSource(), DropEvent.Type.DROPCHANGED, dtde.getTransferable());
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
          callMethod(dte.getSource(), DropEvent.Type.DRAGEXIT, null);
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
          dtde.acceptDrop(dtde.getDropAction());
          callMethod(dtde.getSource(), DropEvent.Type.DROP, dtde.getTransferable());
        }
      };
    }

    public static final class DropEvent<T> {
      private T src;
      private Type type;
      private Transferable transferable;

      public DropEvent(T src, Type type, Transferable transferable) {
        this.src = src;
        this.type = type;
        this.transferable = transferable;
      }

      public T getSrc() {
        return src;
      }

      public Type getType() {
        return type;
      }

      public void ifTransferable(Consumer<Transferable> tr) {
        if (transferable != null) {
          tr.accept(transferable);
        }
      }

      public Transferable getTransferable() {
        return transferable;
      }

      public enum Type {
        DROP, DRAGEXIT, DROPCHANGED, DRAGOVER, DRAGENTER;
      }

    }

  }



}
