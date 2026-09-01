package com.lensmatch.mobile.data;

import java.io.Serializable;

public class FrameModel implements Serializable {
    private String id;
    private String name;
    private String shape;
    private String material;
    private String price;

    public FrameModel(String id, String name, String shape, String material, String price) {
        this.id = id;
        this.name = name;
        this.shape = shape;
        this.material = material;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getShape() { return shape; }
    public String getMaterial() { return material; }
    public String getPrice() { return price; }
}
