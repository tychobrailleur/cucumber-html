package com.weblogism.cucumberjvm.report;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Sébastien Le Callonnec
 */
public class Node {
    private Object value;
    private String status;
    private List<Node> children = new ArrayList<Node>();
    
    public Node(Object value) {
        this.value = value;
    }
    
    public Object getValue() {
        return this.value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addChild(Node aChild) {
        this.children.add(aChild);
    }
    
    public List<Node> getChildren() {
        return this.children;
    }
}
