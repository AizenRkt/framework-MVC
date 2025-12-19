package etu.sprint.framework.utils;

import java.util.HashMap;

public class ModelView {
    
    private String view;
    private HashMap<String, Object> data = new HashMap<>();
    private Boolean isRedirect = false;

    public ModelView() {}

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public void addItem(String key, Object value) {
        this.data.put(key, value);
    }

    public boolean isRedirect() { return isRedirect; }
    public void setRedirect(boolean redirect) { this.isRedirect = redirect; }
}
